# 领域事件框架骨架设计（中文）

## 1. 文档目的

本文档用于沉淀一套可复用的“领域事件 + 本地事件表 + 事务后异步消费 + 补偿重试”框架骨架，目标是支持在独立目录中构建一个业务无关的通用框架，供多个业务域微服务复用。

这份文档聚焦两件事：

- 框架骨架代码结构图
- 核心接口定义

不包含具体业务实现代码，不直接依赖 `presale`、`order` 等业务概念。

---

## 2. 设计目标

这套框架需要满足以下目标：

- 支持业务事务内落本地事件表
- 支持事务提交后异步消费
- 支持按 handler 粒度跟踪执行状态
- 支持失败重试、延迟执行、分组 handler
- 支持业务服务通过扩展点接入，而不是修改框架核心代码
- 支持未来独立抽成共享 jar 或 starter

同时明确几个边界：

- 它不是分布式事务框架
- 它不是 MQ 的替代品
- 它天然是 at-least-once 语义，因此业务 handler 必须幂等

---

## 3. 推荐目录结构

### 3.1 第一版：单模块独立项目

建议先做成单模块，先把抽象跑通，再考虑拆分多模块。

```text
domain-event-framework/
  pom.xml
  src/main/java/com/sinew/event/framework/
    api/
      model/
        DomainEvent.java
        EventPayload.java
        HandlerExecutionRecord.java
        ExecutionStatus.java
        EventHandleResult.java
      handler/
        DomainEventHandler.java
        DelayableEventHandler.java
        GroupedEventHandler.java
      chain/
        EventExecutionContext.java
        EventHandlerChain.java
        EventHandlerChainRegistry.java
      publish/
        DomainEventPublisher.java
      store/
        EventStore.java
        ReplayScanRequest.java
      policy/
        RetryPolicy.java
      serialize/
        EventPayloadSerializer.java

    core/
      builder/
        DefaultHandlerExecutionRecordBuilder.java
      chain/
        DefaultEventExecutionContext.java
        DefaultEventProcessor.java
        AbstractGroupedEventHandler.java
      registry/
        InMemoryEventHandlerChainRegistry.java
      replay/
        DefaultEventReplayCoordinator.java
      scheduler/
        DefaultReplayScanner.java
      store/
        EventRecordStateMachine.java

    spring/
      config/
        DomainEventFrameworkAutoConfiguration.java
      listener/
        SpringDomainEventListener.java
      publish/
        SpringTransactionAwareEventPublisher.java
```

### 3.2 第二版：成熟后可拆多模块

如果后续要做成稳定共享框架，可以拆成下面四层：

```text
event-framework-api
event-framework-core
event-framework-spring-boot-starter
event-framework-mybatis
```

推荐拆分原则：

- `api`：只放接口、模型、枚举
- `core`：只放通用执行逻辑，不依赖具体 Spring 容器能力
- `starter`：负责自动装配、监听器、事务后发布
- `mybatis`：负责默认持久化实现

---

## 4. 核心设计思想

### 4.1 框架只关心通用元数据

框架层只认以下几类稳定字段：

- `bizCode`
- `bizId`
- `eventType`
- `eventKey`
- `handlerCode`

框架不应该知道任何具体业务枚举，不应该直接依赖某个业务域的 `EventType` 实现。

### 4.2 本地事件表按 handler 粒度持久化

推荐继续保留“一个 handler 一条记录”的模型，而不是“一条事件一条记录”。

这样做的优点：

- 可以按 handler 粒度独立重试
- 某个 handler 失败不影响已完成 handler
- 方便支持延迟执行
- 方便支持 grouped handler 的主/子状态流转

### 4.3 replay 不依赖重建复杂业务 Event

框架 replay 时，不要求先还原完整业务事件对象。

更推荐的方式是：

- 从本地事件表读取一组 `HandlerExecutionRecord`
- 用一份轻量级 `DomainEvent` 元数据对象承载 `bizCode`、`bizId`、`eventType`、`eventKey`
- 按 `handlerCode` 找到 handler
- 将当前 record 的 payload 反序列化为 handler 自己关心的 payload
- 直接执行 handler

这样抽成共享框架会更稳定。

### 4.4 handler 身份必须稳定

不能继续用 `getClass().getSimpleName()` 作为持久化标识。

