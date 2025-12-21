package com.hibuka.soda.domain.aggregate;

import com.hibuka.soda.context.DomainEventContext;
import com.hibuka.soda.domain.event.AbstractDomainEvent;
import com.hibuka.soda.domain.event.DomainEvents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * AbstractAggregateRoot description
 *
 * @author kangzeng.ckz
 * @since 2024/10/29
 **/
public abstract class AbstractAggregateRoot extends AbstractDomainEvent implements DomainEvents {
    private final List<AbstractDomainEvent> domainEvents = new ArrayList<>();

    /**
     * Register domain event
     *
     * @param event the domain event to register
     */
    protected void addPendingEvent(AbstractDomainEvent event) {
        if (event != null) {
            injectContext(event);
            domainEvents.add(event);
        }
    }

    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    private void injectContext(AbstractDomainEvent event) {
        // Auto-fill context from DomainEventContext
        String requestId = DomainEventContext.getRequestId();
        String userName = DomainEventContext.getUserName();
        String jti = DomainEventContext.getJti();
        String authorities = DomainEventContext.getAuthorities();
        String callerUid = DomainEventContext.getCallerUid();
        
        if (event.getRequestId() == null) {
            event.setRequestId(requestId);
        }
        if (event.getUserName() == null) {
            event.setUserName(userName);
        }
        if (event.getJti() == null) {
            event.setJti(jti);
        }
        if (event.getAuthorities() == null) {
            event.setAuthorities(authorities);
        }
        if (event.getCallerUid() == null && callerUid != null) {
            event.setCallerUid(callerUid);
        }
    }

    @Override
    public Collection<AbstractDomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    @Override
    public void clearDomainEvents() {
        domainEvents.clear();
    }
} 