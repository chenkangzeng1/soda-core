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