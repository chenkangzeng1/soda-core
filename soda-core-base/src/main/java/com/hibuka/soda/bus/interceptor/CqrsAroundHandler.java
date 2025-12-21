package com.hibuka.soda.bus.interceptor;

import com.alibaba.fastjson.JSON;
import com.hibuka.soda.bus.configuration.EventProperties;
import com.hibuka.soda.cqrs.command.BaseCommand;
import com.hibuka.soda.cqrs.event.EventBus;
import com.hibuka.soda.cqrs.query.BaseQuery;
import com.hibuka.soda.context.DomainEventContext;
import com.hibuka.soda.domain.event.AbstractDomainEvent;
import com.hibuka.soda.domain.event.DomainEvent;
import com.hibuka.soda.domain.event.DomainEvents;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CQRS aspect, uniformly intercepts command, query, and event handlers, records link logs and automatically publishes domain events.
 *
 * @author kangzeng.ckz
 * @since 2025/7/3
 **/
@Aspect
public class CqrsAroundHandler {
    private final EventBus eventBus;
    private final EventProperties eventProperties;
    private Logger logger = LoggerFactory.getLogger(CqrsAroundHandler.class);

    /**
     * Constructor for CqrsAroundHandler.
     * @param eventBus the event bus
     */
    public CqrsAroundHandler(EventBus eventBus, EventProperties eventProperties) {
        this.eventBus = eventBus;
        this.eventProperties = eventProperties;
        logger.info("[CqrsAroundHandler] Bean created!");
        logger.info("[CqrsAroundHandler] Injected EventBus: {}", eventBus.getClass().getName());
    }

    /**
     * Around advice for command, query, and event handlers.
     * @param point the join point
     * @return the result of method execution
     * @throws Throwable if method execution fails
     */
    @Around("execution(* com.hibuka.soda.cqrs.command.CommandHandler+.handle(..)) || " +
            "execution(* com.hibuka.soda.cqrs.query.QueryHandler+.handle(..)) || " +
            "execution(* com.hibuka.soda.cqrs.event.EventHandler+.handle(..))")
    public Object handleExecutor(ProceedingJoinPoint point) throws Throwable {
        logger.info("[CqrsAroundHandler] handleExecutor intercepted: {}", point.getSignature());
        long timestamp = System.currentTimeMillis();
        Object[] args = point.getArgs();
        Object firstArg = (args != null && args.length > 0) ? args[0] : null;
        String username = null;
        String requestId = "";

        boolean streamEnabled = isStreamEnabled();
        boolean isEventHandler = firstArg instanceof AbstractDomainEvent;
        boolean isStreamConsumer = false;
        boolean isAsyncStreamThread = Thread.currentThread().getName().startsWith("SimpleAsyncTaskExecutor");
        if (isEventHandler && streamEnabled && !isStreamConsumer && !isAsyncStreamThread) {
            logger.info("[CqrsAroundHandler] Stream enabled, skip EventHandler on thread: {}", Thread.currentThread().getName());
            return null;
        }

        if (firstArg instanceof BaseCommand) {
            BaseCommand cmd = (BaseCommand) firstArg;
            requestId = cmd.getRequestId();
            username = cmd.getUserName();
            
            // Set DomainEventContext from Command
            DomainEventContext.setRequestId(requestId);
            DomainEventContext.setUserName(username);
            DomainEventContext.setJti(cmd.getJti());
            DomainEventContext.setAuthorities(cmd.getAuthorities());
            if (cmd.getCallerUid() != null) {
                DomainEventContext.setCallerUid(String.valueOf(cmd.getCallerUid()));
            }
            if (cmd.getHopCount() != null) {
                DomainEventContext.setHopCount(cmd.getHopCount());
            }
        } else if (firstArg instanceof BaseQuery) {
            requestId = ((BaseQuery) firstArg).getRequestId();
            username = ((BaseQuery) firstArg).getUserName();
        } else if (firstArg instanceof AbstractDomainEvent) {
            AbstractDomainEvent event = (AbstractDomainEvent) firstArg;
            requestId = event.getRequestId();
            username = event.getUserName();
            
            // Restore DomainEventContext from Event (for downstream Commands)
            DomainEventContext.setRequestId(requestId);
            DomainEventContext.setUserName(username);
            DomainEventContext.setJti(event.getJti());
            DomainEventContext.setAuthorities(event.getAuthorities());
            if (event.getCallerUid() != null) {
                DomainEventContext.setCallerUid(event.getCallerUid());
            }
            if (event.getHopCount() != null) {
                DomainEventContext.setHopCount(event.getHopCount());
            }
        }
        Object result;
        try {
            result = point.proceed();
        } catch (Throwable throwable) {
            throw throwable;
        } finally {
            // Clean up context to prevent memory leaks
            if (firstArg instanceof BaseCommand || firstArg instanceof AbstractDomainEvent) {
                DomainEventContext.clear();
            }
        }
        long timestamp1 = System.currentTimeMillis();
        logger.info("requestId:{},method:{}-{}, {}:{}, result:{}, duration:{}-{},username:{}", requestId, point.getSignature().getDeclaringTypeName(),
                point.getSignature().getName(), firstArg.getClass().getSimpleName(), JSON.toJSONString(firstArg), JSON.toJSONString(result), (System.currentTimeMillis() - timestamp),
                (System.currentTimeMillis() - timestamp1), username);
        return result;
    }

