# event-framework-v2 使用文档

## 1. 文档目标

本文档描述当前 `event-framework-v2` 已经具备的能力、推荐接入方式、最小使用流程，以及当前版本的边界与限制。

这是一份会持续迭代的使用文档。
当前内容严格基于仓库中已经落地的代码能力整理，不包含尚未实现的扩展点承诺。

---

## 2. 当前版本定位

当前版本是一个**单模块领域事件框架骨架**，根包为：

`com.sulin.codepose.event.framework`

它已经具备以下基础能力：

- 定义统一的领域事件 API 契约
- 定义 handler、handler chain、event store、retry policy、payload serializer 等核心扩展点
- 支持按 handler 粒度构建 `HandlerExecutionRecord`
- 支持事务提交后发布 Spring 事件
- 支持基于本地事件记录的异步消费
- 支持普通 handler、延迟 handler、grouped handler 三种基础模型
- 支持 replay 扫描与 replay 协调器骨架
- 提供基于 MySQL + MyBatis-Plus 的默认 `EventStore`
- 提供测试专用的 `InMemoryEventStore`
- 提供 Spring 自动装配

当前版本**还不是最终可直接投产的完整框架**，更准确地说，它是一个已经可运行的第一版骨架。

---

## 3. 当前包结构

```text
src/main/java/com/sulin/codepose/event/framework/
  api/
    model/
    handler/
    chain/
    publish/
    store/
    policy/
    serialize/
  core/
    builder/
    chain/
    policy/
    registry/
    replay/
    scheduler/
    serialize/
    store/
  spring/
    config/
    listener/
    publish/
```

主要入口类：

- `api/model/DomainEvent.java`
- `api/handler/DomainEventHandler.java`
- `api/chain/EventHandlerChain.java`
- `api/store/EventStore.java`
- `core/builder/DefaultHandlerExecutionRecordBuilder.java`
- `core/chain/DefaultEventProcessor.java`
- `spring/publish/SpringTransactionAwareEventPublisher.java`
- `spring/config/DomainEventFrameworkAutoConfiguration.java`

---

## 4. 当前核心模型说明

### 4.1 `DomainEvent`

`DomainEvent` 是业务侧需要实现的事件入口模型。

当前接口定义要求提供以下基础元数据：

- `bizCode()`
- `bizId()`
- `eventType()`
- `eventKey()`
- `occurredAt()`
- `payloads()`

另外，当前接口还提供了一个默认方法：

- `records()`

它的作用是：

- 如果事件对象本身携带了执行记录，`DefaultEventProcessor` 可以直接用这些记录处理
- 如果事件对象没有携带记录，`DefaultEventProcessor` 会基于 `eventKey` 从 `EventStore` 中重新加载记录

这让“首次发布”和“持久化后异步消费”可以共用同一套处理器。

#### 4.1.1 `eventKey` 的价值

`eventKey` 不是业务对象 id，而是“一次事件实例”的唯一标识。

它的核心价值有四个：

- 把同一次事件拆分出来的多条 `HandlerExecutionRecord` 归到同一组
- 在异步消费阶段，支持处理器按 `eventKey` 从 `EventStore` 回查整组记录
- 在 replay 阶段，支持按 `eventKey` 对扫描结果分组后再重放
- 和 `handlerCode` 组合形成稳定唯一约束，避免同一事件的同一 handler 重复建记录

为什么不能只用 `bizId` 或 `bizId + eventType`：

- 同一个业务对象可能多次触发同一种事件
- `bizId` 标识的是业务实体，不是事件实例
- `eventKey` 标识的是“这一回发生的事件”，粒度更细

推荐约束：

- 同一次事件的所有 handler 记录共享同一个 `eventKey`
- 不同事件实例必须使用不同的 `eventKey`
- `eventKey` 应保持稳定且全局唯一

当前推荐格式可以是：

- `bizCode_bizId_eventType_uuid`
- 或业务侧已有的事件流水号/事件唯一号

