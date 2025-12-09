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

    /**
     * Spring event bus configuration.
     */
    public static class SpringProperties {
        /**
         * Whether spring event bus is enabled.
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Redis event bus configuration.
     */
    public static class RedisProperties {
        /**
         * Whether redis event bus is enabled.
         */
        private boolean enabled = false;

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

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

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
}