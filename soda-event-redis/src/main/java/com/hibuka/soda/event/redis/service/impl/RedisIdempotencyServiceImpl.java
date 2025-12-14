package com.hibuka.soda.event.redis.service.impl;

import com.hibuka.soda.bus.configuration.EventProperties;
import com.hibuka.soda.event.redis.service.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis implementation of IdempotencyService.
 * Uses Redis Hash structure to store event processing status.
 *
 * @author kangzeng.ckz
 * @since 2025/12/12
 */
public class RedisIdempotencyServiceImpl implements IdempotencyService {
    private static final Logger logger = LoggerFactory.getLogger(RedisIdempotencyServiceImpl.class);
    
    private static final String STATUS_FIELD = "status";
    private static final String PROCESSED_AT_FIELD = "processedAt";
    private static final String HANDLER_RESULTS_FIELD = "handlerResults";
    private static final String ERROR_FIELD = "error";
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final HashOperations<String, String, Object> hashOps;
    private final EventProperties.RedisProperties.StreamProperties.IdempotencyProperties idempotencyProperties;
    
    /**
     * Constructor for RedisIdempotencyServiceImpl.
     *
     * @param redisTemplate Redis template for operations
     * @param idempotencyProperties Idempotency configuration properties
     */
    public RedisIdempotencyServiceImpl(
            RedisTemplate<String, Object> redisTemplate,
            EventProperties.RedisProperties.StreamProperties.IdempotencyProperties idempotencyProperties) {
        this.redisTemplate = redisTemplate;
        this.hashOps = redisTemplate.opsForHash();
        this.idempotencyProperties = idempotencyProperties;
    }
    
    /**
     * Generates the Redis key for idempotency status.
     *
     * @param eventId Unique event identifier
     * @return Redis key
     */
    private String generateKey(String eventId) {
        return idempotencyProperties.getRedisKeyPrefix() + ":" + eventId;
    }
    