### 4.2 `HandlerExecutionRecord`

`HandlerExecutionRecord` 表示一条 handler 级别的执行记录，是当前框架最核心的持久化单元。

当前已经包含的关键字段：

- `eventKey`
- `bizCode`
- `bizId`
- `eventType`
- `handlerCode`
- `parentHandlerCode`
- `payload`
- `payloadVersion`
- `status`
- `retryNum`
- `executeTime`
- `version`
- `createdAt`
- `updatedAt`

当前设计坚持“一条 handler 一条记录”，而不是“一条事件一条记录”。

### 4.3 `ExecutionStatus`

当前持久化状态包括：

- `PENDING`
- `PROCESSING`
- `FINISHED`
- `ABORT`
- `GROUP_MAIN_FINISHED`
- `GROUP_MAIN_FINISHED_SUB_ABORT`

### 4.4 `EventHandleResult`

运行态返回值与持久化状态分离。

当前支持：

- `FINISHED`
- `FAIL`
- `ABORT`
- `GROUP_MAIN_FINISHED`
- `GROUP_MAIN_FINISHED_SUB_ABORT`

状态转换由 `EventRecordStateMachine` 统一处理。

---

## 5. 当前扩展点说明

### 5.1 `DomainEventHandler<P>`

业务方实现 handler 的核心接口。

需要实现：

- `handlerCode()`：稳定 handler 身份
- `payloadClass()`：payload 类型
- `buildPayload(DomainEvent event)`：首次发布时从事件中提取当前 handler 关心的 payload
- `handle(...)`：真正的处理逻辑

约束：

- `handlerCode` 必须稳定，不能依赖类名重构
- handler 应当按 at-least-once 语义实现幂等

### 5.2 `DelayableEventHandler<P>`

如果一个 handler 需要延迟执行，可以实现这个接口。

扩展方法：

- `executeTime(DomainEvent event, P payload)`

当前框架会在构建 `HandlerExecutionRecord` 时把 `executeTime` 写入记录，执行器遇到未来时间的记录会跳过。

### 5.3 `GroupedEventHandler<P>`

如果一个 handler 是 grouped handler 主处理者，可以实现这个接口。

扩展方法：

- `subHandlers()`

当前 grouped 语义：

- 主 handler 先执行
- 如果主 handler 返回 `GROUP_MAIN_FINISHED`，再执行子 handler
- 如果子 handler 返回 `ABORT` 或 `GROUP_MAIN_FINISHED_SUB_ABORT`，父记录会被更新为 `GROUP_MAIN_FINISHED_SUB_ABORT`

### 5.4 `EventHandlerChain`

业务方需要按 `bizCode + eventType` 定义 handler 链。

当前接口要求：

- `bizCode()`
- `eventType()`
- `handlers()`

框架通过 `EventHandlerChainRegistry` 找到对应处理链。

### 5.5 `EventStore`

当前框架定义了统一的持久化接口：

- `append(...)`
- `compareAndSet(...)`
- `scanRetryable(...)`
- `loadByEventKey(...)`

当前默认正式实现是：

- `spring/store/mybatis/MybatisPlusEventStore`

它基于 MyBatis-Plus 对接 MySQL，并严格对齐当前框架的核心语义：

- `eventKey + handlerCode` 唯一约束
- 基于 `id + version + status` 的 CAS 更新
- `scanRetryable(...)` 的重试状态筛选
- `loadByEventKey(...)` 按 `id asc` 返回整组记录

当前测试夹具实现是：

- `src/test/java/com/sulin/codepose/event/framework/support/store/InMemoryEventStore.java`

它仅用于：

- 本地单测
- 最小链路演示
- 无数据库依赖的 Spring 集成测试

正式代码不再提供默认内存版 `EventStore`。
如果业务不是 MySQL + MyBatis-Plus 组合，应自行提供 `EventStore` 实现。

---

## 6. 当前默认实现说明

### 6.1 `DefaultHandlerExecutionRecordBuilder`

