# 领域事件框架落地方案文档

## 1. 文档目的

本文档描述如何把当前的本地事件表实现逐步演进为一个可复用的领域事件框架，以及如何分阶段、安全地完成这项落地工作。

## 2. 当前状态总结

当前实现的优点：

- 事件记录是在业务事务内持久化的
- 事件分发发生在事务提交之后
- 已经具备失败重试和延迟执行能力
- 已经具备 grouped handler 模式
- 已经具备按 handler 粒度持久化状态的能力

在真正抽成框架之前，需要优先解决的问题：

- replay 逻辑中仍然存在业务分支硬编码
- handler 身份仍然使用 simple class name 持久化
- 状态更新缺少足够强的并发保护
- replay 分批查询逻辑有可能把同一个事件组错误拆开
- payload 兼容策略还没有显式定义
- 包命名和类抽象仍然偏当前业务项目语义

## 3. 落地策略

建议按四个阶段推进。

### Stage 1：稳定当前实现

目标：

在不改变业务行为的前提下，消除最明显的技术耦合和风险点。

任务：

- 在 handler 抽象与持久化模型中引入 `handlerCode`
- 为执行记录更新增加 `version` 或 `oldStatus` 保护条件
- 修复 replay 分组查询，保证一个事件组不会被拆分页
- 用 `EventReplayStrategy` 替代硬编码 replay 解析逻辑
- 增加基础 metrics 和 alarm hooks

退出标准：

- 当前 presale 链路无业务回归
- replay 路径不再直接感知具体业务枚举
- 多个 replay worker 并发执行时，记录状态更新具备足够安全性

### Stage 2：在当前仓库中抽出框架接口

目标：

在仍然留在当前服务仓库的前提下，把通用接口和业务 adapter 代码拆开。

任务：

- 引入 `framework/model`、`framework/handler`、`framework/chain`、`framework/store`、`framework/replay`、`framework/registry`
- 把通用抽象和默认实现迁移到 `framework/`
- 保留业务适配代码在 `domain/presale/event/`
- 引入适配器注册对象，例如 `EventTypeResolver`、`EventHandlerChainProvider`、`EventPayloadSerializer`

退出标准：

- framework 包内不再 import presale 相关类
- presale 接入只通过 framework 定义的扩展点完成

### Stage 3：在第二个业务域或微服务中验证

目标：

验证这套抽象是真的可复用，而不是只适配 presale。

任务：

- 接入第二个业务域，具备不同事件类型和 handler
- 复用同一套本地事件表模型与 replay 基础设施
- 验证延迟 handler、grouped handler、普通 handler 在第二个业务域中的适用性
- 收集对命名、扩展点、默认实现的反馈

退出标准：

- 第二个接入方无需修改框架核心代码即可完成接入
- presale 相关假设不再泄漏到新接入方

### Stage 4：抽成共享库

目标：

将框架正式发布为内部共享依赖。

任务：

- 将 framework 包迁移到独立模块或内部共享库
- 定义版本策略和兼容性边界
- 提供 starter 配置和接入示例
- 提供默认指标与日志规范

退出标准：

- 至少两个服务运行在同一套框架 API 上
- 框架 API 的扩展点趋于稳定
- 升级路径有明确文档说明

## 4. 建议的目标结构

### 4.1 框架模块结构

```text
framework/
  model/
  handler/
  chain/
  replay/
  registry/
  store/
  scheduler/
  support/
```

### 4.2 业务适配结构

```text
domain/<biz>/event/
  types/
  payload/
  handler/
  chain/
  replay/
  adapter/
```

## 5. 详细落地计划

### 5.1 Step A：引入稳定的 handler 身份

原因：

当前持久化使用 simple class name 作为 handler 标识，这在重构场景下非常脆弱，不适合作为长期框架契约。

落地方式：

- 在 handler 抽象中新增 `handlerCode()`
- 持久化时使用 `handlerCode`，而不是 `getClass().getSimpleName()`
- 在迁移期如果有必要，可以允许 legacy 代码暂时自动派生 `handlerCode`

预期结果：

- 所有新记录都使用稳定 handler 标识
- replay 不再依赖 Java 类名重构结果

### 5.2 Step B：引入 replay strategy 抽象

原因：

当前 replay 逻辑仍然知道具体业务域。

落地方式：

- 增加 `EventReplayStrategy`
- 增加 `EventReplayStrategyRegistry`
- 将 `bizCode -> payload 解析 + event type 解析` 的逻辑下沉到业务 adapter

预期结果：

- framework replay coordinator 变成业务无关

### 5.3 Step C：引入事件类型和 handler chain 注册中心

原因：

框架不应该通过硬编码分支发现业务事件类型。

落地方式：

- 增加 `EventTypeRegistry`
- 增加 `EventHandlerChainRegistry`
- 保留现有策略式注册思路，但命名和语义对齐到 framework 视角