    @Override
    public boolean beginProcessing(String eventId) {
        if (!StringUtils.hasText(eventId)) {
            logger.warn("[RedisIdempotencyServiceImpl] Event ID is empty, cannot begin processing");
            return false;
        }
        
        String key = generateKey(eventId);
        
        try {
            // Check current status
            Object statusObj = hashOps.get(key, STATUS_FIELD);
            if (statusObj != null) {
                ProcessingStatus status = ProcessingStatus.valueOf(statusObj.toString());
                if (status == ProcessingStatus.SUCCESS) {
                    logger.info("[RedisIdempotencyServiceImpl] Event already processed successfully: {}", eventId);
                    return false;
                } else if (status == ProcessingStatus.PROCESSING) {
                    logger.info("[RedisIdempotencyServiceImpl] Event already processing: {}", eventId);
                    return false;
                }
                // If FAILED, we can retry
            }
            
            // Set status to PROCESSING with expiration - use smaller initial capacity
            Map<String, Object> statusMap = new HashMap<>(2);
            statusMap.put(STATUS_FIELD, ProcessingStatus.PROCESSING.name());
            statusMap.put(PROCESSED_AT_FIELD, String.valueOf(System.currentTimeMillis()));
            
            // Use hashOps.putAll for batch operation instead of manual transaction
            hashOps.putAll(key, statusMap);
            // Set expiration separately - this is acceptable since status is already PROCESSING
            redisTemplate.expire(key, idempotencyProperties.getExpireTime(), TimeUnit.SECONDS);
            
            logger.debug("[RedisIdempotencyServiceImpl] Begin processing event: {}", eventId);
            return true;
        } catch (Exception e) {
            logger.error("[RedisIdempotencyServiceImpl] Error beginning processing for event {}: {}", 
                    eventId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public void markAsSuccess(String eventId, Map<String, Object> handlerResults) {
        if (!StringUtils.hasText(eventId)) {
            logger.warn("[RedisIdempotencyServiceImpl] Event ID is empty, cannot mark as success");
            return;
        }
        
        String key = generateKey(eventId);
        
        try {
            // Use smaller initial capacity based on actual needs
            int capacity = handlerResults != null && !handlerResults.isEmpty() ? 3 : 2;
            Map<String, Object> statusMap = new HashMap<>(capacity);
            statusMap.put(STATUS_FIELD, ProcessingStatus.SUCCESS.name());
            statusMap.put(PROCESSED_AT_FIELD, String.valueOf(System.currentTimeMillis()));
            if (handlerResults != null && !handlerResults.isEmpty()) {
                statusMap.put(HANDLER_RESULTS_FIELD, handlerResults.toString());
            }
            
            hashOps.putAll(key, statusMap);
            logger.debug("[RedisIdempotencyServiceImpl] Marked event as success: {}", eventId);
        } catch (Exception e) {
            logger.error("[RedisIdempotencyServiceImpl] Error marking event as success {}: {}", 
                    eventId, e.getMessage(), e);
        }
    }
    
    @Override
    public void markAsFailed(String eventId, String error) {
        if (!StringUtils.hasText(eventId)) {
            logger.warn("[RedisIdempotencyServiceImpl] Event ID is empty, cannot mark as failed");
            return;
        }
        
        String key = generateKey(eventId);
        
        try {
            // Use smaller initial capacity based on actual needs
            int capacity = StringUtils.hasText(error) ? 3 : 2;
            Map<String, Object> statusMap = new HashMap<>(capacity);
            statusMap.put(STATUS_FIELD, ProcessingStatus.FAILED.name());
            statusMap.put(PROCESSED_AT_FIELD, String.valueOf(System.currentTimeMillis()));
            if (StringUtils.hasText(error)) {
                statusMap.put(ERROR_FIELD, error);
            }
            
            hashOps.putAll(key, statusMap);
            logger.debug("[RedisIdempotencyServiceImpl] Marked event as failed: {}, error: {}", eventId, error);
        } catch (Exception e) {
            logger.error("[RedisIdempotencyServiceImpl] Error marking event as failed {}: {}", 
                    eventId, e.getMessage(), e);
        }
    }
    
    @Override
    public ProcessingStatus getStatus(String eventId) {
        if (!StringUtils.hasText(eventId)) {
            logger.warn("[RedisIdempotencyServiceImpl] Event ID is empty, cannot get status");
            return null;
        }
        
        String key = generateKey(eventId);
        
        try {
            Object statusObj = hashOps.get(key, STATUS_FIELD);
            if (statusObj == null) {
                return null;
            }
            return ProcessingStatus.valueOf(statusObj.toString());
        } catch (Exception e) {
            logger.error("[RedisIdempotencyServiceImpl] Error getting status for event {}: {}", 
                    eventId, e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public void cleanupExpiredStatus() {
        try {
            String pattern = idempotencyProperties.getRedisKeyPrefix() + ":*";
            
            // Use SCAN instead of KEYS to avoid blocking Redis when there are many keys
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
                
                org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.scan(options);
                
                int deletedCount = 0;
                Set<String> batchKeys = new java.util.HashSet<>(100);
                
                while (cursor.hasNext()) {
                    byte[] keyBytes = cursor.next();
                    String key = redisTemplate.getStringSerializer().deserialize(keyBytes);
                    if (key != null) {
                        batchKeys.add(key);
                        
                        // Delete in batches to avoid blocking Redis with large delete operations
                        if (batchKeys.size() >= 100) {
                            redisTemplate.delete(batchKeys);
                            deletedCount += batchKeys.size();
                            batchKeys.clear();
                        }
                    }
                }
                
                // Delete remaining keys
                if (!batchKeys.isEmpty()) {
                    redisTemplate.delete(batchKeys);
                    deletedCount += batchKeys.size();
                }
                
                if (deletedCount > 0) {
                    logger.info("[RedisIdempotencyServiceImpl] Cleaned up {} idempotency status records", deletedCount);
                }
                
                return null;
            });
        } catch (Exception e) {
            logger.error("[RedisIdempotencyServiceImpl] Error cleaning up expired status: {}", 
                    e.getMessage(), e);
        }
    }
}