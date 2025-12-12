package com.hibuka.soda.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Event bus configuration properties.
 *
 * @author kangzeng.ckz
 * @since 2024/12/09
 */
@ConfigurationProperties(prefix = "soda.event")
public class EventProperties {
    /**
     * Event bus type: spring, redis.
     */
    private String busType = "spring";

    /**
     * Spring event bus configuration.
     */
    private SpringProperties spring = new SpringProperties();

    /**
     * Redis event bus configuration.
     */
    private RedisProperties redis = new RedisProperties();

    /**
     * Serialization configuration.
     */
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
        private String topic = "soda-events";

        /**
         * Redis host.
         */
        private String host = "localhost";

        /**
         * Redis port.
         */
        private int port = 6379;

        /**
         * Redis password.
         */
        private String password;

        /**
         * Redis database index.
         */
        private int database = 0;

        /**
         * Redis Stream configuration.
         */
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
            private String groupName = "soda-events-group";

            /**
             * Consumer name.
             */
            private String consumerName = "soda-events-consumer-" + System.getProperty("server.port", "0");

            /**
             * Maximum length of the Stream.
             */
            private long maxlen = 10000;

            /**
             * Poll timeout in milliseconds.
             */
            private long pollTimeout = 1000;

            /**
             * Acknowledge timeout in milliseconds.
             */
            private long acknowledgeTimeout = 30000;

            /**
             * Maximum number of retries for failed messages.
             */
            private int maxRetries = 3;

            /**
             * Initial retry delay in milliseconds.
             */
            private long initialRetryDelay = 1000;

            /**
             * Whether to use exponential backoff for retries.
             */
            private boolean exponentialBackoff = true;

            /**
             * Name of the dead letter stream.
             */
            private String deadLetterStream = "soda-events-dead-letter";

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
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