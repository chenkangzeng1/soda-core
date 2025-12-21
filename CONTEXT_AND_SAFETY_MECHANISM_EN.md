# Soda Core Context Propagation and Safety Mechanism

## 1. Context Propagation Mechanism

Soda Core framework implements full-link context automatic propagation, ensuring that key metadata such as `requestId` and `userInfo` are not lost in the **Command -> Event -> Command** call chain.

### 1.1 Propagation Path

1.  **Command -> Event**
    *   **Implementation Principle**: In `AbstractAggregateRoot`, when an aggregate root generates a Domain Event, `RepositoryEventAspect` intercepts the `save` operation.
    *   **Mechanism**: `RepositoryEventAspect` reads context information (requestId, jti, hopCount, etc.) from the current `DomainEventContext` (ThreadLocal) and automatically injects it into the `AbstractDomainEvent` to be published.

2.  **Event -> Command**
    *   **Implementation Principle**: `CqrsAroundHandler` intercepts the execution of all `EventHandler`.
    *   **Mechanism**:
        1.  **Context Restoration**: Before `EventHandler` execution starts, the interceptor extracts context information from the `Event` object and restores it to `DomainEventContext` (ThreadLocal).
        2.  **Automatic Injection**: When the EventHandler internally sends a new `Command` through `BusFacade`, `CqrsAroundHandler` intercepts the `CommandBus.send` method. If the Command's `requestId` is empty, it automatically retrieves the value from `DomainEventContext` and fills it in.

### 1.2 Core Components
*   `DomainEventContext`: ThreadLocal-based context holder, stores metadata for the current request.
*   `CqrsAroundHandler`: AOP aspect responsible for context backup and restoration before and after Handler execution, as well as automatic injection when Command is sent.
*   `RepositoryEventAspect`: Spring Data Repository aspect responsible for injecting context into generated Domain Events when aggregate roots are saved.

---

## 2. Async Recursion Protection Mechanism

To prevent infinite loops caused by business logic errors (e.g., Event A -> Command A -> Event A...), the framework introduces a **hopCount (jump count)** mechanism.

### 2.1 Mechanism Principle
*   **Field Definition**: Added `hopCount` (Integer) field to `BaseCommand` and `AbstractDomainEvent` to record the depth of the current message in the call chain.
*   **Propagation Logic**:
    *   **Command (hop=N) -> Event**: `RepositoryEventAspect` passes `hopCount` from Command context to Event.
    *   **Event (hop=N) -> Command**: When `CqrsAroundHandler` sends a new Command, it sets `hopCount` to `Event.hopCount + 1`.

### 2.2 Circuit Breaking Strategy
*   **Threshold Check**: When `CqrsAroundHandler` intercepts `CommandBus.send`, it checks the `hopCount` of the current context.
*   **Automatic Circuit Breaking**: If `hopCount > 20` (hard-coded threshold), the framework throws `IllegalStateException("Async recursion too deep!")` to forcefully interrupt the call chain, preventing message queue explosion or system avalanche.

### 2.3 Application Scenarios
*   **Synchronous Calls**: `BusFacade` has built-in `recursionDepth` check (based on ThreadLocal, threshold 10).
*   **Asynchronous Calls**: The `hopCount` mechanism specifically addresses dead loop problems across threads and services (such as Redis Stream, MQ), because `hopCount` is passed along with the message payload and is not affected by ThreadLocal reset.