package com.hibuka.soda;

import com.hibuka.soda.bus.impl.SimpleEventBus;
import com.hibuka.soda.cqrs.event.EventHandler;
import com.hibuka.soda.domain.event.DomainEvent;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SimpleEventBusTest {
    
    // Test event classes
    static class TestEvent implements DomainEvent {
        private final String eventId;
        private final String message;
        private final LocalDateTime occurredOn;
        
        TestEvent(String message) {
            this.eventId = java.util.UUID.randomUUID().toString();
            this.message = message;
            this.occurredOn = LocalDateTime.now();
        }
        
        public String getMessage() {
            return message;
        }
        
        @Override
        public String getEventId() {
            return eventId;
        }
        
        @Override
        public String getEventType() {
            return getClass().getName();
        }
        
        @Override
        public LocalDateTime getOccurredOn() {
            return occurredOn;
        }
    }
    
    static class TestEvent2 extends TestEvent {
        TestEvent2(String message) {
            super(message);
        }
    }
    
    // Test event handler
    static class TestEventHandler implements EventHandler<TestEvent> {
        private final List<TestEvent> handledEvents = new ArrayList<>();
        
        @Override
        public void handle(TestEvent event) {
            handledEvents.add(event);
        }
        
        public List<TestEvent> getHandledEvents() {
            return handledEvents;
        }
    }
    
    @Test
    void testPublishNullThrowsException() {
        SimpleEventBus bus = new SimpleEventBus(new ArrayList<EventHandler<? extends DomainEvent>>());
        assertThrows(NullPointerException.class, () -> bus.publish(null));
    }
    
    @Test
    void testPublishEventToHandler() {
        // Arrange
        TestEventHandler handler = new TestEventHandler();
        List<EventHandler<? extends DomainEvent>> handlers = List.of(handler);
        SimpleEventBus bus = new SimpleEventBus(handlers);
        TestEvent event = new TestEvent("test-message");
        
        // Act
        bus.publish(event);
        
        // Assert
        List<TestEvent> handledEvents = handler.getHandledEvents();
        assertEquals(1, handledEvents.size());
        assertEquals(event, handledEvents.get(0));
        assertEquals("test-message", handledEvents.get(0).getMessage());
    }
    
    @Test
    void testPublishEventToMultipleHandlers() {
        // Arrange
        TestEventHandler handler1 = new TestEventHandler();
        TestEventHandler handler2 = new TestEventHandler();
        List<EventHandler<? extends DomainEvent>> handlers = List.of(handler1, handler2);
        SimpleEventBus bus = new SimpleEventBus(handlers);
        TestEvent event = new TestEvent("test-message");
        
        // Act
        bus.publish(event);
        
        // Assert
        assertEquals(1, handler1.getHandledEvents().size());
        assertEquals(1, handler2.getHandledEvents().size());
        assertEquals(event, handler1.getHandledEvents().get(0));
        assertEquals(event, handler2.getHandledEvents().get(0));
    }
    
    @Test
    void testPublishSubclassEventToSuperclassHandler() {
        // Arrange
        TestEventHandler handler = new TestEventHandler();
        List<EventHandler<? extends DomainEvent>> handlers = List.of(handler);
        SimpleEventBus bus = new SimpleEventBus(handlers);
        TestEvent2 event = new TestEvent2("subclass-message");
        
        // Act
        bus.publish(event);
        
        // Assert
        List<TestEvent> handledEvents = handler.getHandledEvents();
        assertEquals(1, handledEvents.size());
        assertEquals(event, handledEvents.get(0));
        assertEquals("subclass-message", handledEvents.get(0).getMessage());
        assertTrue(handledEvents.get(0) instanceof TestEvent2);
    }
    
    @Test
    void testPublishEventWithNoHandlers() {
        // Arrange
        List<EventHandler<? extends DomainEvent>> handlers = new ArrayList<>();
        SimpleEventBus bus = new SimpleEventBus(handlers);
        TestEvent event = new TestEvent("test-message");
        
        // Act - should not throw exception
        bus.publish(event);
        
        // Assert - no exception thrown
        assertTrue(true);
    }
    
    @Test
    void testPublishMultipleEvents() {
        // Arrange
        TestEventHandler handler = new TestEventHandler();
        List<EventHandler<? extends DomainEvent>> handlers = List.of(handler);
        SimpleEventBus bus = new SimpleEventBus(handlers);
        
        // Act
        bus.publish(new TestEvent("message-1"));
        bus.publish(new TestEvent("message-2"));
        bus.publish(new TestEvent("message-3"));
        
        // Assert
        List<TestEvent> handledEvents = handler.getHandledEvents();
        assertEquals(3, handledEvents.size());
        assertEquals("message-1", handledEvents.get(0).getMessage());
        assertEquals("message-2", handledEvents.get(1).getMessage());
        assertEquals("message-3", handledEvents.get(2).getMessage());
    }
}