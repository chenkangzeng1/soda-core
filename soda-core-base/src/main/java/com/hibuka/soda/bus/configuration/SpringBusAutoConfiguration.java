package com.hibuka.soda.bus.configuration;

import com.hibuka.soda.bus.facade.BusFacade;
import com.hibuka.soda.cqrs.command.CommandBus;
import com.hibuka.soda.cqrs.command.CommandHandler;
import com.hibuka.soda.cqrs.query.QueryBus;
import com.hibuka.soda.cqrs.query.QueryHandler;
import com.hibuka.soda.bus.impl.SimpleCommandBus;
import com.hibuka.soda.bus.impl.SimpleQueryBus;
import com.hibuka.soda.cqrs.event.EventBus;
import com.hibuka.soda.bus.interceptor.CqrsAroundHandler;
import com.hibuka.soda.bus.configuration.EventProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Spring Bus auto-configuration for CQRS buses and BusFacade.
 *
 * @author kangzeng.ckz
 * @since 2025/12/13
 */
@Configuration
@EnableConfigurationProperties(AsyncConfig.class)
public class SpringBusAutoConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(SpringBusAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public CommandBus commandBus(List<CommandHandler<?, ?>> commandHandlers,
                                EventProperties eventProperties) {
        return new SimpleCommandBus(commandHandlers, eventProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public QueryBus queryBus(List<QueryHandler<?, ?>> queryHandlers) {
        return new SimpleQueryBus(queryHandlers);
    }

    @Bean("cqrsAsyncExecutor")
    @ConditionalOnMissingBean(name = "cqrsAsyncExecutor")
    public Executor cqrsAsyncExecutor(AsyncConfig asyncConfig) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncConfig.getCorePoolSize());
        executor.setMaxPoolSize(asyncConfig.getMaxPoolSize());
        executor.setQueueCapacity(asyncConfig.getQueueCapacity());
        executor.setThreadNamePrefix(asyncConfig.getThreadNamePrefix());
        
        // 配置线程存活时间，避免空闲线程长时间占用资源
        executor.setKeepAliveSeconds(60);
        
        // 配置拒绝策略，避免任务丢失
        executor.setRejectedExecutionHandler((r, executor1) -> {
            logger.warn("[BusAutoConfiguration] Thread pool is full, rejecting task: {}", r.getClass().getName());
            // 可以根据实际需求调整拒绝策略，比如记录日志、发送告警等
            // 这里使用CallerRunsPolicy的思路，但增加了日志记录
            if (!executor1.isShutdown()) {
                try {
                    // 在调用者线程执行任务，避免任务丢失
                    r.run();
                } catch (Exception e) {
                    logger.error("[BusAutoConfiguration] Error executing task in caller thread: {}", e.getMessage(), e);
                }
            }
        });
        
        // 配置线程池监控
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        // 配置线程池指标收集
        executor.initialize();
        
        logger.info("[BusAutoConfiguration] cqrsAsyncExecutor initialized: corePoolSize={}, maxPoolSize={}, queueCapacity={}, threadNamePrefix={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity(), executor.getThreadNamePrefix());
        
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean
    public BusFacade busFacade(CommandBus commandBus,
                             QueryBus queryBus,
                             @Qualifier("cqrsAsyncExecutor") Executor cqrsAsyncExecutor) {
        return new BusFacade(commandBus, queryBus, cqrsAsyncExecutor);
    }

    @Bean
    @ConditionalOnMissingBean
    public CqrsAroundHandler cqrsAroundHandler(EventBus eventBus, EventProperties eventProperties) {
        return new CqrsAroundHandler(eventBus, eventProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public EventProperties eventProperties() {
        return new EventProperties();
    }
}