必须引入稳定的 `handlerCode`，原因如下：

- 类名重构不能影响 replay
- 不同包下可能出现同名类
- 共享框架需要长期可演进的持久化契约

---

## 5. 核心抽象关系图

```text
业务聚合
  -> 构造 DomainEvent
  -> framework 根据 handler chain 构造 HandlerExecutionRecord
  -> EventStore 在业务事务内落库
  -> 事务提交后 DomainEventPublisher 发布事件
  -> SpringDomainEventListener 接收事件
  -> DefaultEventProcessor 执行 handler chain
  -> EventStore 更新每条 handler record 状态

补偿任务
  -> DefaultReplayScanner 扫描待重试记录
  -> DefaultEventReplayCoordinator 按 eventKey 分组
  -> 生成轻量 DomainEvent
  -> DefaultEventProcessor 重放 handler
  -> EventStore 更新状态
```

---

## 6. 核心接口定义

下面的接口定义，是这套框架建议优先稳定下来的最小 API 面。

### 6.1 `DomainEvent`

职责：表示一条业务域事件，但框架只要求它暴露基础元数据和原始 payload 集合。

```java
package com.sinew.event.framework.api.model;

import java.time.Instant;
import java.util.List;

public interface DomainEvent {

    String bizCode();

    Long bizId();

    String eventType();

    /**
     * 全局唯一事件键。
     * 建议格式：bizCode_bizId_eventType_uuid
     * 也可以由业务方自定义。
     */
    String eventKey();

    Instant occurredAt();

    /**
     * 原始事件 payload 集合。
     * 由各 handler 决定是否关心、如何提取。
     */
    List<EventPayload> payloads();
}
```

设计说明：

- `DomainEvent` 是框架入口模型
- 它不要求一定是某个复杂领域对象
- replay 时甚至可以只构造一个轻量实现

### 6.2 `EventPayload`

职责：标记业务事件中的原始载荷对象。

```java
package com.sinew.event.framework.api.model;

public interface EventPayload {
}
```

设计说明：

- 这是业务侧扩展点
- 不同 handler 可以从同一个 `DomainEvent` 中提取不同 payload

### 6.3 `ExecutionStatus`

职责：表示本地事件表中一条 handler 执行记录的状态。

```java
package com.sinew.event.framework.api.model;

public enum ExecutionStatus {
    PENDING,
    PROCESSING,
    FINISHED,
    ABORT,
    GROUP_MAIN_FINISHED,
    GROUP_MAIN_FINISHED_SUB_ABORT
}
```

设计说明：

- `PENDING`：待执行或待重试
- `PROCESSING`：已进入执行中
- `FINISHED`：执行完成
- `ABORT`：终止，不再继续
- 后两个状态用于 grouped handler 场景

### 6.4 `EventHandleResult`

职责：表示一次 handler 执行的结果。

```java
package com.sinew.event.framework.api.model;

public enum EventHandleResult {
    FINISHED,
    FAIL,
    ABORT,
    GROUP_MAIN_FINISHED,
    GROUP_MAIN_FINISHED_SUB_ABORT;

    public boolean isFinished() {
        return this == FINISHED || this == ABORT;
    }
}
```

设计说明：

- `EventHandleResult` 是运行态结果
- `ExecutionStatus` 是持久化态状态
- 两者应通过状态机转换，而不是直接混用

### 6.5 `HandlerExecutionRecord`

职责：表示本地事件表中的一条 handler 执行记录。

```java
package com.sinew.event.framework.api.model;

import java.time.Instant;
import java.time.LocalDateTime;

public record HandlerExecutionRecord(
        Long id,
        String eventKey,
        String bizCode,
        Long bizId,
        String eventType,
        String handlerCode,
        String parentHandlerCode,
        String payload,
        Integer payloadVersion,
        ExecutionStatus status,
        Integer retryNum,
        LocalDateTime executeTime,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
```

设计说明：

- 推荐增加 `eventKey`
- 推荐增加 `payloadVersion`
- 推荐增加 `version` 做 CAS 更新
- `handlerCode` 必须稳定

### 6.6 `DomainEventHandler<P>`

职责：handler 的核心抽象。

