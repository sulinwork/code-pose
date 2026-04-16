# 领域事件框架设计文档

## 1. 背景

当前 `com.sinew.marketing.core.domain.event` 下的实现，其实已经形成了一套比较完整、可落地的本地事件表架构：

- 领域模型在业务事务中创建领域事件
- 事件记录在事务提交前落到本地事件表
- 框架在事务提交后发布进程内事件，异步处理
- 失败或延迟执行的 handler 可以从本地事件表中重放
- 事件执行状态是按 handler 粒度跟踪的，而不是只跟踪到 event 粒度

这套机制在单一业务域中已经很好用，但当前实现仍然和具体业务代码耦合较深，比如 `presale`、`presale_order`。如果后续其他业务微服务也要复用这套机制，就需要把当前实现进一步抽象成一个可复用的通用框架。

## 2. 目标

构建一套基于本地事件表的可复用领域事件框架，供多个业务微服务接入。

### 2.1 功能目标

- 支持事务内落本地事件记录
- 支持事务提交后异步事件分发
- 支持按 handler 粒度跟踪执行状态
- 支持失败重试与延迟执行
- 支持 grouped handler 编排
- 支持各业务服务独立注册事件类型和 handler chain
- 支持框架级别的可观测性与告警能力

### 2.2 工程目标

- 框架核心不能依赖任何具体业务域，如 presale、order
- 新业务接入应通过实现扩展点完成，而不是修改框架代码
- 框架的包结构与抽象命名要足够稳定，便于后续抽成共享库
- 从当前实现迁移到框架化实现时，应尽量支持渐进式演进

### 2.3 非目标

- 不是分布式事务框架
- 不是消息中间件的替代品
- 不保证 exactly-once
- 不是任意长流程编排引擎

## 3. 核心设计原则

### 3.1 事务优先

事件记录必须和业务状态变更处于同一个本地事务中。

### 3.2 at-least-once 语义

框架应接受 at-least-once 的投递语义，并要求业务 handler 必须幂等。

### 3.3 按 handler 粒度持久化

执行状态应按 handler 粒度存储，而不是只按 event 粒度存储。这样才能支持部分成功、独立重试、延迟 handler 以及 grouped handler 编排。

### 3.4 框架核心业务无关

框架核心不应该感知具体业务域。它只应理解通用概念，例如 event type、payload、handler chain、执行状态、replay。

### 3.5 扩展点清晰

业务服务应通过注册、解析、handler 实现等方式接入框架，而不是侵入框架内部实现。

## 4. 目标包结构

如果第一版仍在当前仓库中演进，推荐包结构如下：

```text
src/main/java/com/sinew/marketing/core/domain/event/framework/
  model/
  handler/
  chain/
  registry/
  replay/
  store/
  scheduler/
  support/
```

如果后续要抽成共享库，建议将根包调整为业务无关的形式，例如：

```text
com.sinew.event.framework
```

## 5. 分层架构

### 5.1 框架核心层

负责通用事件处理行为。

职责包括：

- 定义框架级事件模型
- 管理本地事件表持久化
- 执行 handler chain
- 记录 handler 执行状态
- 执行重试与延迟重放
- 暴露指标与告警钩子

### 5.2 业务适配层

由各业务微服务自行实现。

职责包括：

- 定义具体事件类型
- 定义 payload 模型与序列化方式
- 注册 handler chain
- 实现 handler
- 提供 replay 时需要的 event type 和 payload 解析能力
- 在需要时提供业务定制的 retry policy

### 5.3 业务领域层

负责领域聚合状态流转，并决定何时发出领域事件。

职责包括：

- 从聚合行为中创建领域事件
- 在一个事务中同时持久化业务状态和事件记录
- 依赖框架完成分发与 replay

## 6. 推荐核心抽象

### 6.1 模型抽象

#### `DomainEvent`

表示一条待由框架处理的领域事件。

推荐字段：

- `bizCode`
- `bizId`
- `eventType`
- `eventId` 或 `eventKey`
- `occurredAt`
- `records`

#### `HandlerExecutionRecord`

表示一个 handler 对应的一条持久化执行记录。

推荐字段：

- `id`
- `bizCode`
- `bizId`
- `eventType`
- `handlerCode`
- `parentHandlerCode`
- `payload`
- `payloadType`
- `status`
- `retryNum`
- `executeTime`
- `version`
- `createdTime`
- `updatedTime`

### 6.2 handler 抽象

#### `DomainEventHandler<T extends DomainEvent>`

基础 handler 抽象。

职责：

- 声明自己关心的 payload 类型或 payload 范围
- 处理事件
- 返回执行结果

#### `DelayableEventHandler<T extends DomainEvent>`

