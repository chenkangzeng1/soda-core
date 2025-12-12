package com.hibuka.soda.event.redis.service;

import java.util.Map;

/**
 * Interface for idempotency service.
 * This service provides methods to manage event processing status for idempotent handling.
 *
 * @author kangzeng.ckz
 * @since 2025/12/12
 */
public interface IdempotencyService {
    /**
     * Event processing status.
     */
    enum ProcessingStatus {
        PROCESSING, SUCCESS, FAILED
    }
    
    /**
     * Begins processing an event.
     * This method checks if the event can be processed and updates the status to PROCESSING if it can.
     *
     * @param eventId Unique event identifier
     * @return true if the event can be processed, false otherwise
     */
    boolean beginProcessing(String eventId);
    
    /**
     * Marks an event as successfully processed.
     *
     * @param eventId Unique event identifier
     * @param handlerResults Results from event handlers
     */
    void markAsSuccess(String eventId, Map<String, Object> handlerResults);
    
    /**
     * Marks an event as failed to process.
     *
     * @param eventId Unique event identifier
     * @param error Error information
     */
    void markAsFailed(String eventId, String error);
    
    /**
     * Gets the processing status of an event.
     *
     * @param eventId Unique event identifier
     * @return Processing status, null if not found
     */
    ProcessingStatus getStatus(String eventId);
    
    /**
     * Cleans up expired idempotency status records.
     */
    void cleanupExpiredStatus();
}