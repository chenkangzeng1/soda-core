# Soda Core 上下文传递与安全机制

## 1. 上下文传递机制 (Context Propagation)

Soda Core 框架实现了全链路的上下文（Context）自动传递机制，确保 `requestId`、`userInfo` 等关键元数据在 **Command -> Event -> Command** 的调用链中不丢失。

### 1.1 传递路径

1.  **Command -> Event**
    *   **实现原理**：在 `AbstractAggregateRoot` 中，当聚合根产生 Domain Event 时，`RepositoryEventAspect` 会拦截 `save` 操作。
    *   **机制**：`RepositoryEventAspect` 从当前的 `DomainEventContext`（ThreadLocal）中读取上下文信息（requestId, jti, hopCount 等），并自动注入到即将发布的 `AbstractDomainEvent` 中。

2.  **Event -> Command**
    *   **实现原理**：`CqrsAroundHandler` 拦截所有 `EventHandler` 的执行。
    *   **机制**：
        1.  **上下文恢复**：在 `EventHandler` 开始执行前，拦截器从 `Event` 对象中提取上下文信息，并恢复到 `DomainEventContext`（ThreadLocal）中。
        2.  **自动注入**：当 EventHandler 内部通过 `BusFacade` 发送新的 `Command` 时，`CqrsAroundHandler` 拦截 `CommandBus.send` 方法，发现 Command 的 `requestId` 为空，则自动从 `DomainEventContext` 中取值并填入。

### 1.2 核心组件
*   `DomainEventContext`: 基于 ThreadLocal 的上下文持有者，存储当前请求的元数据。
*   `CqrsAroundHandler`: AOP 切面，负责在 Handler 执行前后进行上下文的备份与恢复，以及 Command 发送时的自动注入。
*   `RepositoryEventAspect`: Spring Data Repository 切面，负责在聚合根保存时，将上下文注入到产生的 Domain Event 中。

---

## 2. 异步递归保护机制 (Async Recursion Protection)

为了防止业务逻辑错误导致无限循环（例如：Event A -> Command A -> Event A...），框架引入了 **hopCount（跳跃计数）** 机制。

### 2.1 机制原理
*   **字段定义**：在 `BaseCommand` 和 `AbstractDomainEvent` 中新增了 `hopCount` (Integer) 字段，用于记录当前消息在调用链中的深度。
*   **传递逻辑**：
    *   **Command (hop=N) -> Event**: `RepositoryEventAspect` 将 `hopCount` 从 Command 上下文传递给 Event。
    *   **Event (hop=N) -> Command**: `CqrsAroundHandler` 在发送新 Command 时，将 `hopCount` 设为 `Event.hopCount + 1`。

### 2.2 熔断策略
*   **阈值检查**：在 `CqrsAroundHandler` 拦截 `CommandBus.send` 时，会检查当前上下文的 `hopCount`。
*   **自动熔断**：如果 `hopCount > 20`（硬编码阈值），框架将抛出 `IllegalStateException("Async recursion too deep!")`，强制中断调用链，防止消息队列爆炸或系统雪崩。

### 2.3 适用场景
*   **同步调用**：`BusFacade` 已内置 `recursionDepth` 检查（基于 ThreadLocal，阈值 10）。
*   **异步调用**：`hopCount` 机制专门解决跨线程、跨服务（如 Redis Stream, MQ）的死循环问题，因为 `hopCount` 是随消息 payload 传递的，不受 ThreadLocal 重置的影响。
