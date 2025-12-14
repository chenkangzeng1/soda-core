package com.hibuka.soda.bus.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Event bus configuration properties.
 *
 * @author kangzeng.ckz
 * @since 2024/12/09
 */
@ConfigurationProperties(prefix = "soda.event")
@Validated
public class EventProperties {
    /**
     * Event bus type: spring, redis.
     */
    @NotBlank(message = "Event bus type cannot be blank")
    private String busType = "spring";

    /**
     * Spring event bus configuration.
     */
    @NotNull(message = "Spring event bus configuration cannot be null")
    private SpringProperties spring = new SpringProperties();

    /**
     * Redis event bus configuration.
     */
    @NotNull(message = "Redis event bus configuration cannot be null")
    private RedisProperties redis = new RedisProperties();

    /**
     * Serialization configuration.
     */
    @NotNull(message = "Serialization configuration cannot be null")
    private SerializationProperties serialization = new SerializationProperties();

    public String getBusType() {
        return busType;
    }

    public void setBusType(String busType) {
        this.busType = busType;
    }

    public SpringProperties getSpring() {
        return spring;
    }

    public void setSpring(SpringProperties spring) {
        this.spring = spring;
    }

    public RedisProperties getRedis() {
        return redis;
    }

    public void setRedis(RedisProperties redis) {
        this.redis = redis;
    }

    public SerializationProperties getSerialization() {
        return serialization;
    }

    public void setSerialization(SerializationProperties serialization) {
        this.serialization = serialization;
    }

    /**
     * Spring event bus configuration.
     */
    public static class SpringProperties {
        // Spring event bus is enabled by default when bus-type is spring
    }

    /**
         * Redis event bus configuration.
         */
        public static class RedisProperties {
            /**
             * Redis topic for event publishing.
             */
            @NotBlank(message = "Redis topic cannot be blank")
            private String topic = "soda-events";

            /**
             * Redis host.
             */
            @NotBlank(message = "Redis host cannot be blank")
            private String host = "localhost";

            /**
             * Redis port.
             */
            @Positive(message = "Redis port must be positive")
            private int port = 6379;

            /**
             * Redis password.
             */
            private String password;

            /**
             * Redis database index.
             */
            @PositiveOrZero(message = "Redis database index must be positive or zero")
            private int database = 0;

            /**
             * Redis Stream configuration.
             */
            @NotNull(message = "Redis Stream configuration cannot be null")
            private StreamProperties stream = new StreamProperties();

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }

        public StreamProperties getStream() {
            return stream;
        }

        public void setStream(StreamProperties stream) {
            this.stream = stream;
        }

        /**
             * Redis Stream configuration properties.
             */
            public static class StreamProperties {
                /**
                 * Whether to enable Redis Stream mode.
                 */
                private boolean enabled = false;

                /**
                 * Consumer group name.
                 */
                @NotBlank(message = "Consumer group name cannot be blank")
                private String groupName = "soda-events-group";

                /**
                 * Consumer name.
                 */
                @NotBlank(message = "Consumer name cannot be blank")
                private String consumerName = "soda-events-consumer-" + System.getProperty("server.port", "0");

                /**
                 * Maximum length of the Stream.
                 */
                @Positive(message = "Stream maximum length must be positive")
                private long maxlen = 10000;

                /**
                 * Poll timeout in milliseconds.
                 */
                @Positive(message = "Poll timeout must be positive")
                private long pollTimeout = 1000;

                /**
                 * Acknowledge timeout in milliseconds.
                 */
                @Positive(message = "Acknowledge timeout must be positive")
                private long acknowledgeTimeout = 30000;

                /**
                 * Maximum number of retries for failed messages.
                 */
                @PositiveOrZero(message = "Maximum retries must be positive or zero")
                private int maxRetries = 3;

                /**
                 * Initial retry delay in milliseconds.
                 */
                @Positive(message = "Initial retry delay must be positive")
                private long initialRetryDelay = 1000;

                /**
                 * Whether to use exponential backoff for retries.
                 */
                private boolean exponentialBackoff = true;

                /**
                 * Name of the dead letter stream.
                 */
                @NotBlank(message = "Dead letter stream name cannot be blank")
                private String deadLetterStream = "soda-events-dead-letter";
                
                /**
                 * Idempotency configuration for event processing.
                 */
                @NotNull(message = "Idempotency configuration cannot be null")
                private IdempotencyProperties idempotency = new IdempotencyProperties();

            public boolean isEnabled() {
                return enabled;
            }
            
            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
            
