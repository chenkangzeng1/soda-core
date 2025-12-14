package com.hibuka.soda.bus.configuration;

import com.hibuka.soda.bus.impl.SimpleEventBus;
import com.hibuka.soda.cqrs.event.EventBus;
import com.hibuka.soda.cqrs.event.EventHandler;
import com.hibuka.soda.domain.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SimpleEventBus auto-configuration class.
 * This configuration is loaded when soda.event.bus-type=simple.
 *
 * @author kangzeng.ckz
 * @since 2025/12/14
 */
@Configuration
@ConditionalOnProperty(name = "soda.event.bus-type", havingValue = "simple", matchIfMissing = true)
public class SimpleEventBusAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SimpleEventBusAutoConfiguration.class);

    public SimpleEventBusAutoConfiguration() {
        logger.info("[SimpleEventBusAutoConfiguration] Constructor called");
    }

    /**
     * Creates a SimpleEventBus instance.
     * Default event bus implementation with no external dependencies.
     *
     * @param eventHandlers the event handlers
     * @return the simple event bus
     */
    @Bean
    @ConditionalOnMissingBean
    public EventBus simpleEventBus(List<EventHandler<? extends DomainEvent>> eventHandlers) {
        logger.info("[SimpleEventBusAutoConfiguration] Creating SimpleEventBus instance with {} event handlers", eventHandlers.size());
        SimpleEventBus eventBus = new SimpleEventBus(eventHandlers);
        logger.info("[SimpleEventBusAutoConfiguration] SimpleEventBus instance created: {}", eventBus);
        return eventBus;
    }
}