package com.hibuka.soda.event.spring;

import com.hibuka.soda.cqrs.event.EventBus;
import com.hibuka.soda.cqrs.event.EventHandler;
import com.hibuka.soda.domain.event.DomainEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Auto-configuration for SpringEventBus.
 * Activates when soda.event.bus-type=spring is set.
 *
 * @author kangzeng.ckz
 * @since 2024/12/09
 */
@Configuration
@ConditionalOnProperty(name = "soda.event.bus-type", havingValue = "spring")
public class SpringEventBusAutoConfiguration {
    
    @Bean
    @Primary
    public EventBus springEventBus(ApplicationEventPublisher applicationEventPublisher,
                                  List<EventHandler<? extends DomainEvent>> eventHandlers) {
        return new SpringEventBus(applicationEventPublisher, eventHandlers);
    }
}