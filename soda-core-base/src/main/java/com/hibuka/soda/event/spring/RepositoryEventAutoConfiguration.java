package com.hibuka.soda.event.spring;

import com.hibuka.soda.cqrs.event.EventBus;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.beans.factory.ObjectProvider;

@Configuration
@EnableAspectJAutoProxy
@ConditionalOnBean(EventBus.class)
@AutoConfigureAfter(name = {
        "com.hibuka.soda.event.spring.SpringEventBusAutoConfiguration",
        "com.hibuka.soda.event.redis.RedisEventBusAutoConfiguration"
})
public class RepositoryEventAutoConfiguration {
    @Bean
    public RepositoryEventAspect repositoryEventAspect(EventBus eventBus, ObjectProvider<com.hibuka.soda.context.CommandContext> commandContextProvider) {
        return new RepositoryEventAspect(eventBus, commandContextProvider);
    }
}
