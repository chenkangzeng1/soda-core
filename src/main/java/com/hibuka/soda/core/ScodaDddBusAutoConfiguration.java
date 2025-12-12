package com.hibuka.soda.core;

import com.hibuka.soda.cqrs.handle.CommandBus;
import com.hibuka.soda.cqrs.handle.CommandHandler;
import com.hibuka.soda.cqrs.handle.EventBus;
import com.hibuka.soda.cqrs.handle.EventHandler;
import com.hibuka.soda.cqrs.handle.QueryBus;
import com.hibuka.soda.cqrs.handle.QueryHandler;
import com.hibuka.soda.domain.DomainEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import com.hibuka.soda.core.context.CommandContext;
import org.springframework.beans.factory.ObjectProvider;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import java.util.List;

/**
 * DDD/CQRS bus auto-configuration class, registers core buses, facades, aspects, thread pools and other Beans, supports business custom overrides.
 *
 * @author kangzeng.ckz
 * @since 2025/7/3
 **/
@Configuration
@EnableConfigurationProperties({AsyncConfig.class, EventProperties.class})
public class ScodaDddBusAutoConfiguration {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScodaDddBusAutoConfiguration.class);
    
    @Autowired
    public ScodaDddBusAutoConfiguration(EventProperties eventProperties) {
        logger.info("[ScodaDddBusAutoConfiguration] Constructor called");
        logger.info("[ScodaDddBusAutoConfiguration] Bus type: {}", eventProperties.getBusType());
    }
    /**
     * Creates a CQRS around handler.
     * @param eventBus the event bus
     * @return the CQRS around handler
     */
    @Bean
    public CqrsAroundHandler cqrsAroundHandler(EventBus eventBus) {
        return new CqrsAroundHandler(eventBus);
    }

    /**
     * Creates a simple event bus.
     * @param eventHandlers the event handlers
     * @return the simple event bus
     */
    @Bean
    @ConditionalOnProperty(name = "soda.event.bus-type", havingValue = "simple")
    public EventBus simpleEventBus(List<EventHandler<? extends DomainEvent>> eventHandlers) {
        return new SimpleEventBus(eventHandlers);
    }

    /**
     * Creates a Spring event bus.
     * Default event bus implementation when no other bus type is specified.
     *
     * @param applicationEventPublisher Spring's application event publisher
     * @param eventHandlers the event handlers
     * @return the Spring event bus
     */
    @Bean
    @ConditionalOnProperty(name = "soda.event.bus-type", havingValue = "spring", matchIfMissing = true)
    public EventBus springEventBus(org.springframework.context.ApplicationEventPublisher applicationEventPublisher,
                                 List<EventHandler<? extends DomainEvent>> eventHandlers) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScodaDddBusAutoConfiguration.class);
        logger.info("[ScodaDddBusAutoConfiguration] Creating SpringEventBus");
        return new SpringEventBus(applicationEventPublisher, eventHandlers);
    }

    /**
     * Creates a simple command bus.
     * @param commandHandlers the command handlers
     * @return the simple command bus
     */
    @Bean
    public CommandBus simpleCommandBus(List<CommandHandler<?, ?>> commandHandlers) {
        return new SimpleCommandBus(commandHandlers);
    }

    /**
     * Creates a simple query bus.
     * @param queryHandlers the query handlers
     * @return the simple query bus
     */
    @Bean
    public QueryBus simpleQueryBus(List<QueryHandler<?, ?>> queryHandlers) {
        return new SimpleQueryBus(queryHandlers);
    }

    /**
     * Creates a CQRS async executor.
     * @param asyncConfig the async configuration
     * @return the CQRS async executor
     */
    @Bean("cqrsAsyncExecutor")
    @ConditionalOnMissingBean(name = "cqrsAsyncExecutor")
    @Primary
    public Executor cqrsAsyncExecutor(AsyncConfig asyncConfig) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncConfig.getCorePoolSize());
        executor.setMaxPoolSize(asyncConfig.getMaxPoolSize());
        executor.setQueueCapacity(asyncConfig.getQueueCapacity());
        executor.setThreadNamePrefix(asyncConfig.getThreadNamePrefix());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean
    public CommandContext commandContext() {
        return new CommandContext();
    }

    @Bean
    public RepositoryEventAspect repositoryEventAspect(EventBus eventBus, ObjectProvider<CommandContext> commandContextProvider) {
        return new RepositoryEventAspect(eventBus, commandContextProvider);
    }

    /**
     * Creates a bus facade.
     * @param commandBus the command bus
     * @param queryBus the query bus
     * @param cqrsAsyncExecutor the CQRS async executor
     * @return the bus facade
     */
    @Bean
    @ConditionalOnMissingBean
    public BusFacade busFacade(CommandBus commandBus, QueryBus queryBus,@Qualifier("cqrsAsyncExecutor") Executor cqrsAsyncExecutor) {
        return new BusFacade(commandBus, queryBus, cqrsAsyncExecutor);
    }
} 