用于支持延迟执行的可选扩展接口。

#### `GroupedEventHandler<T extends DomainEvent>`

用于支持主/子 handler 编排的可选扩展接口。

### 6.3 chain 抽象

#### `EventHandlerChain<T extends DomainEvent>`

定义某类事件对应的有序 handler 链。

#### `EventExecutionContext`

承载一次执行过程中共享的上下文，例如前置 handler 结果、诊断信息等。

### 6.4 registry 抽象

#### `EventTypeRegistry`

用于把 `bizCode + eventType` 映射到具体的事件类型定义。

#### `EventHandlerChainRegistry`

用于把事件定义映射到其对应的 handler chain。

#### `EventReplayStrategyRegistry`

用于把 `bizCode` 映射到对应的 replay strategy。

### 6.5 replay 抽象

#### `EventReplayStrategy`

用于承载业务相关的 replay 逻辑。

推荐职责：

- 从持久化记录中解析 payload
- 根据存储值解析 event type
- 在需要时定制 replay 分组 key
- 在需要时定制可重试判断逻辑

#### `EventReplayCoordinator`

框架核心中的通用 replay 编排器。

职责：

- 对待重试记录进行分组
- 从持久化记录重建领域事件
- 解析 handler chain 并执行 replay

### 6.6 持久化抽象

#### `EventStore`

框架层的持久化抽象。

职责：

- 保存新的 handler 执行记录
- 更新 handler 执行状态
- 查询待重试记录
- 按 event key 查询完整事件组

## 7. 推荐命名规范

从一开始就建议采用框架中性命名。

| 当前概念 | 推荐框架命名 |
| --- | --- |
| `Event` | `DomainEvent` |
| `EventHandlerInfo` | `HandlerExecutionRecord` |
| `EventRepository` | `EventStore` |
| `EventFactory` | `DomainEventFactory` |
| `EventHandler` | `DomainEventHandler` |
| `EventDelayHandler` | `DelayableEventHandler` |
| `EventGroupHandler` | `GroupedEventHandler` |
| `EventChainContext` | `EventExecutionContext` |
| `EventHandlerChainFactory` | `EventHandlerChainRegistry` |
| `EventDomainService` | `EventReplayCoordinator` |
| `EventReplayStrategyFactory` | `EventReplayStrategyRegistry` |

## 8. 端到端事件生命周期

### 8.1 发布链路

1. 聚合执行业务行为。
2. 聚合创建 `DomainEvent`。
3. 框架解析对应的 handler chain。
4. 框架为每个适用的 handler 创建一条 `HandlerExecutionRecord`。
5. 业务事务同时持久化聚合状态和事件记录。
6. 事务提交后，框架发布进程内事件，异步处理。

### 8.2 消费链路

1. 监听器接收到已提交的 `DomainEvent`。
2. 框架解析对应的 handler chain。
3. 每个 handler 根据当前状态和执行时间决定是否执行。
4. 框架在每次 handler 返回结果后更新其对应 record 的状态。

### 8.3 replay 链路

1. 调度器从本地事件表中扫描出待重试记录。
2. 框架按事件标识对记录进行分组。
3. 框架通过 `EventReplayStrategy` 重建 event type 和 payload。
4. 框架从持久化记录重建 `DomainEvent`。
5. 框架重放同一套 handler chain。
6. replay 后再次更新记录状态。

## 9. 状态模型

推荐状态如下：

- `PENDING`
- `PROCESSING`
- `FINISHED`
- `ABORT`
- `GROUP_MAIN_FINISHED`
- `GROUP_MAIN_FINISHED_SUB_ABORT`

推荐状态流转规则：

- 成功 -> `FINISHED`
- handler 失败 -> `PENDING` 并增加 retry 次数
- handler 抛异常 -> `PENDING` 并增加 retry 次数
- 不可重试的业务终止 -> `ABORT`
- grouped handler 主处理成功但子处理待执行 -> `GROUP_MAIN_FINISHED`
- grouped handler 主处理成功但子处理需终止 -> `GROUP_MAIN_FINISHED_SUB_ABORT`

## 10. 数据库设计建议

建议继续采用“一条 handler 执行记录一行”的模型。

推荐字段：

- `id`
- `biz_code`
- `biz_id`
- `event_type`
- `event_key`
- `handler_code`
- `parent_handler_code`
- `payload`
- `payload_type`
- `status`
- `retry_num`
- `execute_time`
- `version`
- `created_time`
- `updated_time`
- `delete_mark`

推荐索引：

- `event_key + handler_code` 唯一索引
- `biz_code + status + execute_time` 普通索引
- `biz_code + biz_id + event_type` 普通索引
- `created_time` 普通索引

