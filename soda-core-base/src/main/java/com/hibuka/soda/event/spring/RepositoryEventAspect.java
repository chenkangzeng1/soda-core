package com.hibuka.soda.event.spring;

import com.hibuka.soda.context.CommandContext;
import com.hibuka.soda.context.CommandContextHolder;
import com.hibuka.soda.cqrs.event.EventBus;
import com.hibuka.soda.domain.aggregate.AbstractAggregateRoot;
import com.hibuka.soda.domain.event.AbstractDomainEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Aspect
public class RepositoryEventAspect {
    private static final Logger log = LoggerFactory.getLogger(RepositoryEventAspect.class);
    private final EventBus eventBus;
    private final ObjectProvider<CommandContext> commandContextProvider;

    public RepositoryEventAspect(EventBus eventBus, ObjectProvider<CommandContext> commandContextProvider) {
        this.eventBus = eventBus;
        this.commandContextProvider = commandContextProvider;
        CommandContextHolder.setProvider(commandContextProvider);
    }

    @Around(
        "execution(* *..*Repository+.save(..)) || " +
        "execution(* *..*Repository+.update(..)) || " +
        "execution(* *..*Repository+.delete(..)) || " +
        "execution(* *..*Repository+.operate(..))"
    )
    public Object aroundSave(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("[RepositoryEventAspect] Intercepted method: {}", joinPoint.getSignature().getName());
        Object result = joinPoint.proceed();
        Object[] args = joinPoint.getArgs();
        
        if (args != null && args.length > 0 && args[0] instanceof AbstractAggregateRoot) {
            AbstractAggregateRoot agg = (AbstractAggregateRoot) args[0];
            List<AbstractDomainEvent> events = List.copyOf(agg.getDomainEvents());
            
            if (!events.isEmpty()) {
                agg.clearDomainEvents();
                
                enrichEventsWithContext(events);
                
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    log.info("Transaction synchronization is active, registering afterCommit callback for {} events", events.size());
                    TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                log.info("Transaction committed, publishing {} events", events.size());
                                publishEvents(events);
                            }
                        }
                    );
                } else {
                    log.info("No transaction synchronization active, publishing {} events immediately", events.size());
                    publishEvents(events);
                }
            }
        }
        return result;
    }
    
    private void enrichEventsWithContext(List<AbstractDomainEvent> events) {
        CommandContext ctx = CommandContextHolder.getContext();
        if (ctx != null) {
            events.forEach(e -> {
                e.setRequestId(ctx.getRequestId());
                e.setJti(ctx.getJti());
                String[] authoritiesArray = ctx.getAuthorities();
                if (authoritiesArray != null) {
                    e.setAuthorities(String.join(",", authoritiesArray));
                }
                e.setUserName(ctx.getUserName());
                String callerUidStr = ctx.getCallerUid();
                if (callerUidStr != null && !callerUidStr.isEmpty()) {
                    try {
                        e.setCallerUid(Long.parseLong(callerUidStr));
                    } catch (NumberFormatException ex) {
                        log.error("Failed to parse callerUid: {}", callerUidStr, ex);
                    }
                }
            });
        }
    }
    
    private void publishEvents(List<AbstractDomainEvent> events) {
        log.info("Publishing {} domain events via {}, thread={}", 
                events.size(), eventBus.getClass().getName(), Thread.currentThread().getName());
        for (AbstractDomainEvent event : events) {
            try {
                log.info("Publishing event: eventId={}, eventType={}", event.getEventId(), event.getEventType());
                eventBus.publish(event);
            } catch (Exception e) {
                log.error("Failed to publish domain event: {}", event, e);
            }
        }
    }
}
