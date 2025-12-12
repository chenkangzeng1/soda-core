package com.hibuka.soda.event.redis;

import com.hibuka.soda.cqrs.handle.EventBus;
import com.hibuka.soda.cqrs.handle.EventHandler;
import com.hibuka.soda.core.EventProperties;
import com.hibuka.soda.domain.DomainEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

/**
 * Redis event bus auto-configuration class.
 * This configuration is only loaded when Redis dependencies are present and soda.event.bus-type is set to "redis".
 *
 * @author kangzeng.ckz
 * @since 2024/12/09
 */
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@ConditionalOnClass(RedisTemplate.class)
@ConditionalOnProperty(name = "soda.event.bus-type", havingValue = "redis")
@EnableConfigurationProperties(EventProperties.class)
public class RedisEventBusAutoConfiguration {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RedisEventBusAutoConfiguration.class);
    private final EventProperties eventProperties;
    
    /**
     * Constructor for RedisEventBusAutoConfiguration.
     *
     * @param eventProperties Event properties from application.yml
     */
    @Autowired
    public RedisEventBusAutoConfiguration(EventProperties eventProperties) {
        logger.info("[RedisEventBusAutoConfiguration] Constructor called");
        this.eventProperties = eventProperties;
        logger.info("[RedisEventBusAutoConfiguration] Bus type: {}", eventProperties.getBusType());
    }
    
    /**
     * Creates a Redis template with proper serializers for event publishing.
     * Default name is sodaRedisEventBusTemplate to avoid conflicts with user-defined Redis templates.
     *
     * @param redisConnectionFactory Redis connection factory
     * @return Configured Redis template
     */
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public RedisTemplate<String, Object> sodaRedisEventBusTemplate(RedisConnectionFactory redisConnectionFactory) {
        logger.info("[RedisEventBusAutoConfiguration] Creating sodaRedisEventBusTemplate");
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        
        // Create ObjectMapper with Java 8 date/time support and other necessary configurations
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        
        // Add SmartCircularReferenceSerializerModifier to handle circular references automatically
        objectMapper.setSerializerFactory(
            objectMapper.getSerializerFactory()
                .withSerializerModifier(new SmartCircularReferenceSerializerModifier())
        );
        
        // Configure circular reference handling based on properties
        configureCircularReferenceHandling(objectMapper);
        
        // Add global configuration to ignore common circular reference types
        // This prevents StackOverflowError and serialization issues
        configureGlobalIgnoredTypes(objectMapper);
        
        // Use customized ObjectMapper for JSON serialization
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        template.afterPropertiesSet();
        logger.info("[RedisEventBusAutoConfiguration] Created sodaRedisEventBusTemplate");
        return template;
    }
    
    /**
     * Creates a Redis Pub/Sub event bus instance when Stream mode is disabled.
     *
     * @param sodaRedisEventBusTemplate Default Redis template for soda event bus
     * @param applicationEventPublisher Spring's application event publisher
     * @param redisConnectionFactory Redis connection factory
     * @param eventHandlers List of event handlers to register
     * @return Redis Pub/Sub event bus instance
     */
    @Bean
    @Primary
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnProperty(name = "soda.event.redis.stream.enabled", havingValue = "false", matchIfMissing = true)
    public EventBus redisEventBus(@Qualifier("sodaRedisEventBusTemplate") RedisTemplate<String, Object> sodaRedisEventBusTemplate,
                                 ApplicationEventPublisher applicationEventPublisher,
                                 RedisConnectionFactory redisConnectionFactory,
                                 List<EventHandler<? extends DomainEvent>> eventHandlers) {
        logger.info("[RedisEventBusAutoConfiguration] Creating RedisEventBus (Pub/Sub mode)");
        // Get topic name from configuration or use default
        String topicName = eventProperties.getRedis().getTopic();
        logger.info("[RedisEventBusAutoConfiguration] Redis topic: {}", topicName);
        
        RedisEventBus redisEventBus = new RedisEventBus(
            sodaRedisEventBusTemplate,
            applicationEventPublisher,
            redisConnectionFactory,
            eventHandlers,
            topicName
        );
        
        logger.info("[RedisEventBusAutoConfiguration] Created RedisEventBus (Pub/Sub mode)");
        return redisEventBus;
    }
    
    /**
     * Creates a Redis Stream event bus instance when Stream mode is enabled.
     *
     * @param sodaRedisEventBusTemplate Default Redis template for soda event bus
     * @param applicationEventPublisher Spring's application event publisher
     * @param redisConnectionFactory Redis connection factory
     * @param eventHandlers List of event handlers to register
     * @return Redis Stream event bus instance
     */
    @Bean
    @Primary
    @ConditionalOnBean(RedisConnectionFactory.class)
    @ConditionalOnProperty(name = "soda.event.redis.stream.enabled", havingValue = "true")
    public EventBus redisStreamEventBus(@Qualifier("sodaRedisEventBusTemplate") RedisTemplate<String, Object> sodaRedisEventBusTemplate,
                                       ApplicationEventPublisher applicationEventPublisher,
                                       RedisConnectionFactory redisConnectionFactory,
                                       List<EventHandler<? extends DomainEvent>> eventHandlers) {
        logger.info("[RedisEventBusAutoConfiguration] Creating RedisStreamEventBus (Stream mode)");
        // Get configuration from properties
        String topicName = eventProperties.getRedis().getTopic();
        String groupName = eventProperties.getRedis().getStream().getGroupName();
        String consumerName = eventProperties.getRedis().getStream().getConsumerName();
        long maxlen = eventProperties.getRedis().getStream().getMaxlen();
        long pollTimeout = eventProperties.getRedis().getStream().getPollTimeout();
        int maxRetries = eventProperties.getRedis().getStream().getMaxRetries();
        long initialRetryDelay = eventProperties.getRedis().getStream().getInitialRetryDelay();
        boolean exponentialBackoff = eventProperties.getRedis().getStream().isExponentialBackoff();
        String deadLetterStream = eventProperties.getRedis().getStream().getDeadLetterStream();
        
        logger.info("[RedisEventBusAutoConfiguration] Redis Stream configuration: topic={}, group={}, consumer={}, maxlen={}, pollTimeout={}, maxRetries={}, initialRetryDelay={}, exponentialBackoff={}, deadLetterStream={}",
                   topicName, groupName, consumerName, maxlen, pollTimeout, maxRetries, initialRetryDelay, exponentialBackoff, deadLetterStream);
        
        RedisStreamEventBus redisStreamEventBus = new RedisStreamEventBus(
            sodaRedisEventBusTemplate,
            applicationEventPublisher,
            redisConnectionFactory,
            eventHandlers,
            topicName,
            groupName,
            consumerName,
            maxlen,
            pollTimeout,
            maxRetries,
            initialRetryDelay,
            exponentialBackoff,
            deadLetterStream,
            eventProperties.getRedis().getStream().getIdempotency()
        );
        
        logger.info("[RedisEventBusAutoConfiguration] Created RedisStreamEventBus (Stream mode)");
        return redisStreamEventBus;
    }
    
    /**
     * Configures circular reference handling based on properties.
     * 
     * @param objectMapper ObjectMapper to configure
     */
    private void configureCircularReferenceHandling(ObjectMapper objectMapper) {
        // Set fail on self references based on properties
        objectMapper.configure(
            com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_SELF_REFERENCES,
            eventProperties.getSerialization().isFailOnSelfReferences()
        );
        
        // Configure circular reference handler based on properties
        switch (eventProperties.getSerialization().getCircularReferenceHandler()) {
            case IGNORE:
                // Ignore circular references by not including them in serialization
                objectMapper.configure(
                    com.fasterxml.jackson.databind.SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL,
                    true
                );
                break;
            case ERROR:
                // Throw error when circular references are detected
                objectMapper.configure(
                    com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_SELF_REFERENCES,
                    true
                );
                break;
            case RETAIN:
                // For retaining circular references, Jackson uses JsonIdentityInfo by default
                // We'll keep default behavior which handles circular references automatically
                break;
            default:
                // Default to ignore
                objectMapper.configure(
                    com.fasterxml.jackson.databind.SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL,
                    true
                );
        }
    }
    
    /**
     * Configures global ignored types to prevent circular reference issues.
     * 
     * @param objectMapper ObjectMapper to configure
     */
    private void configureGlobalIgnoredTypes(ObjectMapper objectMapper) {
        try {
            // Configure circular reference handling
            objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
            
            // Ignore Logger and related types
            objectMapper.addMixIn(Logger.class, IgnoreMixIn.class);
            
            // Ignore Logback related types that cause circular references
            try {
                Class<?> loggerContextClass = Class.forName("ch.qos.logback.classic.LoggerContext");
                Class<?> appenderClass = Class.forName("ch.qos.logback.core.Appender");
                objectMapper.addMixIn(loggerContextClass, IgnoreMixIn.class);
                objectMapper.addMixIn(appenderClass, IgnoreMixIn.class);
            } catch (ClassNotFoundException e) {
                // Logback classes might not be present in all environments
                logger.debug("[RedisEventBusAutoConfiguration] Logback classes not found, skipping");
            }
            
            // Ignore Spring framework related types
            try {
                Class<?> applicationContextClass = Class.forName("org.springframework.context.ApplicationContext");
                Class<?> beanFactoryClass = Class.forName("org.springframework.beans.factory.BeanFactory");
                Class<?> environmentClass = Class.forName("org.springframework.core.env.Environment");
                Class<?> applicationEventPublisherClass = Class.forName("org.springframework.context.ApplicationEventPublisher");
                objectMapper.addMixIn(applicationContextClass, IgnoreMixIn.class);
                objectMapper.addMixIn(beanFactoryClass, IgnoreMixIn.class);
                objectMapper.addMixIn(environmentClass, IgnoreMixIn.class);
                objectMapper.addMixIn(applicationEventPublisherClass, IgnoreMixIn.class);
            } catch (ClassNotFoundException e) {
                logger.debug("[RedisEventBusAutoConfiguration] Some Spring framework classes not found, skipping");
            }
            
            // Ignore thread pool and concurrency related types
            try {
                Class<?> executorServiceClass = Class.forName("java.util.concurrent.ExecutorService");
                Class<?> threadPoolExecutorClass = Class.forName("java.util.concurrent.ThreadPoolExecutor");
                Class<?> scheduledExecutorServiceClass = Class.forName("java.util.concurrent.ScheduledExecutorService");
                Class<?> lockClass = Class.forName("java.util.concurrent.locks.Lock");
                Class<?> semaphoreClass = Class.forName("java.util.concurrent.Semaphore");
                objectMapper.addMixIn(executorServiceClass, IgnoreMixIn.class);
                objectMapper.addMixIn(threadPoolExecutorClass, IgnoreMixIn.class);
                objectMapper.addMixIn(scheduledExecutorServiceClass, IgnoreMixIn.class);
                objectMapper.addMixIn(lockClass, IgnoreMixIn.class);
                objectMapper.addMixIn(semaphoreClass, IgnoreMixIn.class);
            } catch (ClassNotFoundException e) {
                logger.debug("[RedisEventBusAutoConfiguration] Some concurrency classes not found, skipping");
            }
            
            // Ignore cache related types
            try {
                Class<?> cacheClass = Class.forName("org.springframework.cache.Cache");
                Class<?> cacheManagerClass = Class.forName("org.springframework.cache.CacheManager");
                objectMapper.addMixIn(cacheClass, IgnoreMixIn.class);
                objectMapper.addMixIn(cacheManagerClass, IgnoreMixIn.class);
            } catch (ClassNotFoundException e) {
                logger.debug("[RedisEventBusAutoConfiguration] Some cache classes not found, skipping");
            }
            
            logger.info("[RedisEventBusAutoConfiguration] Configured global ignored types for circular reference prevention");
        } catch (Exception e) {
            logger.warn("[RedisEventBusAutoConfiguration] Error configuring global ignored types: {}", e.getMessage());
        }
    }
    
    /**
     * MixIn class to ignore fields during serialization.
     * This prevents StackOverflowError and serialization issues with circular references.
     */
    @JsonIgnoreProperties(value = {"*"}, allowGetters = false, allowSetters = false)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE) // Ignore type information
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
                   getterVisibility = JsonAutoDetect.Visibility.NONE,
                   setterVisibility = JsonAutoDetect.Visibility.NONE,
                   isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                   creatorVisibility = JsonAutoDetect.Visibility.NONE)
    private static class IgnoreMixIn {
        // This class doesn't need any implementation, it's just used to define Jackson serialization rules
    }
}