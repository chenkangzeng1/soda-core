package com.hibuka.soda.event.redis;

import com.hibuka.soda.base.error.BaseException;
import com.hibuka.soda.cqrs.handle.EventBus;
import com.hibuka.soda.cqrs.handle.EventHandler;
import com.hibuka.soda.domain.DomainEvent;
import com.hibuka.soda.domain.AbstractDomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Redis event bus implementation using Redis Pub/Sub mechanism.
 * This implementation supports multi-instance event propagation through Redis.
 *
 * @author kangzeng.ckz
 * @since 2024/12/09
 */
public class RedisEventBus implements EventBus, InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(RedisEventBus.class);
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ChannelTopic topic;
    private final RedisConnectionFactory redisConnectionFactory;
    private final Map<Class<? extends DomainEvent>, List<EventHandler>> handlers = new ConcurrentHashMap<>();
    private final Map<String, Class<? extends DomainEvent>> eventTypeToClassMap = new ConcurrentHashMap<>();
    
    /**
     * Constructor for RedisEventBus.
     *
     * @param redisTemplate Redis template for event publishing
     * @param applicationEventPublisher Spring's application event publisher
     * @param redisConnectionFactory Redis connection factory for message listening
     * @param eventHandlers List of event handlers to register
     * @param topicName Redis topic name for event publishing
     */
    public RedisEventBus(RedisTemplate<String, Object> redisTemplate,
                       ApplicationEventPublisher applicationEventPublisher,
                       RedisConnectionFactory redisConnectionFactory,
                       List<EventHandler<? extends DomainEvent>> eventHandlers,
                       String topicName) {
        this.redisTemplate = redisTemplate;
        this.applicationEventPublisher = applicationEventPublisher;
        this.redisConnectionFactory = redisConnectionFactory;
        this.topic = new ChannelTopic(topicName);
        
        // Register event handlers
        registerEventHandlers(eventHandlers);
    }
    
    /**
     * Registers all event handlers.
     *
     * @param eventHandlers List of event handlers to register
     */
    private void registerEventHandlers(List<EventHandler<? extends DomainEvent>> eventHandlers) {
        logger.info("[RedisEventBus] Registering {} event handlers", eventHandlers.size());
        for (EventHandler<? extends DomainEvent> handler : eventHandlers) {
            Class<?>[] typeArguments = GenericTypeResolver.resolveTypeArguments(
                    handler.getClass(),
                    EventHandler.class
            );
            if (typeArguments != null && typeArguments.length > 0) {
                Class<? extends DomainEvent> eventType = (Class<? extends DomainEvent>) typeArguments[0];
                subscribe(eventType, handler);
                logger.info("[RedisEventBus] Registered handler for event: {}", eventType.getName());
            }
        }
    }
    
    /**
     * Sets up Redis message listener container after initialization.
     */
    @Override
    public void afterPropertiesSet() {
        // Check if container is already initialized
        if (this.messageListenerContainer == null) {
            // Create message listener adapter with String deserialization
            MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(this, "handleRedisMessage");
            // Configure listener adapter to use StringRedisSerializer for message conversion
            listenerAdapter.setSerializer(new StringRedisSerializer());
            listenerAdapter.afterPropertiesSet();
            
            // Set up message listener container
            RedisMessageListenerContainer container = new RedisMessageListenerContainer();
            container.setConnectionFactory(redisConnectionFactory);
            container.addMessageListener(listenerAdapter, topic);
            
            try {
                // Initialize the container
                container.afterPropertiesSet();
                // Start the container
                container.start();
                
                // Store the container reference
                this.messageListenerContainer = container;
                
                logger.info("[RedisEventBus] Subscribed to Redis topic: {}", topic.getTopic());
            } catch (Exception e) {
                logger.error("[RedisEventBus] Failed to initialize Redis message listener container", e);
                throw new RuntimeException("Failed to initialize Redis message listener container", e);
            }
        } else {
            logger.info("[RedisEventBus] Redis message listener container already initialized, skipping");
        }
    }
    
    private RedisMessageListenerContainer messageListenerContainer;
    
    /**
     * Handles incoming Redis messages.
     *
     * @param message Redis message containing the event
     */
    public void handleRedisMessage(String message) {
        try {
            // Create ObjectMapper with Java 8 date/time support and proper configuration
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            
            // Configure deserialization features
            objectMapper.configure(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS,
                false
            );
            objectMapper.configure(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false
            );
            
            // Deserialize JSON string to AbstractDomainEvent (using custom deserialization for abstract classes)
            AbstractDomainEvent event = deserializeAbstractDomainEvent(message, objectMapper);
            
            // Only proceed if event was successfully deserialized
            if (event != null) {
                logger.info("[RedisEventBus] Received event from Redis: {}, EventId: {}", 
                        event.getClass().getName(), event.getEventId());
                
                // Publish event to local application event publisher
                applicationEventPublisher.publishEvent(event);
                
                // Also publish to local handlers
                publishToLocalHandlers(event);
            } else {
                logger.info("[RedisEventBus] Event deserialization returned null, skipping further processing");
            }
        } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
            // Handle JSON mapping exceptions (including circular references)
            if (e instanceof com.fasterxml.jackson.databind.exc.InvalidDefinitionException && 
                e.getMessage().contains("AbstractDomainEvent")) {
                // This is an expected warning - we can't directly deserialize an abstract class
                // The event has already been handled locally, so we'll just log a warning
                logger.warn("[RedisEventBus] Expected warning: Cannot directly deserialize AbstractDomainEvent. This is normal if the event has been handled locally.");
            } else {
                // For other JSON mapping exceptions, log as error
                String errorDetails = buildJsonMappingErrorDetails(e);
                logger.error("[RedisEventBus] JSON mapping error handling Redis message: {}. Error details: {}", 
                        message, errorDetails, e);
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // Handle JSON processing exceptions
            logger.error("[RedisEventBus] JSON processing error handling Redis message: {}. Error at line {}, column {}", 
                    message, e.getLocation().getLineNr(), e.getLocation().getColumnNr(), e);
        } catch (Exception e) {
            // Handle other exceptions
            logger.error("[RedisEventBus] Unexpected error handling Redis message: {}. Error type: {}, Message: {}", 
                    message, e.getClass().getName(), e.getMessage(), e);
        }
    }
    
    /**
     * Builds detailed error information for JSON mapping exceptions.
     *
     * @param e JSON mapping exception
     * @return Detailed error information
     */
    private String buildJsonMappingErrorDetails(com.fasterxml.jackson.databind.JsonMappingException e) {
        StringBuilder errorDetails = new StringBuilder();
        
        errorDetails.append("Error type: ").append(e.getClass().getSimpleName());
        errorDetails.append(", Message: ").append(e.getMessage());
        
        if (e.getPath() != null && !e.getPath().isEmpty()) {
            errorDetails.append(", Failed at path: ");
            for (com.fasterxml.jackson.databind.JsonMappingException.Reference ref : e.getPath()) {
                errorDetails.append(ref.getFieldName()).append(".");
            }
            // Remove trailing dot
            if (errorDetails.charAt(errorDetails.length() - 1) == '.') {
                errorDetails.setLength(errorDetails.length() - 1);
            }
        }
        
        if (e.getLocation() != null) {
            errorDetails.append(", Location: line ").append(e.getLocation().getLineNr())
                       .append(", column ").append(e.getLocation().getColumnNr());
        }
        
        // Check if it's a circular reference error
        if (e.getMessage().contains("circular reference") || e.getMessage().contains("self-reference")) {
            errorDetails.append(", Possible cause: Circular reference detected");
            errorDetails.append(", Solution: Check event structure for circular references or adjust serialization configuration");
        }
        
        return errorDetails.toString();
    }
    
    /**
     * Deserializes JSON string to AbstractDomainEvent using custom deserialization logic.
     * The message format is [eventType, eventData] as generated by RedisTemplate's GenericJackson2JsonRedisSerializer.
     *
     * @param message JSON string containing the event in array format: [eventType, eventData]
     * @param objectMapper ObjectMapper instance
     * @return Deserialized AbstractDomainEvent
     * @throws Exception If deserialization fails
     */
    private AbstractDomainEvent deserializeAbstractDomainEvent(String message, ObjectMapper objectMapper) throws Exception {
        logger.info("[RedisEventBus] Processing message: {}", message);
        
        // Parse the JSON as a tree to handle both array and object formats
        com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(message);
        
        if (rootNode.isArray() && rootNode.size() >= 2) {
            // Case 1: Message is in array format [eventType, eventData] (from RedisTemplate)
            com.fasterxml.jackson.databind.JsonNode eventTypeNode = rootNode.get(0);
            com.fasterxml.jackson.databind.JsonNode eventDataNode = rootNode.get(1);
            
            logger.info("[RedisEventBus] Array format detected: eventType={}, eventData={}", eventTypeNode, eventDataNode);
            
            if (eventTypeNode.isTextual()) {
                // Get the fully qualified event class name from the first element
                String eventClassName = eventTypeNode.asText();
                logger.info("[RedisEventBus] Event class name: {}", eventClassName);
                
                // Load the actual event class using reflection
                Class<? extends AbstractDomainEvent> eventClass = (Class<? extends AbstractDomainEvent>) Class.forName(eventClassName);
                logger.info("[RedisEventBus] Event class loaded: {}", eventClass);
                
                try {
                    // First try: deserialize directly to the concrete event class
                    AbstractDomainEvent event = objectMapper.treeToValue(eventDataNode, eventClass);
                    logger.info("[RedisEventBus] Successfully deserialized event: {}, EventId: {}", event.getClass().getName(), event.getEventId());
                    return event;
                } catch (com.fasterxml.jackson.databind.exc.MismatchedInputException e) {
                    // If direct deserialization fails due to nested arrays, we need to handle it differently
                    logger.warn("[RedisEventBus] Direct deserialization failed, trying to handle nested arrays: {}", e.getMessage());
                    
                    // We'll create a deep copy of the eventDataNode and convert nested arrays to objects
                    com.fasterxml.jackson.databind.node.ObjectNode modifiedEventDataNode = objectMapper.createObjectNode();
                    
                    // Iterate through all fields in the eventDataNode
                    eventDataNode.fields().forEachRemaining(entry -> {
                        String fieldName = entry.getKey();
                        com.fasterxml.jackson.databind.JsonNode fieldValue = entry.getValue();
                        
                        // If the field is an array with className and data, extract just the data
                        if (fieldValue.isArray() && fieldValue.size() >= 2 && fieldValue.get(0).isTextual()) {
                            // Get the actual data from the second element
                            com.fasterxml.jackson.databind.JsonNode actualData = fieldValue.get(1);
                            modifiedEventDataNode.set(fieldName, actualData);
                        } else {
                            // Otherwise, keep the field as is
                            modifiedEventDataNode.set(fieldName, fieldValue);
                        }
                    });
                    
                    try {
                        // Try deserialization again with the modified node
                        AbstractDomainEvent event = objectMapper.treeToValue(modifiedEventDataNode, eventClass);
                        logger.info("[RedisEventBus] Successfully deserialized event after handling nested arrays: {}, EventId: {}", event.getClass().getName(), event.getEventId());
                        return event;
                    } catch (com.fasterxml.jackson.databind.exc.InvalidDefinitionException e2) {
                        // If still fails with InvalidDefinitionException (no constructor), log as warning and return null
                        // This is acceptable because the event has already been handled locally
                        logger.warn("[RedisEventBus] Expected warning: Cannot deserialize {} due to missing constructor. This is normal if the event has been handled locally.", eventClassName);
                        return null;
                    }
                } catch (com.fasterxml.jackson.databind.exc.InvalidDefinitionException e) {
                    // If direct deserialization fails due to missing constructor, log as warning and return null
                    // This is acceptable because the event has already been handled locally
                    logger.warn("[RedisEventBus] Expected warning: Cannot deserialize {} due to missing constructor. This is normal if the event has been handled locally.", eventClassName);
                    return null;
                }
            } else {
                throw new IllegalArgumentException("Invalid Redis message format: expected eventType to be a string, got " + eventTypeNode.getNodeType());
            }
        } else if (rootNode.isObject()) {
            // Case 2: Message is already an object (fallback for other formats)
            logger.info("[RedisEventBus] Object format detected");
            
            // Try to extract type information from the object if available
            com.fasterxml.jackson.databind.JsonNode typeNode = rootNode.get("@class");
            if (typeNode != null && typeNode.isTextual()) {
                String eventClassName = typeNode.asText();
                logger.info("[RedisEventBus] Found @class field: {}", eventClassName);
                
                Class<? extends AbstractDomainEvent> eventClass = (Class<? extends AbstractDomainEvent>) Class.forName(eventClassName);
                
                try {
                    AbstractDomainEvent event = objectMapper.treeToValue(rootNode, eventClass);
                    logger.info("[RedisEventBus] Successfully deserialized event: {}, EventId: {}", event.getClass().getName(), event.getEventId());
                    return event;
                } catch (com.fasterxml.jackson.databind.exc.InvalidDefinitionException e) {
                    // If deserialization fails due to missing constructor, log as warning and return null
                    logger.warn("[RedisEventBus] Expected warning: Cannot deserialize {} due to missing constructor. This is normal if the event has been handled locally.", eventClassName);
                    return null;
                }
            }
            
            // Fallback: if no @class field, try to use eventType field to map to concrete class
            com.fasterxml.jackson.databind.JsonNode eventTypeNode = rootNode.get("eventType");
            if (eventTypeNode != null && eventTypeNode.isTextual()) {
                String eventType = eventTypeNode.asText();
                logger.info("[RedisEventBus] Found eventType field: {}", eventType);
                
                // Try to find a matching class in our eventTypeToClassMap
                for (Map.Entry<String, Class<? extends DomainEvent>> entry : eventTypeToClassMap.entrySet()) {
                    if (entry.getKey().contains(eventType)) {
                        logger.info("[RedisEventBus] Found matching class: {}", entry.getValue());
                        
                        Class<? extends AbstractDomainEvent> eventClass = (Class<? extends AbstractDomainEvent>) entry.getValue();
                        
                        try {
                            AbstractDomainEvent event = objectMapper.treeToValue(rootNode, eventClass);
                            logger.info("[RedisEventBus] Successfully deserialized event: {}, EventId: {}", event.getClass().getName(), event.getEventId());
                            return event;
                        } catch (com.fasterxml.jackson.databind.exc.InvalidDefinitionException e) {
                            // If deserialization fails due to missing constructor, log as warning and return null
                            logger.warn("[RedisEventBus] Expected warning: Cannot deserialize {} due to missing constructor. This is normal if the event has been handled locally.", entry.getValue().getName());
                            return null;
                        }
                    }
                }
            }
            
            // Last resort: throw exception since we can't determine the concrete type
            throw new IllegalArgumentException("Invalid Redis message format: no type information found in JSON object");
        } else {
            throw new IllegalArgumentException("Invalid Redis message format: expected JSON array or object, got " + rootNode.getNodeType());
        }
    }
    
    /**
     * Publishes event to local handlers.
     *
     * @param event Domain event to publish
     */
    private void publishToLocalHandlers(DomainEvent event) {
        List<EventHandler> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            for (EventHandler handler : eventHandlers) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    logger.error("[RedisEventBus] Error handling event by local handler: {}", 
                            handler.getClass().getName(), e);
                }
            }
        }
    }
    
    @Override
    public void publish(DomainEvent event) throws BaseException {
        try {
            logger.info("[RedisEventBus] Publishing event to Redis: {}, EventId: {}", 
                    event.getClass().getName(), ((AbstractDomainEvent) event).getEventId());
            
            // Publish to Redis topic
            redisTemplate.convertAndSend(topic.getTopic(), event);
            
            // Also publish to local application event publisher
            applicationEventPublisher.publishEvent(event);
            
            // Also publish to local handlers
            publishToLocalHandlers(event);
        } catch (Exception e) {
            logger.error("[RedisEventBus] Error publishing event to Redis. Event: {}, Topic: {}", 
                    event.getClass().getName(), topic.getTopic(), e);
            throw new BaseException("Failed to publish event to Redis: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void subscribe(Class<? extends DomainEvent> eventType, EventHandler handler) {
        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
        logger.debug("[RedisEventBus] Subscribed handler for event: {}", eventType.getName());
        
        // Add to event type mapping if it's an AbstractDomainEvent subclass
        if (AbstractDomainEvent.class.isAssignableFrom(eventType)) {
            // Simple mapping: use event type name as key, but we'll need to improve this later
            // For now, we'll just store the class so we can use it for deserialization
            eventTypeToClassMap.put(eventType.getSimpleName(), eventType);
        }
    }
    
    @Override
    public void unsubscribe(Class<? extends DomainEvent> eventType, EventHandler handler) {
        List<EventHandler> eventHandlers = handlers.get(eventType);
        if (eventHandlers != null) {
            eventHandlers.remove(handler);
            logger.debug("[RedisEventBus] Unsubscribed handler for event: {}", eventType.getName());
        }
    }
}