```java
package com.sinew.event.framework.api.handler;

import com.sinew.event.framework.api.chain.EventExecutionContext;
import com.sinew.event.framework.api.model.DomainEvent;
import com.sinew.event.framework.api.model.EventHandleResult;
import com.sinew.event.framework.api.model.HandlerExecutionRecord;

import java.util.Optional;

public interface DomainEventHandler<P> {

    /**
     * 稳定 handler 标识。
     */
    String handlerCode();

    /**
     * 当前 handler 对应的 payload 类型。
     * 主要用于 replay 时反序列化。
     */
    Class<P> payloadClass();

    /**
     * 首次发布时，从 DomainEvent 中提取当前 handler 关心的 payload。
     * 不关心则返回 Optional.empty()。
     */
    Optional<P> buildPayload(DomainEvent event);

    /**
     * 执行 handler。
     */
    EventHandleResult handle(
            DomainEvent event,
            P payload,
            HandlerExecutionRecord record,
            EventExecutionContext context
    );

    /**
     * 子 handler 可覆写父 handler 标识。
     */
    default String parentHandlerCode() {
        return null;
    }
}
```

设计说明：

- 这版接口里，handler 直接对自己关心的 payload 类型 `P` 负责
- 首次发布和 replay 都能复用这套约束
- 业务接入方只需要实现自己的 handler，不需要改核心 replay 逻辑

### 6.7 `DelayableEventHandler<P>`

职责：支持延迟执行。

```java
package com.sinew.event.framework.api.handler;

import com.sinew.event.framework.api.model.DomainEvent;

import java.time.LocalDateTime;

public interface DelayableEventHandler<P> extends DomainEventHandler<P> {

    LocalDateTime executeTime(DomainEvent event, P payload);
}
```

设计说明：

- 首次构建 record 时，框架会根据 `executeTime` 写入执行时间
- 延迟 handler 初始状态通常是 `PENDING`

### 6.8 `GroupedEventHandler<P>`

职责：支持主/子 handler 编排。

```java
package com.sinew.event.framework.api.handler;

import java.util.List;

public interface GroupedEventHandler<P> extends DomainEventHandler<P> {

    List<DomainEventHandler<?>> subHandlers();
}
```

设计说明：

- `GroupedEventHandler` 只是表达能力接口
- 真正的主/子协调逻辑建议放到 core 层的抽象类里

### 6.9 `EventExecutionContext`

职责：在一次事件执行链中共享上下文结果。

```java
package com.sinew.event.framework.api.chain;

import com.sinew.event.framework.api.model.EventHandleResult;

import java.util.Map;
import java.util.Optional;

public interface EventExecutionContext {

    void putResult(String handlerCode, EventHandleResult result);

    Optional<EventHandleResult> getResult(String handlerCode);

    Map<String, EventHandleResult> results();
}
```

设计说明：

- 用于记录前置 handler 执行结果
- grouped handler 和后置 handler 都可以读取上下文

### 6.10 `EventHandlerChain`

职责：定义一类事件的 handler 链。

```java
package com.sinew.event.framework.api.chain;

import com.sinew.event.framework.api.handler.DomainEventHandler;

import java.util.List;

public interface EventHandlerChain {

    String bizCode();

    String eventType();

    /**
     * 只返回顶层 handler。
     * 子 handler 由 grouped handler 自己持有。
     */
    List<DomainEventHandler<?>> handlers();
}
```

设计说明：

- 框架通过 `bizCode + eventType` 定位 handler chain
- 这也是业务接入时最重要的扩展点之一

### 6.11 `EventHandlerChainRegistry`

职责：注册和查找 handler chain。

```java
package com.sinew.event.framework.api.chain;

import com.sinew.event.framework.api.model.DomainEvent;

import java.util.Optional;

public interface EventHandlerChainRegistry {

    Optional<EventHandlerChain> getChain(String bizCode, String eventType);

    default Optional<EventHandlerChain> getChain(DomainEvent event) {
        return getChain(event.bizCode(), event.eventType());
    }
}
```

设计说明：

- 比 `Factory` 更适合表达“注册并查找”语义
- 后续业务接入时可以通过 Spring 扫描或显式注册

### 6.12 `EventPayloadSerializer`

职责：负责 handler payload 的序列化与反序列化。

```java
package com.sinew.event.framework.api.serialize;

public interface EventPayloadSerializer {

    <T> String serialize(T payload, Integer version);

    <T> T deserialize(String content, Class<T> payloadClass, Integer version);
}
```

