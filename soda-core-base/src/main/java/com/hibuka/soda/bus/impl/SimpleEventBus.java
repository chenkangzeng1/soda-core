package com.hibuka.soda.bus.impl;

import com.hibuka.soda.cqrs.event.EventHandler;
import com.hibuka.soda.domain.event.DomainEvent;
import com.hibuka.soda.foundation.error.BaseException;
import com.hibuka.soda.cqrs.event.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.GenericTypeResolver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple event bus implementation, providing basic event publishing and subscription functionality.
 *
 * @author kangzeng.ckz
 * @since 2024/12/09
 */
public class SimpleEventBus implements EventBus {
    private static final Logger logger = LoggerFactory.getLogger(SimpleEventBus.class);
    private final Map<Class<? extends DomainEvent>, List<EventHandler>> handlers = new ConcurrentHashMap<>();
    
    /**
     * Constructor for SimpleEventBus.
     *
     * @param eventHandlers List of event handlers to register
     */
    public SimpleEventBus(List<EventHandler<? extends DomainEvent>> eventHandlers) {
        logger.info("[SimpleEventBus] Constructor called, handlers size: {}", eventHandlers.size());
        registerEventHandlers(eventHandlers);
    }
    
    /**
     * Registers all event handlers.
     *
     * @param eventHandlers List of event handlers to register
     */
    private void registerEventHandlers(List<EventHandler<? extends DomainEvent>> eventHandlers) {
        for (EventHandler<? extends DomainEvent> handler : eventHandlers) {
            Class<?>[] typeArguments = GenericTypeResolver.resolveTypeArguments(
                    handler.getClass(),
                    EventHandler.class
            );
            if (typeArguments != null && typeArguments.length > 0) {
                Class<? extends DomainEvent> eventType = (Class<? extends DomainEvent>) typeArguments[0];
                subscribe(eventType, handler);
            }
        }
        logger.info("[SimpleEventBus] Registered {} event handler types", handlers.size());
    }
    
    @Override
    public void publish(DomainEvent event) throws BaseException {
        // Get all handlers for the event type and all its supertypes
        Class<?> eventType = event.getClass();
        while (eventType != null) {
            // Get handlers for the current type
            List<EventHandler> eventHandlers = handlers.get(eventType);
            if (eventHandlers != null && !eventHandlers.isEmpty()) {
                for (EventHandler handler : eventHandlers) {
                    handler.handle(event);
                }
            }
            
            // Check all interfaces implemented by this class
            for (Class<?> interfaceType : eventType.getInterfaces()) {
                if (DomainEvent.class.isAssignableFrom(interfaceType)) {
                    List<EventHandler> interfaceHandlers = handlers.get(interfaceType);
                    if (interfaceHandlers != null && !interfaceHandlers.isEmpty()) {
                        for (EventHandler handler : interfaceHandlers) {
                            handler.handle(event);
                        }
                    }
                }
            }
            
            // Move to the superclass
            eventType = eventType.getSuperclass();
        }
    }
    
    @Override
    public void subscribe(Class<? extends DomainEvent> eventType, EventHandler handler) {
        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
        logger.info("[SimpleEventBus] Subscribed handler {} for event {}", handler.getClass().getName(), eventType.getName());
    }
    
    @Override
    public void unsubscribe(Class<? extends DomainEvent> eventType, EventHandler handler) {
        List<EventHandler> eventHandlers = handlers.get(eventType);
        if (eventHandlers != null) {
            eventHandlers.remove(handler);
            logger.info("[SimpleEventBus] Unsubscribed handler {} for event {}", handler.getClass().getName(), eventType.getName());
            if (eventHandlers.isEmpty()) {
                handlers.remove(eventType);
                logger.info("[SimpleEventBus] Removed event type {} from handlers", eventType.getName());
            }
        }
    }
}