## 11. 关键框架契约

### 11.1 稳定的 handler 身份

不要再把 simple class name 持久化为 handler 标识。应引入稳定的 `handlerCode`，由 handler 自己声明。

原因：

- 类重命名不能破坏 replay
- 不同 package 下可能存在同名类
- handler 身份必须能跨重构保持稳定

### 11.2 handler 必须幂等

每个 handler 都必须能够安全地重复执行。

原因：

- replay 本质上是 at-least-once
- 进程内重复投递是可能发生的
- 异常场景下可能出现并发重试窗口

### 11.3 状态更新需要乐观并发保护

每次更新持久化记录状态时，都应带上保护条件，例如 `id + version` 或 `id + oldStatus`。

原因：

- 防止多个 replay worker 相互覆盖状态
- 让状态流转更明确、更可审计

### 11.4 payload 需要版本化

建议持久化 payloadVersion，以支持历史事件 replay 时的 schema 演进。

## 12. 扩展点设计

建议对业务接入方暴露以下扩展点：

### 12.1 `EventPayloadSerializer`

职责：

- 将事件 payload 序列化成存储字符串
- 将存储字符串反序列化回强类型对象
- 支持 payload 版本演进

### 12.2 `EventTypeResolver`

职责：

- 将存储中的 `bizCode + eventType` 解析为框架中的事件类型定义

### 12.3 `EventHandlerChainProvider`

职责：

- 为某个事件类型提供有序 handler chain

### 12.4 `EventReplayStrategy`

职责：

- 从持久化记录中重建 replay 所需的事件模型
- 在需要时定制 replay 分组或过滤逻辑

### 12.5 `RetryPolicy`

职责：

- 决定最大重试次数
- 决定回退策略
- 决定死信阈值

## 13. 可观测性与运维能力

框架应暴露指标与运维钩子。

推荐指标：

- 按 `bizCode` 统计待处理记录数
- 按 `bizCode` 统计处理中记录数
- replay 吞吐量
- handler 成功率
- handler retry 次数分布
- 最老 pending 记录年龄
- 死信数量

推荐告警：

- 最老 pending 记录超过阈值
- 某 handler 重复失败次数超过阈值
- replay 调度延迟超过阈值
- 死信数量短时间内突然升高

## 14. 当前实现到目标框架的映射关系

建议的映射关系如下：

| 当前类 | 目标角色 |
| --- | --- |
| `domain.event.Event` | `framework.model.DomainEvent` |
| `domain.event.EventHandlerInfo` | `framework.model.HandlerExecutionRecord` |
| `domain.event.EventFactory` | `framework.support.DomainEventFactory` |
| `domain.event.EventListener` | `framework.support.DomainEventListener` |
| `domain.event.EventPublisher` | `framework.support.DomainEventPublisher` |
| `domain.event.repository.EventRepository` | `framework.store.EventStore` |
| `domain.event.chain.AbstractEventHandlerChain` | `framework.chain.AbstractDomainEventHandlerChain` |
| `domain.event.handler.AbstractEventGroupHandler` | `framework.handler.AbstractGroupedEventHandler` |
| `domain.event.service.EventDomainService` | `framework.replay.EventReplayCoordinator` |
| `core.service.EventQueryInnerService` | `framework.scheduler.EventRecordQueryService` |

## 15. 演进路径

### Phase 1

在不改变业务行为的前提下，先清理当前实现。

- 引入稳定的 `handlerCode`
- 用 strategy 替代直接写死的业务 replay 解析逻辑
- 为状态更新增加乐观并发保护
- 修复 replay 查询分组逻辑

### Phase 2

抽出通用接口，并把通用类移动到 `framework/` 包下。

### Phase 3

把 presale 相关实现移动为 adapter，放到 `domain/presale/event/` 下。

### Phase 4

在一个或两个额外服务验证通过后，把框架抽成内部共享库。

## 16. 风险与权衡

- 通用框架会带来一定抽象成本，因此接口要尽量小而明确
- grouped handler 很强大，但也会增加理解成本
- 按 handler 粒度持久化会提升 replay 精度，但也会增加表数据量
- 过早抽库可能会冻结错误抽象，建议至少有两个业务域验证后再稳定共享 API

## 17. 建议结论

建议以当前本地事件表模型为基础构建第一版可复用框架，因为当前实现已经具备了正确的核心原语：事务内持久化、事务后异步分发、按 handler 粒度跟踪 replay 状态、延迟执行、grouped 编排。

真正要做的，不是重新发明一套机制，而是把“通用框架职责”和“业务域解析职责”清晰拆开，让其他微服务后续可以通过注册和扩展点接入，而不是复制粘贴当前实现。