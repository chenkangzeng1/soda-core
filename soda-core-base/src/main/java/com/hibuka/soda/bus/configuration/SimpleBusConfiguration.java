package com.hibuka.soda.bus.configuration;

import com.hibuka.soda.cqrs.command.CommandBus;
import com.hibuka.soda.cqrs.command.CommandHandler;
import com.hibuka.soda.cqrs.event.EventBus;
import com.hibuka.soda.cqrs.event.EventHandler;
import com.hibuka.soda.cqrs.query.QueryBus;
import com.hibuka.soda.cqrs.query.QueryHandler;
import com.hibuka.soda.domain.event.DomainEvent;
import com.hibuka.soda.bus.impl.SimpleEventBus;
import com.hibuka.soda.bus.impl.SimpleCommandBus;
import com.hibuka.soda.bus.impl.SimpleQueryBus;

import java.util.List;

/**
 * Simple DDD/CQRS bus configuration class, registers core buses without Spring dependencies.
 *
 * @author kangzeng.ckz
 * @since 2025/12/13
 */
public class SimpleBusConfiguration {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SimpleBusConfiguration.class);

    /**
     * Creates a simple event bus.
     * Default event bus implementation with no external dependencies.
     *
     * @param eventHandlers the event handlers
     * @return the simple event bus
     */
    public EventBus simpleEventBus(List<EventHandler<? extends DomainEvent>> eventHandlers) {
        logger.info("[SimpleBusConfiguration] Creating SimpleEventBus");
        return new SimpleEventBus(eventHandlers);
    }

    /**
     * Creates a simple command bus.
     *
     * @param commandHandlers the command handlers
     * @return the simple command bus
     */
    public CommandBus simpleCommandBus(List<CommandHandler<?, ?>> commandHandlers, EventProperties eventProperties) {
        return new SimpleCommandBus(commandHandlers, eventProperties);
    }

    /**
     * Creates a simple query bus.
     *
     * @param queryHandlers the query handlers
     * @return the simple query bus
     */
    public QueryBus simpleQueryBus(List<QueryHandler<?, ?>> queryHandlers) {
        return new SimpleQueryBus(queryHandlers);
    }
}