职责：

- 解析 `DomainEvent` 对应的 `EventHandlerChain`
- 对每个 handler 调用 `buildPayload(...)`
- 为每个命中的 handler 生成一条 `HandlerExecutionRecord`
- 如果是 grouped handler，会递归为子 handler 生成记录
- 如果是 delay handler，会写入 `executeTime`

### 6.2 `DefaultEventProcessor`

职责：

- 按 `bizCode + eventType` 查找处理链
- 按 `handlerCode` 找到对应记录
- 反序列化 payload
- 调用 handler
- 根据结果通过 `EventRecordStateMachine` 推进状态
- 通过 `EventStore.compareAndSet(...)` 更新记录

当前行为要点：

- 如果 `event.records()` 为空，会自动回退到 `EventStore.loadByEventKey(event.eventKey())`
- `PENDING` 状态会先 CAS 更新为 `PROCESSING`
- 执行成功后再更新为 `FINISHED` 或 grouped 中间态
- 执行失败会根据 `RetryPolicy` 回退到 `PENDING` 或 `ABORT`

### 6.3 `EventRecordStateMachine`

职责：

- 管理运行态结果到持久化状态的转换
- 统一处理失败重试、异常重试、grouped 中间态

### 6.4 `InMemoryEventHandlerChainRegistry`

职责：

- 按 `bizCode + eventType` 注册并查找 chain
- 启动时校验重复 key

### 6.5 `SpringTransactionAwareEventPublisher`

职责：

- `publishNow(...)`：直接发布 Spring 事件
- `publishAfterCommit(...)`：如果当前有事务，则在 `afterCommit` 发布；否则直接发布

### 6.6 `SpringDomainEventListener`

职责：

- 监听 Spring 发布出来的 `DomainEvent`
- 调用 `DefaultEventProcessor` 处理
- 捕获异常，避免异常回抛到业务线程

### 6.7 `DefaultReplayScanner` 与 `DefaultEventReplayCoordinator`

当前 replay 能力已经有骨架：

- `DefaultReplayScanner`：调用 `EventStore.scanRetryable(...)` 扫描记录
- `DefaultEventReplayCoordinator`：按 `eventKey` 聚合记录，再交给 `DefaultEventProcessor`

当前 replay 仍然是骨架能力，后续还会继续增强。

---

## 7. Spring 自动装配

当前自动装配类：

- `spring/config/DomainEventFrameworkAutoConfiguration.java`
- `spring/config/DomainEventMybatisPlusStoreAutoConfiguration.java`

`DomainEventFrameworkAutoConfiguration` 默认会注入：

- `DomainEventPublisher`
- `EventPayloadSerializer`
- `RetryPolicy`
- `EventRecordStateMachine`
- `EventHandlerChainRegistry`
- `DefaultHandlerExecutionRecordBuilder`

只有在上下文中存在 `EventStore` bean 时，才会继续注入：

- `DefaultReplayScanner`
- `DefaultEventProcessor`
- `DefaultEventReplayCoordinator`
- `SpringDomainEventListener`

`DomainEventMybatisPlusStoreAutoConfiguration` 在以下条件成立时自动注入默认 `EventStore`：

- classpath 中存在 MyBatis-Plus 基础设施
- Spring 上下文中存在 `DataSource`
- Spring 上下文中已经完成 `SqlSessionFactory` 装配
- 业务方没有自定义 `EventStore`

自动装配入口已经写入：

- `src/main/resources/META-INF/spring.factories`

这意味着：

- 如果业务方提供了 `DataSource` 且使用 MyBatis-Plus，框架会自动装配默认持久化能力
- 如果业务方显式提供自己的 `EventStore`，框架会优先使用业务方实现
- 如果业务方只提供 `DataSource`，但并不使用 MyBatis-Plus，则不会得到默认 store，需要自行提供 `EventStore`
- 如果没有任何 `EventStore`，框架只保留基础构件，不会启动处理链路

---

## 8. 5 分钟快速开始