            public IdempotencyProperties getIdempotency() {
                return idempotency;
            }
            
            public void setIdempotency(IdempotencyProperties idempotency) {
                this.idempotency = idempotency;
            }
            
            /**
                 * Idempotency configuration properties.
                 */
                public static class IdempotencyProperties {
                    /**
                     * Whether to enable idempotency for event processing.
                     */
                    private boolean enabled = false;
                    
                    /**
                     * Redis key prefix for idempotency status storage.
                     */
                    @NotBlank(message = "Redis key prefix cannot be blank")
                    private String redisKeyPrefix = "soda-events-idempotency";
                    
                    /**
                     * Expiration time for idempotency status in seconds.
                     */
                    @Positive(message = "Expiration time must be positive")
                    private long expireTime = 86400; // 24 hours
                    
                    /**
                     * Retry interval for idempotency status checks in milliseconds.
                     */
                    @Positive(message = "Retry interval must be positive")
                    private long retryInterval = 100;
                
                public boolean isEnabled() {
                    return enabled;
                }
                
                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }
                
                public String getRedisKeyPrefix() {
                    return redisKeyPrefix;
                }
                
                public void setRedisKeyPrefix(String redisKeyPrefix) {
                    this.redisKeyPrefix = redisKeyPrefix;
                }
                
                public long getExpireTime() {
                    return expireTime;
                }
                
                public void setExpireTime(long expireTime) {
                    this.expireTime = expireTime;
                }
                
                public long getRetryInterval() {
                    return retryInterval;
                }
                
                public void setRetryInterval(long retryInterval) {
                    this.retryInterval = retryInterval;
                }
            }

            public String getGroupName() {
                return groupName;
            }

            public void setGroupName(String groupName) {
                this.groupName = groupName;
            }

            public String getConsumerName() {
                return consumerName;
            }

            public void setConsumerName(String consumerName) {
                this.consumerName = consumerName;
            }

            public long getMaxlen() {
                return maxlen;
            }

            public void setMaxlen(long maxlen) {
                this.maxlen = maxlen;
            }

            public long getPollTimeout() {
                return pollTimeout;
            }

            public void setPollTimeout(long pollTimeout) {
                this.pollTimeout = pollTimeout;
            }

            public long getAcknowledgeTimeout() {
                return acknowledgeTimeout;
            }

            public void setAcknowledgeTimeout(long acknowledgeTimeout) {
                this.acknowledgeTimeout = acknowledgeTimeout;
            }

            public int getMaxRetries() {
                return maxRetries;
            }

            public void setMaxRetries(int maxRetries) {
                this.maxRetries = maxRetries;
            }

            public long getInitialRetryDelay() {
                return initialRetryDelay;
            }

            public void setInitialRetryDelay(long initialRetryDelay) {
                this.initialRetryDelay = initialRetryDelay;
            }

            public boolean isExponentialBackoff() {
                return exponentialBackoff;
            }

            public void setExponentialBackoff(boolean exponentialBackoff) {
                this.exponentialBackoff = exponentialBackoff;
            }

            public String getDeadLetterStream() {
                return deadLetterStream;
            }

            public void setDeadLetterStream(String deadLetterStream) {
                this.deadLetterStream = deadLetterStream;
            }
        }
    }

    /**
         * Serialization configuration for JSON processing.
         */
        public static class SerializationProperties {
            /**
             * Circular reference handling strategy: IGNORE, ERROR, RETAIN.
             */
            @NotNull(message = "Circular reference handling strategy cannot be null")
            private CircularReferenceHandler circularReferenceHandler = CircularReferenceHandler.IGNORE;

            /**
             * Whether to fail on self references during serialization.
             */
            private boolean failOnSelfReferences = false;

        public CircularReferenceHandler getCircularReferenceHandler() {
            return circularReferenceHandler;
        }

        public void setCircularReferenceHandler(CircularReferenceHandler circularReferenceHandler) {
            this.circularReferenceHandler = circularReferenceHandler;
        }

        public boolean isFailOnSelfReferences() {
            return failOnSelfReferences;
        }

        public void setFailOnSelfReferences(boolean failOnSelfReferences) {
            this.failOnSelfReferences = failOnSelfReferences;
        }

        /**
         * Circular reference handling strategies.
         */
        public enum CircularReferenceHandler {
            /**
             * Ignore circular references (default behavior).
             */
            IGNORE,
            /**
             * Throw an error when circular references are detected.
             */
            ERROR,
            /**
             * Retain circular references using JSON references.
             */
            RETAIN
        }
    }
}