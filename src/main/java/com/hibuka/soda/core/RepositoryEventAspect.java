package com.hibuka.soda.core;

import com.hibuka.soda.core.context.CommandContext;
import com.hibuka.soda.core.context.CommandContextHolder;
import com.hibuka.soda.cqrs.handle.EventBus;
import com.hibuka.soda.domain.AbstractAggregateRoot;
import com.hibuka.soda.domain.AbstractDomainEvent;
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
        "execution(* com.hibuka.soda.domain.repository.WriteRepository+.save(..)) || " +
        "execution(* com.hibuka.soda.domain.repository.WriteRepository+.update(..)) || " +
        "execution(* com.hibuka.soda.domain.repository.WriteRepository+.delete(..)) || " +
        "execution(* com.hibuka.soda.domain.repository.WriteRepository+.operate(..))"
    )
    public Object aroundSave(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        Object[] args = joinPoint.getArgs();
        
        if (args != null && args.length > 0 && args[0] instanceof AbstractAggregateRoot) {
            AbstractAggregateRoot agg = (AbstractAggregateRoot) args[0];
            List<AbstractDomainEvent> events = List.copyOf(agg.getDomainEvents());
            
            if (!events.isEmpty()) {
                agg.clearDomainEvents();
                
                // 填充事件上下文信息
                enrichEventsWithContext(events);
                
                // 注册事务同步回调,在事务提交后发布事件
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                publishEvents(events);
                            }
                        }
                    );
                } else {
                    // 如果没有事务,立即发布(测试环境或非事务场景)
                    publishEvents(events);
                }
            }
        }
        return result;
    }
    
    private void enrichEventsWithContext(List<AbstractDomainEvent> events) {
        CommandContext ctx = CommandContextHolder.get();
        if (ctx != null) {
            events.forEach(e -> {
                e.setRequestId(ctx.getRequestId());
                e.setJti(ctx.getJti());
                e.setAuthorities(ctx.getAuthorities());
                e.setUserName(ctx.getUserName());
                e.setCallerUid(ctx.getCallerUid());
            });
        }
    }
    
    private void publishEvents(List<AbstractDomainEvent> events) {
        for (AbstractDomainEvent event : events) {
            try {
                eventBus.publish(event);
            } catch (Exception e) {
                // 记录日志,便于排查问题
                log.error("Failed to publish domain event: {}", event, e);
            }
        }
    }
}
