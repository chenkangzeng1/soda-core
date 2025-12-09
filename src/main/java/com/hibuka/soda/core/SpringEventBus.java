package com.hibuka.soda.core;

import com.hibuka.soda.base.error.BaseException;
import com.hibuka.soda.cqrs.handle.EventBus;
import com.hibuka.soda.cqrs.handle.EventHandler;
import com.hibuka.soda.domain.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.GenericTypeResolver;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Event bus implementation based on Spring ApplicationEvent.
 * This implementation supports multi-instance event propagation through Spring's event mechanism.
 *
 * @author kangzeng.ckz
 * @since 2024/12/09
 */
@Component
public class SpringEventBus implements EventBus {
    private static final Logger logger = LoggerFactory.getLogger(SpringEventBus.class);
    
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Map<Class<? extends DomainEvent>, List<EventHandler>> handlers = new ConcurrentHashMap<>();
    
    /**
     * Constructor for SpringEventBus.
     *
     * @param applicationEventPublisher Spring's application event publisher
     * @param eventHandlers List of event handlers to register
     */
    public SpringEventBus(ApplicationEventPublisher applicationEventPublisher,
                         List<EventHandler<? extends DomainEvent>> eventHandlers) {
        this.applicationEventPublisher = applicationEventPublisher;
        registerEventHandlers(eventHandlers);
        logger.info("[SpringEventBus] Initialized with {} event handlers", handlers.size());
    }
    
    /**
     * Registers all event handlers.
     *
     * @param eventHandlers List of event handlers to register
     */
    private void registerEventHandlers(List<EventHandler<? extends DomainEvent>> eventHandlers) {
        logger.info("[SpringEventBus] Registering {} event handlers", eventHandlers.size());
        for (EventHandler<? extends DomainEvent> handler : eventHandlers) {
            Class<?>[] typeArguments = GenericTypeResolver.resolveTypeArguments(
                    handler.getClass(),
                    EventHandler.class
            );
            if (typeArguments != null && typeArguments.length > 0) {
                Class<? extends DomainEvent> eventType = (Class<? extends DomainEvent>) typeArguments[0];
                subscribe(eventType, handler);
                logger.info("[SpringEventBus] Registered handler for event: {}", eventType.getName());
            }
        }
    }
    
    @Override
    public void publish(DomainEvent event) throws BaseException {
        logger.info("[SpringEventBus] Publishing event: {}", event.getClass().getName());
        applicationEventPublisher.publishEvent(event);
        
        // Also publish to local handlers for immediate processing
        List<EventHandler> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            for (EventHandler handler : eventHandlers) {
                handler.handle(event);
            }
        }
    }
    
    @Override
    public void subscribe(Class<? extends DomainEvent> eventType, EventHandler handler) {
        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
        logger.debug("[SpringEventBus] Subscribed handler for event: {}", eventType.getName());
    }
    
    @Override
    public void unsubscribe(Class<? extends DomainEvent> eventType, EventHandler handler) {
        List<EventHandler> eventHandlers = handlers.get(eventType);
        if (eventHandlers != null) {
            eventHandlers.remove(handler);
            logger.debug("[SpringEventBus] Unsubscribed handler for event: {}", eventType.getName());
        }
    }
}