如果你想先快速确认这套骨架能不能跑起来，建议先按下面这条最短路径接入。

当前推荐先直接走 `MySQL + MyBatis-Plus` 版本最小链路，目标不是一步把 replay 等高级能力接全，而是先把“业务事务内落库 + 事务提交后异步处理”跑通：

- 定义一个 `DomainEvent`
- 实现一个 `DomainEventHandler`
- 注册一个 `EventHandlerChain`
- 在事务内构建并保存 `HandlerExecutionRecord`
- 在事务提交后发布事件
- 让 Spring 监听器自动消费并更新执行状态

### 8.1 你最终会得到什么

跑通后，你应该可以看到下面这条链路成立：

```text
创建事件
  -> build records
  -> append 到 MybatisPlusEventStore
  -> publishAfterCommit
  -> SpringDomainEventListener 收到事件
  -> DefaultEventProcessor 执行 handler
  -> record 状态更新为 FINISHED
```

### 8.2 接入前准备数据库

正式接入前需要先准备三件事：

1. 应用上下文中提供可用的 `DataSource`
2. 应用侧使用 MyBatis-Plus 基础设施，保证 `SqlSessionFactory` 能正常装配
3. 提前执行框架附带的 MySQL 建表 SQL：
   - `docs/domain_event_record.mysql.sql`

当前框架只提供建表基线，不负责自动建表。
测试使用的 H2 schema 位于：

- `src/test/resources/sql/domain_event_record_h2.sql`

如果你的业务不是 `MySQL + MyBatis-Plus` 组合，请不要依赖默认 store，而是自行实现 `EventStore`。

### 8.3 第一步：准备一个最小事件

```java
public class DemoCreatedEvent implements DomainEvent {

    @Override
    public String bizCode() {
        return "demo";
    }

    @Override
    public Long bizId() {
        return 1L;
    }

    @Override
    public String eventType() {
        return "created";
    }

    @Override
    public String eventKey() {
        return "demo_1_created_1";
    }

    @Override
    public Instant occurredAt() {
        return Instant.now();
    }

    @Override
    public List<EventPayload> payloads() {
        return Collections.<EventPayload>singletonList(new DemoPayload("payload"));
    }
}
```

### 8.3 第二步：准备 payload

```java
public class DemoPayload implements EventPayload {

    private String value;

    public DemoPayload() {
    }

    public DemoPayload(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
```

### 8.4 第三步：实现一个最小 handler

```java
@Component
public class DemoHandler implements DomainEventHandler<DemoPayload> {

    @Override
    public String handlerCode() {
        return "demo-handler";
    }

    @Override
    public Class<DemoPayload> payloadClass() {
        return DemoPayload.class;
    }

    @Override
    public Optional<DemoPayload> buildPayload(DomainEvent event) {
        return Optional.of(new DemoPayload("payload"));
    }

    @Override
    public EventHandleResult handle(
            DomainEvent event,
            DemoPayload payload,
            HandlerExecutionRecord record,
            EventExecutionContext context
    ) {
        return EventHandleResult.FINISHED;
    }
}
```

### 8.5 第四步：注册 handler chain

```java
@Bean
public EventHandlerChain demoChain(DemoHandler handler) {
    return new EventHandlerChain() {
        @Override
        public String bizCode() {
            return "demo";
        }

        @Override
        public String eventType() {
            return "created";
        }

        @Override
        public List<DomainEventHandler<?>> handlers() {
            return Collections.<DomainEventHandler<?>>singletonList(handler);
        }
    };
}
```

### 8.6 第五步：在事务内构建并保存 records

当前版本还没有统一 facade，所以最小用法是手动串三步：

```java
@Transactional
public void createDemo() {
    DomainEvent event = new DemoCreatedEvent();
    List<HandlerExecutionRecord> records = recordBuilder.build(event);
    eventStore.append(event, records);
}
```

这一步建议和业务数据更新放在同一个本地事务里。

### 8.7 第六步：事务提交后发布事件