    /**
     * Around advice for command bus send method.
     * @param joinPoint the join point
     * @return the result of method execution
     * @throws Throwable if method execution fails
     */
    @Around("execution(* com.hibuka.soda.cqrs.command.CommandBus+.send(..))")
    public Object handleEventBus(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.info("[CqrsAroundHandler] handleEventBus invoked! method: {}", joinPoint.getSignature());
        logger.info("[CqrsAroundHandler] Current thread: {}, EventBus: {}", Thread.currentThread().getName(), eventBus.getClass().getName());
        
        // Auto-fill context into Command if context exists in ThreadLocal
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof BaseCommand) {
            BaseCommand cmd = (BaseCommand) args[0];
            if (cmd.getRequestId() == null) {
                String requestId = DomainEventContext.getRequestId();
                if (requestId != null) {
                    cmd.setRequestId(requestId);
                    cmd.setUserName(DomainEventContext.getUserName());
                    cmd.setJti(DomainEventContext.getJti());
                    cmd.setAuthorities(DomainEventContext.getAuthorities());
                    String callerUid = DomainEventContext.getCallerUid();
                    if (callerUid != null) {
                        try {
                            cmd.setCallerUid(callerUid);
                        } catch (Exception ignored) {}
                    }
                }
            }
            
            // Async recursion protection
            Integer hopCount = DomainEventContext.getHopCount();
            int currentHop = (hopCount != null) ? hopCount : 0;
            if (currentHop > 20) {
                logger.error("[CqrsAroundHandler] Async recursion too deep! hopCount={}, command={}", currentHop, cmd.getClass().getSimpleName());
                throw new IllegalStateException("Async recursion too deep! hopCount=" + currentHop);
            }
            cmd.setHopCount(currentHop + 1);
        }

        // 执行原始命令
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            throw throwable;
        }
        // 获取命令对象
        if (args.length > 0 && args[0] instanceof BaseCommand) {
            // 如果命令执行后产生了领域事件，发布事件
            if (result instanceof DomainEvents) {
                boolean streamEnabled = isStreamEnabled();
                for (AbstractDomainEvent event : ((DomainEvents) result).getDomainEvents()) {
                    sycBaseInfo((BaseCommand) args[0], event);
                    if (streamEnabled) {
                        logger.info("[CqrsAroundHandler] Stream enabled, skip local immediate publish (DomainEvents path), will rely on repository aspect. eventId: {}, eventType: {}", event.getEventId(), event.getEventType());
                    } else {
                        logger.info("[CqrsAroundHandler] Publishing event via EventBus (DomainEvents path), eventId: {}, eventType: {}", event.getEventId(), event.getEventType());
                        eventBus.publish(event);
                    }
                }
            }
            // 如果结果本身就是一个DomainEvent，直接发布
            else if (result instanceof DomainEvent) {
                AbstractDomainEvent e = (AbstractDomainEvent) result;
                boolean streamEnabled = isStreamEnabled();
                if (streamEnabled) {
                    logger.info("[CqrsAroundHandler] Stream enabled, skip local immediate publish (DomainEvent path), will rely on repository aspect. eventId: {}, eventType: {}", e.getEventId(), e.getEventType());
                } else {
                    logger.info("[CqrsAroundHandler] Publishing event via EventBus (DomainEvent path), eventId: {}, eventType: {}", e.getEventId(), e.getEventType());
                    eventBus.publish((DomainEvent) result);
                }
            }
        }
        return result;
    }

    private void sycBaseInfo(BaseCommand command, AbstractDomainEvent event) {
        event.setRequestId(command.getRequestId());
        event.setJti(command.getJti());
        event.setAuthorities(command.getAuthorities());
        event.setUserName(command.getUserName());
        event.setCallerUid(command.getCallerUid());
        event.setHopCount(command.getHopCount());
    }

    private boolean isStreamEnabled() {
        try {
            return eventProperties != null
                    && "redis".equals(eventProperties.getBusType());
        } catch (Exception e) {
            logger.warn("[CqrsAroundHandler] Failed to read bus type from EventProperties, defaulting to false: {}", e.getMessage());
            return false;
        }
    }
}
