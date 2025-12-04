package com.hibuka.soda.core;

import com.hibuka.soda.core.context.CommandContext;
import com.hibuka.soda.core.context.CommandContextHolder;
import com.hibuka.soda.core.context.DomainEventContext;
import com.hibuka.soda.cqrs.handle.EventBus;
import com.hibuka.soda.domain.AbstractAggregateRoot;
import com.hibuka.soda.domain.AbstractDomainEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.ObjectProvider;



import java.util.List;

@Aspect
public class RepositoryEventAspect {
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
            List<AbstractDomainEvent> events = agg.getDomainEvents() == null ? null : List.copyOf(agg.getDomainEvents());
            agg.clearDomainEvents();
            if (events != null && !events.isEmpty()) {
                DomainEventContext.addAll(events);
                {
                        CommandContext ctx = CommandContextHolder.get();
                        for (AbstractDomainEvent e : DomainEventContext.getEvents()) {
                            if (ctx != null) {
                                e.setRequestId(ctx.getRequestId());
                                e.setJti(ctx.getJti());
                                e.setAuthorities(ctx.getAuthorities());
                                e.setUserName(ctx.getUserName());
                                e.setCallerUid(ctx.getCallerUid());
                            }
                            try {
                                eventBus.publish(e);
                            } catch (Exception ignored) {
                            }
                        }
                        DomainEventContext.clear();
                }
            }
        }
        return result;
    }
}
