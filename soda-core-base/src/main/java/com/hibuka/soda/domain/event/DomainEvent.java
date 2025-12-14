package com.hibuka.soda.domain.event;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * DomainEvent description
 *
 * @author kangzeng.ckz
 * @since 2024/10/25
 **/
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface DomainEvent {
    /**
     * Get event ID
     * @return the event ID
     */
    String getEventId();

    /**
     * Get event occurrence time
     * @return the time when the event occurred
     */
    LocalDateTime getOccurredOn();

    /**
     * Get event type
     * @return the event type
     */
    String getEventType();
} 