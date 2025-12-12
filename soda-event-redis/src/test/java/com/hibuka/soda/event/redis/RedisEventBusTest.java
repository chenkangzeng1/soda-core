package com.hibuka.soda.event.redis;

import com.hibuka.soda.cqrs.handle.EventBus;
import com.hibuka.soda.core.EventProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for RedisEventBus.
 *
 * @author kangzeng.ckz
 * @since 2024/12/09
 */
class RedisEventBusTest {
    
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisEventBusAutoConfiguration.class));
    
    /**
     * Test that RedisEventBus is not created when bus-type is not "redis".
     */
    @Test
    void testRedisEventBusNotCreatedWhenBusTypeNotRedis() {
        contextRunner.withPropertyValues("soda.event.bus-type=spring")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RedisEventBus.class);
                    assertThat(context).doesNotHaveBean("redisEventBusTemplate");
                });
    }
    
    /**
     * Test that RedisEventBus auto-configuration properties are loaded correctly.
     */
    @Test
    void testEventPropertiesLoaded() {
        contextRunner.withPropertyValues(
                "soda.event.bus-type=redis",
                "soda.event.redis.topic=custom-topic"
        )
        .run(context -> {
            EventProperties properties = context.getBean(EventProperties.class);
            assertThat(properties).isNotNull();
            assertThat(properties.getRedis().getTopic()).isEqualTo("custom-topic");
        });
    }
    
    /**
     * Test that RedisEventBus beans are not created when Redis dependencies are not available.
     * This test relies on the fact that RedisTemplate is not available in the test context.
     */
    @Test
    void testRedisEventBusBeansNotCreatedWhenRedisNotAvailable() {
        contextRunner.withPropertyValues("soda.event.bus-type=redis")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RedisEventBus.class);
                    assertThat(context).doesNotHaveBean(RedisTemplate.class);
                });
    }
}