```java
@Transactional
public void publishDemoAfterCommit(DomainEvent event) {
    domainEventPublisher.publishAfterCommit(event);
}
```

当前默认的 `SpringTransactionAwareEventPublisher` 会：

- 有事务时，在 `afterCommit` 触发发布
- 没有事务时，直接发布

### 8.8 第七步：验证结果

如果接入正确，当前默认 listener 会自动处理这个事件。

你至少可以验证两件事：

1. handler 的 `handle(...)` 被调用了
2. 对应 record 的状态从 `PENDING / PROCESSING` 最终推进到 `FINISHED`

如果你想看现成的最小工作示例，直接参考：

- `src/test/java/com/sulin/codepose/event/framework/spring/MybatisPlusPipelineIntegrationTest.java`

如果你只是想看无数据库依赖的测试夹具版本，也可以参考：

- `src/test/java/com/sulin/codepose/event/framework/spring/InMemoryPipelineIntegrationTest.java`

### 8.9 快速开始阶段的建议

第一次接入时，建议只做这些：

- 先只实现一个普通 handler
- 先准备 `DataSource`、MyBatis-Plus 和建表 SQL
- 先不要碰 replay、delay、grouped handler
- 先把 `build -> append -> publishAfterCommit -> listener -> processor` 跑通

等这条最小链路稳定后，再继续加：

- retry / replay
- delay handler
- grouped handler

如果你只是在测试里验证最小链路，也可以参考测试专用的内存实现：

- `src/test/java/com/sulin/codepose/event/framework/support/store/InMemoryEventStore.java`
- `src/test/java/com/sulin/codepose/event/framework/spring/InMemoryPipelineIntegrationTest.java`

---

## 9. 当前推荐接入方式

当前建议业务方按下面方式接入。

### Step 1：定义自己的 `DomainEvent`

示例：

```java
public class OrderCreatedEvent implements DomainEvent {

    private final Long orderId;
    private final Instant occurredAt;

    public OrderCreatedEvent(Long orderId, Instant occurredAt) {
        this.orderId = orderId;
        this.occurredAt = occurredAt;
    }

    @Override
    public String bizCode() {
        return "order";
    }

    @Override
    public Long bizId() {
        return orderId;
    }

    @Override
    public String eventType() {
        return "created";
    }

    @Override
    public String eventKey() {
        return "order_" + orderId + "_created";
    }

    @Override
    public Instant occurredAt() {
        return occurredAt;
    }

    @Override
    public List<EventPayload> payloads() {
        return Collections.<EventPayload>singletonList(new OrderCreatedPayload(orderId));
    }
}
```

### Step 2：定义 payload

```java
public class OrderCreatedPayload implements EventPayload {

    private Long orderId;

    public OrderCreatedPayload() {
    }

    public OrderCreatedPayload(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
```

### Step 3：实现 handler

```java
@Component
public class SendWelcomeMessageHandler implements DomainEventHandler<OrderCreatedPayload> {

    @Override
    public String handlerCode() {
        return "send-welcome-message";
    }

    @Override
    public Class<OrderCreatedPayload> payloadClass() {
        return OrderCreatedPayload.class;
    }

    @Override
    public Optional<OrderCreatedPayload> buildPayload(DomainEvent event) {
        return Optional.of(new OrderCreatedPayload(event.bizId()));
    }

    @Override
    public EventHandleResult handle(
            DomainEvent event,
            OrderCreatedPayload payload,
            HandlerExecutionRecord record,
            EventExecutionContext context
    ) {
        // 这里写具体业务逻辑
        return EventHandleResult.FINISHED;
    }
}
```

### Step 4：定义 handler chain

```java
@Bean
public EventHandlerChain orderCreatedChain(SendWelcomeMessageHandler handler) {
    return new EventHandlerChain() {
        @Override
        public String bizCode() {
            return "order";
        }

        @Override
        public String eventType() {
            return "created";
        }

        @Override
        public List<DomainEventHandler<?>> handlers() {
            return Collections.<DomainEventHandler<?>>singletonList(handler);
        }
    };
}
```