预期结果：

- 所有 handler chain 和 event type 都通过 registry 解析

### 5.4 Step D：强化持久化契约

原因：

框架持久化必须能在 replay 并发场景下保持安全。

落地方式：

- 增加 `version` 字段，或 compare-and-set 更新契约
- 状态更新时显式带上并发保护条件
- 引入 `eventKey + handlerCode` 唯一约束

预期结果：

- 状态流转更安全，也更容易推理与审计

### 5.5 Step E：引入 payload 版本化

原因：

历史 replay 必须考虑 schema 兼容问题。

落地方式：

- 增加 `payloadVersion`
- 让 serializer / deserializer 感知版本
- 明确 handler 兼容策略文档

预期结果：

- payload 模型演进变得可控

### 5.6 Step F：引入死信与可观测性支持

原因：

一个可复用框架必须具备运维能力，而不只是执行能力。

落地方式：

- 增加 dead-letter 状态或死信表
- 增加 retry 延迟、失败分布等 metrics
- 增加 repeated failure 和 replay backlog 的 alarm hook

预期结果：

- 服务方可以更低成本地在生产环境中运维这套框架

## 6. 业务服务接入契约

业务服务应当实现或注册以下内容：

- 事件类型定义
- payload 模型与 serializer
- handler 实现
- handler chain provider
- replay strategy
- 在需要时覆盖 retry policy

业务服务不应修改以下内容：

- framework replay coordinator
- framework event listener
- framework event store 接口
- framework scheduler 核心逻辑
- framework execution chain 核心逻辑

## 7. 从当前实现到目标框架的代码迁移图

### 7.1 建议保留为通用核心能力的部分

- 当前事件监听模型
- 当前事务提交后发布模式
- 当前延迟 handler 模型
- 当前 grouped handler 编排模式
- 当前按 handler 粒度持久化状态的模式

### 7.2 建议在抽框架前先重构的部分

- 当前包含业务分支的 replay service
- 当前缺少 CAS 保护的状态更新 mapper
- 当前基于 simple class name 的 handler 身份
- 当前可能拆开 replay 分组的查询逻辑

### 7.3 建议保留为业务 adapter 的部分

- presale 事件枚举
- presale payload 解析
- presale handler chain
- presale 专属 retry 或告警策略

## 8. 推荐实施顺序

### Iteration 1

先在当前代码中做最小但必要的安全重构。

交付物：

- `EventReplayStrategy`
- `EventReplayStrategyRegistry`
- 稳定的 `handlerCode`
- 更安全的 `updateEventHandleStatus`
- 修复后的 replay 分组查询逻辑

### Iteration 2

创建 framework 包骨架，并迁移通用契约。

交付物：

- framework 包骨架
- 重命名后的核心抽象
- 基于新 framework 契约的 presale adapter 实现

### Iteration 3

补齐运维能力。

交付物：

- metrics
- alarms
- dead-letter support
- 更完善的 replay trace logging

### Iteration 4

在第二个接入方中验证。

交付物：

- 第二个业务集成示例
- framework API 清理
- 可抽库准备清单

## 9. 测试策略

### 9.1 单元测试

覆盖：

- 状态流转
- 延迟执行逻辑
- grouped handler 执行语义
- payload 序列化与版本兼容
- registry 解析行为

### 9.2 集成测试

覆盖：

- 业务状态与事件记录同事务保存
- 事务提交后分发
- handler 失败与 replay 重试
- `PROCESSING` 状态在“机器重启等价场景”下的恢复
- dead-letter 阈值行为

### 9.3 兼容性测试

覆盖：

- 新代码 replay 旧版本 payload
- 在 `handlerCode` 稳定的前提下，handler 重命名不影响持久化记录回放
- 第二个业务 adapter 接入时无需修改核心代码

## 10. 落地风险

- 抽象过早，可能暴露出不成熟的接口设计
- 核心类型一次性重命名过多，可能增加迁移成本
- 扩展点设计过多，可能提高接入复杂度
- 缺少可观测性时，replay backlog 容易在生产中长期潜伏

## 11. 阶段决策检查点

每进入下一阶段前，建议确认以下问题：

- framework core 是否还 import 任何业务域类
- 新增一个业务 event type 是否还需要修改 core replay 代码
- 状态流转是否具备并发安全性
- handler 身份是否在重构后仍保持稳定
- payload 兼容策略是否有文档且可测试

## 12. 推荐的下一批实现内容

下一批代码改造建议聚焦在“最小范围内，为抽框架创造条件”的动作上。

推荐批次：

1. 增加 `EventReplayStrategy` 和 registry
2. 改造 replay service，切换为 strategy 解析
3. 引入稳定 `handlerCode`
4. 让 record 更新具备并发安全性
5. 修复 replay 分页分组 bug

这一批改造的价值在于：在控制范围的前提下，为后续真正抽成框架打下基础。