设计说明：

- 建议 payload 独立版本化
- replay 的兼容性依赖这个接口的稳定性

### 6.13 `DomainEventPublisher`

职责：负责发布事件。

```java
package com.sinew.event.framework.api.publish;

import com.sinew.event.framework.api.model.DomainEvent;

public interface DomainEventPublisher {

    /**
     * 当前线程立即发布。
     */
    void publishNow(DomainEvent event);

    /**
     * 事务提交后发布。
     */
    void publishAfterCommit(DomainEvent event);
}
```

设计说明：

- 对业务方来说，最重要的是 `publishAfterCommit`
- Spring 适配层可以基于 `TransactionSynchronizationManager` 实现

### 6.14 `ReplayScanRequest`

职责：封装 replay 扫描条件。

```java
package com.sinew.event.framework.api.store;

import java.time.Instant;
import java.util.List;

public record ReplayScanRequest(
        List<String> bizCodes,
        Long lastId,
        Integer limit,
        Integer maxRetryNum,
        Instant createdBefore,
        Instant executeBefore
) {
}
```

设计说明：

- 用来统一补偿扫描条件
- 方便底层持久化实现做分页与条件扩展

### 6.15 `EventStore`

职责：本地事件表的持久化抽象。

```java
package com.sinew.event.framework.api.store;

import com.sinew.event.framework.api.model.DomainEvent;
import com.sinew.event.framework.api.model.ExecutionStatus;
import com.sinew.event.framework.api.model.HandlerExecutionRecord;

import java.util.List;

public interface EventStore {

    /**
     * 业务事务内追加事件记录。
     */
    void append(DomainEvent event, List<HandlerExecutionRecord> records);

    /**
     * 带并发保护的状态更新。
     * 推荐底层实现为 id + version 或 id + old_status 的 CAS。
     */
    boolean compareAndSet(
            Long recordId,
            Long expectedVersion,
            ExecutionStatus expectedStatus,
            HandlerExecutionRecord nextRecord
    );

    /**
     * 扫描待 replay 的记录。
     */
    List<HandlerExecutionRecord> scanRetryable(ReplayScanRequest request);

    /**
     * 按 eventKey 拉取完整事件组。
     */
    List<HandlerExecutionRecord> loadByEventKey(String eventKey);
}
```

设计说明：

- `append` 在业务事务内调用
- `compareAndSet` 是框架持久化契约的关键，避免并发 replay 覆盖状态
- 推荐持久化层围绕 `eventKey + handlerCode` 建唯一约束

### 6.16 `RetryPolicy`

职责：统一重试策略。

```java
package com.sinew.event.framework.api.policy;

import com.sinew.event.framework.api.model.HandlerExecutionRecord;

import java.time.LocalDateTime;

public interface RetryPolicy {

    int maxRetryCount(String bizCode, String eventType, String handlerCode);

    LocalDateTime nextExecuteTime(HandlerExecutionRecord record);

    default boolean canRetry(HandlerExecutionRecord record) {
        return record.retryNum() < maxRetryCount(
                record.bizCode(),
                record.eventType(),
                record.handlerCode()
        );
    }
}
```

设计说明：

- 重试次数、回退策略、死信阈值都可以从这里衍生
- 业务方可以覆盖默认实现

---

## 7. core 层建议提供的默认实现

框架 `core` 层建议至少提供下面几类默认实现。

### 7.1 `DefaultHandlerExecutionRecordBuilder`

职责：首次发布事件时，根据 `DomainEvent + EventHandlerChain` 生成一组 `HandlerExecutionRecord`。

它负责：

- 遍历顶层 handler
- 调用 `handler.buildPayload(event)`
- 如果 handler 关心该事件，则构造 record
- 如果 handler 是延迟 handler，则写入 `executeTime`
- 初始化 `status`、`retryNum`、`payloadVersion`

### 7.2 `DefaultEventProcessor`

职责：执行 handler 链。

它负责：

- 读取当前事件对应的 chain
- 加载或匹配当前 handler 的 record
- 反序列化 record payload
- 调用 handler 执行
- 根据 `EventHandleResult` 更新 record 状态
- 处理 grouped handler 的主/子流转