### Step 5：在业务事务内落记录

当前版本还没有统一 facade，因此推荐业务方手动串以下三步：

```java
@Transactional
public void createOrder(Long orderId) {
    DomainEvent event = new OrderCreatedEvent(orderId, Instant.now());
    List<HandlerExecutionRecord> records = recordBuilder.build(event);
    eventStore.append(event, records);
}
```

这一步应该与业务状态变更放在同一个本地事务内。

### Step 6：事务提交后发布事件

```java
@Transactional
public void publishAfterOrderCreate(DomainEvent event) {
    domainEventPublisher.publishAfterCommit(event);
}
```

发布后，`SpringDomainEventListener` 会在事务提交后收到事件，并调用 `DefaultEventProcessor` 处理。

---

## 10. 当前最小工作流

当前版本推荐理解为下面这条链路：

```text
业务事务内
  -> 创建 DomainEvent
  -> DefaultHandlerExecutionRecordBuilder 构建 records
  -> EventStore.append(event, records)
  -> DomainEventPublisher.publishAfterCommit(event)

事务提交后
  -> SpringDomainEventListener 收到 DomainEvent
  -> DefaultEventProcessor.process(event)
  -> 如果 event 不带 records，则按 eventKey 从 EventStore.loadByEventKey(...) 回查
  -> 执行 handler
  -> EventStore.compareAndSet(...) 更新状态
```

---

## 11. 当前可参考的最小示例

仓库里已经有两个最小可运行示例测试：

- `src/test/java/com/sulin/codepose/event/framework/spring/InMemoryPipelineIntegrationTest.java`
- `src/test/java/com/sulin/codepose/event/framework/spring/MybatisPlusPipelineIntegrationTest.java`

其中：

- `InMemoryPipelineIntegrationTest` 适合看最小测试夹具 wiring
- `MybatisPlusPipelineIntegrationTest` 更接近正式接入路径

它们覆盖了：

- 注册 `EventHandlerChain`
- 实现 `DomainEventHandler`
- 构建 `HandlerExecutionRecord`
- `EventStore.append(...)`
- `publishAfterCommit(...)`
- `SpringDomainEventListener` 自动消费
- handler 执行后把 record 状态推进为 `FINISHED`

如果你要接正式环境，优先参考 MyBatis-Plus 版本测试。

---

## 12. 当前版本限制

以下内容在当前版本中**还未落地或尚未完善**：

### 12.1 还没有统一业务入口服务

当前业务方仍需要手动调用：

- `recordBuilder.build(event)`
- `eventStore.append(event, records)`
- `domainEventPublisher.publishAfterCommit(event)`

后续建议补一个统一 facade 或 service。

### 12.2 正式持久化依赖数据库

当前正式代码不再提供默认内存版 `EventStore`。

正式接入至少需要：

- 一个 `DataSource`
- 应用侧可用的 MyBatis-Plus 基础设施
- 已执行 `docs/domain_event_record.mysql.sql`
- 或者业务方自行提供 `EventStore`

如果既没有默认 store 条件，也没有自定义 `EventStore`，则不会启动：

- `DefaultReplayScanner`
- `DefaultEventProcessor`
- `DefaultEventReplayCoordinator`
- `SpringDomainEventListener`

测试环境仍然可以使用测试夹具版 `InMemoryEventStore`。

### 12.3 replay 仍是骨架

当前已有 replay scanner 和 replay coordinator，但以下能力后续还要继续演进：

- 更严格的 replay 分页分组控制
- 业务无关但可扩展的 replay strategy
- 更清晰的 replay 调度入口
- 更完整的“处理中记录恢复”语义

### 12.4 可观测性还未加入

当前还没有：

- metrics
- alarm hooks
- dead-letter
- trace logging 规范

### 12.5 数据库能力已落地，后续仍会继续增强

当前已经具备：

