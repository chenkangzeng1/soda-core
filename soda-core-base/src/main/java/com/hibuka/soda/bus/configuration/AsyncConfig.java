package com.hibuka.soda.bus.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Common CQRS async thread pool configuration, supports starter configuration override.
 *
 * @author kangzeng.ckz
 * @since 2025/7/3
 **/
@Data
@Validated
@ConfigurationProperties(prefix = "scoda.ddd.cqrs.async")
public class AsyncConfig {
    @PositiveOrZero(message = "Core pool size must be positive or zero")
    private int corePoolSize = 8;
    
    @Positive(message = "Max pool size must be positive")
    private int maxPoolSize = 16;
    
    @PositiveOrZero(message = "Queue capacity must be positive or zero")
    private int queueCapacity = 100;
    
    @NotBlank(message = "Thread name prefix cannot be blank")
    private String threadNamePrefix = "cqrs-async-";
}