### 7.3 `AbstractGroupedEventHandler`

职责：承接 grouped handler 的通用主/子协调逻辑。

建议把以下能力统一下沉到抽象类：

- 主 handler 先执行
- 根据主 handler 结果决定子 handler 是否继续
- 子 handler 失败时如何回写状态
- 主 handler 与子 handler 的状态联动

### 7.4 `DefaultEventReplayCoordinator`

职责：负责 replay 编排。

它负责：

- 调用扫描器拿到待重试记录
- 按 `eventKey` 分组
- 构建轻量级 `DomainEvent`
- 找到 chain 后重放
- 更新状态

### 7.5 `DefaultReplayScanner`

职责：封装补偿扫描逻辑。

它负责：

- 按状态筛选 `PENDING`、`PROCESSING`、分组中间态
- 按 `executeTime`、`retryNum`、创建时间等条件筛选
- 保证分页时不拆开同一个 `eventKey`

---

## 8. spring 适配层建议职责

### 8.1 `SpringTransactionAwareEventPublisher`

职责：事务后发布。

建议行为：

- `publishNow`：直接使用 Spring `ApplicationEventPublisher`
- `publishAfterCommit`：基于事务同步机制，在 `afterCommit` 中发布

### 8.2 `SpringDomainEventListener`

职责：监听发布出的 `DomainEvent` 并交给 `DefaultEventProcessor` 执行。

建议行为：

- 支持异步线程池执行
- 记录执行日志与异常日志
- 不把异常直接抛回业务线程

### 8.3 `DomainEventFrameworkAutoConfiguration`

职责：自动装配框架默认实现。

建议负责注入：

- `DomainEventPublisher`
- `EventHandlerChainRegistry`
- `DefaultEventProcessor`
- `DefaultEventReplayCoordinator`
- `RetryPolicy`
- `EventPayloadSerializer`

---

## 9. 业务服务接入框架时需要实现什么

业务服务只需要关心下面几件事：

- 定义自己的 `DomainEvent` 实现
- 定义自己的 `EventPayload` 类型
- 定义自己的 `DomainEventHandler`
- 定义自己的 `EventHandlerChain`
- 注册到 `EventHandlerChainRegistry`

业务服务不应该修改下面这些框架组件：

- replay 协调器
- 执行处理器
- 本地事件表状态机
- 事务后发布逻辑
- 通用扫描器

这也是判断框架边界是否设计合理的重要标准。

---

## 10. 关键落地约束

在真正开始编码前，建议先把以下几个约束定成框架契约：

### 10.1 handler 必须幂等

因为这套框架天然是 at-least-once 语义。

### 10.2 handlerCode 必须稳定

不能使用类名、不能依赖重构敏感标识。

### 10.3 record 更新必须带并发保护

推荐：

- `id + version` CAS
- 或 `id + oldStatus` CAS

### 10.4 payload 必须版本化

建议至少引入：

- `payload`
- `payloadVersion`

### 10.5 replay 分页不能拆开事件组

必须按 `eventKey` 保证完整分组，不允许同一组 record 被拆到两页处理。

---

## 11. 推荐的第一批编码顺序

如果后面正式开始实现，我建议先按下面顺序落地：

1. 先建 `api` 层接口和模型
2. 再建 `core` 层的 `DefaultHandlerExecutionRecordBuilder`
3. 再建 `EventStore` 默认实现和表结构
4. 再建 `DefaultEventProcessor`
5. 再建 `SpringTransactionAwareEventPublisher` 和 `SpringDomainEventListener`
6. 最后建 `DefaultEventReplayCoordinator` 与 `DefaultReplayScanner`

这样推进的好处是：

- API 先稳定
- 发布链路和 replay 链路能逐步打通
- 不会一开始就陷入具体业务适配细节

---

## 12. 总结

这套框架骨架的核心思想可以概括为三句话：

- 用 `DomainEvent` 表达事件元数据
- 用 `HandlerExecutionRecord` 表达本地事件表中的执行单元
- 用 `DomainEventHandler + EventHandlerChain + EventStore` 构成可复用的执行与补偿框架

如果后续继续推进实现，建议下一步直接输出两类内容：

- 建表 SQL 与 MyBatis 持久化接口骨架
- 一套最小可运行 demo：事件发布、落表、事务后监听、失败重试