- MySQL 建表 SQL
- 默认 `MySQL + MyBatis-Plus` 的 `MybatisPlusEventStore`
- `eventKey + handlerCode` 数据库唯一索引
- H2 集成测试覆盖

后续仍可继续演进：

- 更强的运维与 replay 调度能力
- 更多非官方持久化适配由业务自行通过 `EventStore` 扩展

### 12.6 API 仍可能继续演进

虽然当前命名已经尽量贴近设计文档，但以下内容仍可能在后续迭代中微调：

- facade 入口模型
- replay 扩展点
- grouped handler 细节语义
- payload 版本化策略
- 默认 retry policy 能力

---

## 13. 当前接入建议

如果现在就基于这版骨架开始试接入，建议遵守下面几条：

- 先只接一个最简单的普通 handler 场景
- 正式环境直接使用 `MybatisPlusEventStore` 路径，非这套技术栈请自行实现 `EventStore`
- `handlerCode` 从一开始就按稳定标识设计，不要用类名
- handler 逻辑按幂等方式实现
- `eventKey` 保持稳定且全局唯一
- 不要让业务方直接依赖 `core` 内部细节，优先依赖 `api`
- `MybatisPlusPipelineIntegrationTest` 当作正式接入模板
- 把 `InMemoryPipelineIntegrationTest` 当作测试夹具模板

---

## 14. 当前测试验证情况

当前仓库已经补充了以下测试：

- 状态机测试
- registry 测试
- record builder 测试
- processor 测试
- 事务后发布测试
- 测试夹具版内存链路测试
- MyBatis-Plus store 集成测试
- MyBatis-Plus 端到端链路与 replay 测试
- MyBatis-Plus 自动装配回退测试

建议验证命令：

```bash
mvn test
```

---

## 15. 下一步文档建议迭代方向

后续这个文档建议继续补充这些内容：

1. 统一发布入口的推荐用法
2. 数据库接入的应用级最佳实践
3. 数据库表结构与索引设计
4. grouped handler 的标准用法示例
5. delay handler 的标准用法示例
6. replay 触发方式与运维说明
7. 接入规范与最佳实践
8. 常见问题与排错指南

---

## 16. 参考代码位置

当前最值得先读的文件：

- `src/main/java/com/sulin/codepose/event/framework/api/model/DomainEvent.java`
- `src/main/java/com/sulin/codepose/event/framework/api/model/HandlerExecutionRecord.java`
- `src/main/java/com/sulin/codepose/event/framework/api/handler/DomainEventHandler.java`
- `src/main/java/com/sulin/codepose/event/framework/api/chain/EventHandlerChain.java`
- `src/main/java/com/sulin/codepose/event/framework/api/store/EventStore.java`
- `src/main/java/com/sulin/codepose/event/framework/core/builder/DefaultHandlerExecutionRecordBuilder.java`
- `src/main/java/com/sulin/codepose/event/framework/core/chain/DefaultEventProcessor.java`
- `src/main/java/com/sulin/codepose/event/framework/spring/store/mybatis/MybatisPlusEventStore.java`
- `docs/domain_event_record.mysql.sql`
- `src/test/resources/sql/domain_event_record_h2.sql`
- `src/main/java/com/sulin/codepose/event/framework/spring/publish/SpringTransactionAwareEventPublisher.java`
- `src/main/java/com/sulin/codepose/event/framework/spring/config/DomainEventFrameworkAutoConfiguration.java`
- `src/main/java/com/sulin/codepose/event/framework/spring/config/DomainEventMybatisPlusStoreAutoConfiguration.java`
- `src/test/java/com/sulin/codepose/event/framework/spring/MybatisPlusPipelineIntegrationTest.java`
- `src/test/java/com/sulin/codepose/event/framework/spring/config/DomainEventMybatisPlusStoreAutoConfigurationTest.java`
- `src/test/java/com/sulin/codepose/event/framework/spring/InMemoryPipelineIntegrationTest.java`
