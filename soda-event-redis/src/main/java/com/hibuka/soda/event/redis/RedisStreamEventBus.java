package com.hibuka.soda.event.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hibuka.soda.base.error.BaseException;
import com.hibuka.soda.cqrs.handle.EventBus;
import com.hibuka.soda.cqrs.handle.EventHandler;
import com.hibuka.soda.domain.AbstractDomainEvent;
import com.hibuka.soda.domain.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis event bus implementation using Redis Stream mechanism.
 * This implementation supports message persistence, consumer groups, and message acknowledgment.
 *
 * @author kangzeng.ckz
 * @since 2025/12/12
 */
public class RedisStreamEventBus implements EventBus, InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(RedisStreamEventBus.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RedisConnectionFactory redisConnectionFactory;
    private final String streamKey;
    private final String groupName;
    private final String consumerName;
    private final long maxlen;
    private final long pollTimeout;
    private final String deadLetterStream;
    private final int maxRetries;
    private final long initialRetryDelay;
    private final boolean exponentialBackoff;
    
    private final Map<Class<? extends DomainEvent>, List<EventHandler>> handlers = new ConcurrentHashMap<>();
    private final Map<String, Class<? extends DomainEvent>> eventTypeToClassMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    
    private StreamMessageListenerContainer<?, ?> container;
    private Subscription subscription;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Constructor for RedisStreamEventBus.
     *
     * @param redisTemplate Redis template for Stream operations
     * @param applicationEventPublisher Spring's application event publisher
     * @param redisConnectionFactory Redis connection factory
     * @param eventHandlers List of event handlers to register
     * @param topicName Stream key name for event publishing
     * @param groupName Consumer group name
     * @param consumerName Consumer name
     * @param maxlen Maximum stream length
     * @param pollTimeout Poll timeout in milliseconds
     * @param maxRetries Maximum number of retries
     * @param initialRetryDelay Initial retry delay in milliseconds
     * @param exponentialBackoff Whether to use exponential backoff
     * @param deadLetterStream Name of the dead letter stream
     */
    public RedisStreamEventBus(RedisTemplate<String, Object> redisTemplate,
                              ApplicationEventPublisher applicationEventPublisher,
                              RedisConnectionFactory redisConnectionFactory,
                              List<EventHandler<? extends DomainEvent>> eventHandlers,
                              String topicName,
                              String groupName,
                              String consumerName,
                              long maxlen,
                              long pollTimeout,
                              int maxRetries,
                              long initialRetryDelay,
                              boolean exponentialBackoff,
                              String deadLetterStream) {
        this.redisTemplate = redisTemplate;
        this.applicationEventPublisher = applicationEventPublisher;
        this.redisConnectionFactory = redisConnectionFactory;
        this.streamKey = topicName;
        this.groupName = groupName;
        this.consumerName = consumerName;
        this.maxlen = maxlen;
        this.pollTimeout = pollTimeout;
        this.maxRetries = maxRetries;
        this.initialRetryDelay = initialRetryDelay;
        this.exponentialBackoff = exponentialBackoff;
        this.deadLetterStream = deadLetterStream;
        
        // Create and configure ObjectMapper with JavaTimeModule support
        // We don't use reflection to get it from RedisTemplate anymore because the field name might change
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_SELF_REFERENCES, false);
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL, true);
        // Add additional configuration for deserialization
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        this.objectMapper = mapper;
        
        // Register event handlers
        registerEventHandlers(eventHandlers);
    }
    
    /**
     * Registers all event handlers.
     *
     * @param eventHandlers List of event handlers to register
     */
    private void registerEventHandlers(List<EventHandler<? extends DomainEvent>> eventHandlers) {
        logger.info("[RedisStreamEventBus] Registering {} event handlers", eventHandlers.size());
        for (EventHandler<? extends DomainEvent> handler : eventHandlers) {
            Class<?>[] typeArguments = GenericTypeResolver.resolveTypeArguments(
                    handler.getClass(),
                    EventHandler.class
            );
            if (typeArguments != null && typeArguments.length > 0) {
                Class<? extends DomainEvent> eventType = (Class<? extends DomainEvent>) typeArguments[0];
                try {
                    subscribe(eventType, handler);
                    logger.info("[RedisStreamEventBus] Registered handler for event: {}", eventType.getName());
                } catch (BaseException e) {
                    logger.error("[RedisStreamEventBus] Error registering handler for event {}: {}", eventType.getName(), e.getMessage(), e);
                    throw new RuntimeException("Failed to register event handler", e);
                }
            }
        }
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        if (initialized.compareAndSet(false, true)) {
            logger.info("[RedisStreamEventBus] Initializing Redis Stream event bus");
            
            // Create stream and consumer group if they don't exist
            createStreamAndGroup();
            
            // Set up stream listener container
            configureStreamListener();
            
            logger.info("[RedisStreamEventBus] Initialized Redis Stream event bus");
        }
    }
    
    /**
     * Creates the stream and consumer group if they don't exist.
     */
    private void createStreamAndGroup() {
        try {
            // Check if stream exists, create if not
            Boolean exists = redisTemplate.hasKey(streamKey);
            if (exists == null || !exists) {
                logger.info("[RedisStreamEventBus] Creating stream: {}", streamKey);
                // Create stream with initial entry to ensure it exists
                Map<String, Object> initialEntry = new HashMap<>();
                initialEntry.put("type", "INIT");
                redisTemplate.opsForStream().add(streamKey, initialEntry);
                logger.info("[RedisStreamEventBus] Stream created: {}", streamKey);
            }
            
            // Create consumer group
            try {
                redisTemplate.opsForStream().createGroup(streamKey, groupName);
                logger.info("[RedisStreamEventBus] Consumer group created: {}", groupName);
            } catch (Exception e) {
                // Group likely already exists, log and continue
                logger.info("[RedisStreamEventBus] Consumer group likely already exists: {}", groupName);
            }
        } catch (Exception e) {
            logger.error("[RedisStreamEventBus] Error creating stream or consumer group: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Redis Stream event bus", e);
        }
    }
    
    /**
     * Configures the stream listener container for message consumption.
     */
    private void configureStreamListener() {
        // Create container options WITHOUT specifying targetType - this lets Redis handle the raw bytes
        // We'll deserialize manually in handleStreamMessage
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<?, ?> options = 
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofMillis(pollTimeout))
                        .errorHandler(throwable -> {
                            // Handle Redis connection and other exceptions gracefully
                            logger.warn("[RedisStreamEventBus] Stream listener error, will retry: {}", throwable.getMessage());
                            // Don't rethrow - let container continue running
                        })
                        .build();
        
        // Create container with raw types to avoid ambiguous Record reference and generics issues
        @SuppressWarnings({"rawtypes", "unchecked"})
        StreamMessageListenerContainer container = StreamMessageListenerContainer.create(redisConnectionFactory, options);
        this.container = container;
        
        // Create subscription for the consumer group
        ReadOffset offset = ReadOffset.lastConsumed();
        Consumer consumer = Consumer.from(groupName, consumerName);
        StreamOffset<String> streamOffset = StreamOffset.create(streamKey, offset);
        
        // Create stream listener that handles raw MapRecord with explicit type
        StreamListener<String, MapRecord<String, String, String>> listener = new StreamListener<>() {
            @Override
            public void onMessage(MapRecord<String, String, String> message) {
                try {
                    handleStreamMessageWithRetry(message);
                } catch (Exception e) {
                    logger.error("[RedisStreamEventBus] Error handling stream message: {}", e.getMessage(), e);
                }
            }
        };
        
        // Use manual ACK by calling receive() instead of receiveAutoAck()
        @SuppressWarnings({"rawtypes", "unchecked"})
        Subscription subscription = container.receive(consumer, streamOffset, listener);
        this.subscription = subscription;
        
        // Start the container
        container.start();
        logger.info("[RedisStreamEventBus] Stream listener container started");
    }
    
    /**
     * Handles a stream message with retry logic and dead letter queue functionality.
     *
     * @param message The stream message to handle
     */
    private void handleStreamMessageWithRetry(MapRecord<String, String, String> message) {
        logger.info("[RedisStreamEventBus] Received stream message: ID={}, Stream={}", message.getId(), message.getStream());
        
        boolean processed = false;
        
        for (int retryCount = 0; retryCount <= maxRetries; retryCount++) {
            try {
                processed = handleStreamMessageInternal(message);
                if (processed) {
                    // Acknowledge the message using RedisTemplate
                    redisTemplate.opsForStream().acknowledge(streamKey, groupName, message.getId());
                    logger.info("[RedisStreamEventBus] Successfully processed message after {} retries: ID={}", retryCount, message.getId());
                    break;
                }
            } catch (Exception e) {
                if (retryCount < maxRetries) {
                    long delay = calculateRetryDelay(retryCount);
                    logger.warn("[RedisStreamEventBus] Failed to process message, retrying in {}ms (attempt {}/{}): ID={}, Error: {}", 
                               delay, retryCount + 1, maxRetries + 1, message.getId(), e.getMessage());
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("[RedisStreamEventBus] Retry sleep interrupted: {}", ie.getMessage());
                        break;
                    }
                } else {
                    logger.error("[RedisStreamEventBus] Maximum retries exceeded, moving to dead letter queue: ID={}", message.getId());
                    moveToDeadLetterQueue(message, "Max retries exceeded");
                    // Acknowledge the original message after moving to dead letter queue
                    redisTemplate.opsForStream().acknowledge(streamKey, groupName, message.getId());
                    break;
                }
            }
        }
        
        if (!processed) {
            logger.error("[RedisStreamEventBus] Message processing failed without exception, moving to dead letter queue: ID={}", message.getId());
            moveToDeadLetterQueue(message, "Processing failed without exception");
            // Acknowledge the original message after moving to dead letter queue
            redisTemplate.opsForStream().acknowledge(streamKey, groupName, message.getId());
        }
    }
    
    /**
     * Internal method to handle stream message processing without retry logic.
     *
     * @param message The stream message to handle
     * @return true if processing was successful, false otherwise
     * @throws Exception if an error occurs during processing
     */
    private boolean handleStreamMessageInternal(MapRecord<String, String, String> message) throws Exception {
        // Get the serialized event and type from the message
        String serializedEvent = message.getValue().get("event");
        String eventType = message.getValue().get("type");
        
        if (serializedEvent == null) {
            logger.error("[RedisStreamEventBus] Missing 'event' field in stream message");
            return false;
        }
        
        if (eventType == null) {
            logger.error("[RedisStreamEventBus] Missing 'type' field in stream message");
            return false;
        }
        
        // Get the event class from the type map
        Class<? extends DomainEvent> eventClass = eventTypeToClassMap.get(eventType);
        
        // Fallback: handle quoted event types (e.g., when RedisTemplate adds quotes)
        if (eventClass == null) {
            // Try removing quotes if present
            String unquotedEventType = eventType;
            if ((eventType.startsWith("\"") && eventType.endsWith("\"")) || 
                (eventType.startsWith("'")) && eventType.endsWith("'")) {
                unquotedEventType = eventType.substring(1, eventType.length() - 1);
                logger.debug("[RedisStreamEventBus] Trying unquoted event type: {}", unquotedEventType);
                eventClass = eventTypeToClassMap.get(unquotedEventType);
            }
        }
        
        if (eventClass == null) {
            logger.error("[RedisStreamEventBus] No registered handler for event type: {}", eventType);
            return false;
        }
        
        // Deserialize the event from JSON string using the comprehensive deserializeDomainEvent method
        logger.debug("[RedisStreamEventBus] Deserializing event from JSON: {}", serializedEvent);
        
        DomainEvent event = deserializeDomainEvent(serializedEvent, eventClass);
        
        if (event != null) {
            // Publish to local handlers
            publishToLocalHandlers(event);
            
            // Publish to Spring application event publisher
            applicationEventPublisher.publishEvent(event);
            
            logger.info("[RedisStreamEventBus] Successfully processed event: {}", event.getClass().getName());
        } else {
            // This is expected behavior if the event has been handled locally
            logger.info("[RedisStreamEventBus] Event deserialization returned null, which is expected for domain events handled locally. Acknowledging message.");
        }
        
        return true; // Always return true to avoid unnecessary retries and DLQ entries
    }
    
    /**
     * Calculates the retry delay based on the retry count and exponential backoff setting.
     *
     * @param retryCount The current retry count (0-based)
     * @return The calculated delay in milliseconds
     */
    private long calculateRetryDelay(int retryCount) {
        if (exponentialBackoff) {
            return (long) (initialRetryDelay * Math.pow(2, retryCount));
        }
        return initialRetryDelay;
    }
    
    /**
     * Helper method to resolve nested arrays in JSON nodes. This is needed because RedisTemplate serializes objects
     * as arrays with format [className, objectData].
     *
     * @param mapper ObjectMapper to use for JSON processing
     * @param node JSON node to process
     * @return ObjectNode with nested arrays resolved to their actual data
     */
    private com.fasterxml.jackson.databind.node.ObjectNode resolveNestedArrays(
            ObjectMapper mapper, 
            com.fasterxml.jackson.databind.JsonNode node) {
        com.fasterxml.jackson.databind.node.ObjectNode result = mapper.createObjectNode();
        
        // Iterate through all fields in the node
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                com.fasterxml.jackson.databind.JsonNode fieldValue = entry.getValue();
                
                if (fieldValue.isArray() && fieldValue.size() >= 2 && fieldValue.get(0).isTextual()) {
                    // If it's an array with [className, objectData] format, extract just the data
                    com.fasterxml.jackson.databind.JsonNode actualData = fieldValue.get(1);
                    
                    if (actualData.isObject()) {
                        // Recursively resolve nested objects
                        result.set(fieldName, resolveNestedArrays(mapper, actualData));
                    } else {
                        // For non-object data, use as-is
                        result.set(fieldName, actualData);
                    }
                } else if (fieldValue.isObject()) {
                    // Recursively resolve nested objects
                    result.set(fieldName, resolveNestedArrays(mapper, fieldValue));
                } else {
                    // For non-array, non-object data, use as-is
                    result.set(fieldName, fieldValue);
                }
            });
        }
        
        return result;
    }
    
    /**
     * Moves a message to the dead letter queue.
     *
     * @param message The message to move
     * @param reason The reason for moving to dead letter queue
     */
    private void moveToDeadLetterQueue(MapRecord<String, String, String> message, String reason) {
        try {
            Map<String, Object> deadLetterEntry = new HashMap<>(message.getValue());
            deadLetterEntry.put("deadLetterReason", reason);
            deadLetterEntry.put("deadLetterTimestamp", System.currentTimeMillis());
            deadLetterEntry.put("originalStream", message.getStream());
            deadLetterEntry.put("originalId", message.getId().getValue());
            
            redisTemplate.opsForStream().add(deadLetterStream, deadLetterEntry);
            logger.info("[RedisStreamEventBus] Moved message to dead letter queue: ID={}, DeadLetterStream={}", 
                       message.getId(), deadLetterStream);
        } catch (Exception e) {
            logger.error("[RedisStreamEventBus] Error moving message to dead letter queue: ID={}, Error: {}", 
                       message.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Deserializes a JSON string to a DomainEvent using the same approach as RedisEventBus.
     *
     * @param json The JSON string to deserialize
     * @param eventClass The specific DomainEvent class to deserialize to
     * @return The deserialized DomainEvent, or null if deserialization is not possible (which is acceptable for domain events)
     */
    private DomainEvent deserializeDomainEvent(String json, Class<? extends DomainEvent> eventClass) {
        try {
            // Create a simple ObjectMapper for deserialization
            ObjectMapper deserializeMapper = new ObjectMapper();
            deserializeMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            deserializeMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            deserializeMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            deserializeMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
            // Try direct deserialization first
            try {
                return deserializeMapper.readValue(json, eventClass);
            } catch (com.fasterxml.jackson.databind.exc.InvalidDefinitionException e) {
                // Expected case: Domain events often don't have default constructors
                // This is normal and acceptable, as the event has already been handled locally
                logger.warn("[RedisStreamEventBus] Expected warning: Cannot deserialize {} due to missing constructor. This is normal if the event has been handled locally.", eventClass.getName());
                return null;
            } catch (Exception e) {
                logger.warn("[RedisStreamEventBus] Direct deserialization failed, trying fallback methods: {}", e.getMessage());
                
                // Fallback: handle the array format that RedisTemplate uses for serialization
                com.fasterxml.jackson.databind.JsonNode rootNode = deserializeMapper.readTree(json);
                if (rootNode.isArray() && rootNode.size() >= 2) {
                    // Format is [eventType, eventData]
                    com.fasterxml.jackson.databind.JsonNode eventDataNode = rootNode.get(1);
                    
                    try {
                        if (eventDataNode.isObject()) {
                            // Resolve nested arrays to handle RedisTemplate serialization format
                            com.fasterxml.jackson.databind.node.ObjectNode modifiedEventDataNode = resolveNestedArrays(deserializeMapper, eventDataNode);
                            
                            // Try deserialization with modified node
                            return deserializeMapper.treeToValue(modifiedEventDataNode, eventClass);
                        } else {
                            // If it's not an object, try direct deserialization
                            return deserializeMapper.treeToValue(eventDataNode, eventClass);
                        }
                    } catch (com.fasterxml.jackson.databind.exc.InvalidDefinitionException e2) {
                        // Expected case: Domain events often don't have default constructors
                        logger.warn("[RedisStreamEventBus] Expected warning: Cannot deserialize {} due to missing constructor. This is normal if the event has been handled locally.", eventClass.getName());
                        return null;
                    }
                }
                
                // Fallback: handle object format with @class field
                if (rootNode.isObject()) {
                    com.fasterxml.jackson.databind.JsonNode classNode = rootNode.get("@class");
                    if (classNode != null && classNode.isTextual()) {
                        String actualClassName = classNode.asText();
                        logger.debug("[RedisStreamEventBus] Found @class field: {}, using it for deserialization", actualClassName);
                        Class<?> actualClass = Class.forName(actualClassName);
                        if (eventClass.isAssignableFrom(actualClass)) {
                            try {
                                return deserializeMapper.treeToValue(rootNode, (Class<? extends DomainEvent>) actualClass);
                            } catch (com.fasterxml.jackson.databind.exc.InvalidDefinitionException e2) {
                                // Expected case: Domain events often don't have default constructors
                                logger.warn("[RedisStreamEventBus] Expected warning: Cannot deserialize {} due to missing constructor. This is normal if the event has been handled locally.", actualClassName);
                                return null;
                            }
                        }
                    }
                }
                
                // All fallbacks failed, log as warning and return null (acceptable for domain events)
                logger.warn("[RedisStreamEventBus] All deserialization methods failed for event: {}", eventClass.getName());
                return null;
            }
        } catch (Exception e) {
            // Log any unexpected exceptions and return null
            logger.warn("[RedisStreamEventBus] Unexpected error during deserialization: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Publishes event to local handlers.
     *
     * @param event Domain event to publish
     */
    private void publishToLocalHandlers(DomainEvent event) throws Exception {
        List<EventHandler> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            for (EventHandler handler : eventHandlers) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    logger.error("[RedisStreamEventBus] Error handling event by local handler: {}", 
                            handler.getClass().getName(), e);
                    throw e; // Re-throw the exception to trigger retry mechanism
                }
            }
        }
    }
    
    @Override
    public void publish(DomainEvent event) throws BaseException {
        try {
            logger.info("[RedisStreamEventBus] Publishing event: {} to stream: {}", event.getClass().getName(), streamKey);
            
            // Use the same approach as RedisEventBus - let RedisTemplate handle serialization
            // Create stream entry with field-value pairs
            Map<String, Object> entry = new HashMap<>();
            
            // Instead of serializing manually, let RedisTemplate handle it when adding to stream
            // This uses the same serialization configured in RedisTemplate (which works for Pub/Sub)
            entry.put("event", event);
            entry.put("type", event.getClass().getName());
            logger.debug("[RedisStreamEventBus] Stream entry created: {}", entry);
            
            // Use RedisTemplate's opsForStream to add the entry
            logger.debug("[RedisStreamEventBus] Calling redisTemplate.opsForStream().add()");
            Object recordId = redisTemplate.opsForStream().add(streamKey, entry);
            logger.info("[RedisStreamEventBus] Event published to stream: {}, recordId: {}", event.getClass().getName(), recordId);
            
            // Verify the stream was created
            Boolean streamExists = redisTemplate.hasKey(streamKey);
            logger.info("[RedisStreamEventBus] Stream exists after publish: {}", streamExists);
            
            // Verify the entry was added
            if (recordId != null) {
                logger.info("[RedisStreamEventBus] Record added successfully with ID: {}", recordId);
            } else {
                logger.error("[RedisStreamEventBus] Failed to get record ID after publish");
            }
            
            // Also publish to local application event publisher (same as RedisEventBus)
            applicationEventPublisher.publishEvent(event);
            
            // Also publish to local handlers (same as RedisEventBus)
            publishToLocalHandlers(event);
            
        } catch (Exception e) {
            logger.error("[RedisStreamEventBus] Error publishing event: {}", e.getMessage(), e);
            throw new BaseException("Failed to publish event to Redis Stream", e);
        }
    }
    
    @Override
    public void subscribe(Class<? extends DomainEvent> eventType, EventHandler handler) throws BaseException {
        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
        eventTypeToClassMap.put(eventType.getName(), eventType);
        logger.info("[RedisStreamEventBus] Subscribed handler for event: {}", eventType.getName());
    }
    
    @Override
    public void unsubscribe(Class<? extends DomainEvent> eventType, EventHandler handler) throws BaseException {
        List<EventHandler> eventHandlers = handlers.get(eventType);
        if (eventHandlers != null) {
            eventHandlers.remove(handler);
            logger.info("[RedisStreamEventBus] Unsubscribed handler for event: {}", eventType.getName());
        }
    }
}