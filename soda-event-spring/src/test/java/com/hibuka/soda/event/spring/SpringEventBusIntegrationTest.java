package com.hibuka.soda.event.spring;

import com.hibuka.soda.cqrs.event.EventHandler;
import com.hibuka.soda.domain.event.AbstractDomainEvent;
import com.hibuka.soda.domain.event.DomainEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for SpringEventBus.
 * Tests the functionality of SpringEventBus in a Spring Boot application context.
 */
@SpringBootTest(classes = SpringEventBusIntegrationTest.TestApplication.class)
public class SpringEventBusIntegrationTest {

    @Autowired
    private SpringEventBus springEventBus;

    private TestEventHandler testEventHandler;
    private TestDomainEvent testDomainEvent;

    @BeforeEach
    void setUp() {
        testEventHandler = new TestEventHandler();
        testDomainEvent = new TestDomainEvent("test-event-id", "test-data");
    }

    @AfterEach
    void tearDown() {
        testEventHandler.clearEvents();
    }

    /**
     * Test that SpringEventBus can publish and subscribe to domain events.
     */
    @Test
    void testPublishSubscribe() throws Exception {
        // Subscribe to the event
        springEventBus.subscribe(TestDomainEvent.class, testEventHandler);

        // Publish the event
        springEventBus.publish(testDomainEvent);

        // Verify the event was handled
        Thread.sleep(100); // Give time for event processing
        assertEquals(1, testEventHandler.getEvents().size());
        assertEquals(testDomainEvent, testEventHandler.getEvents().get(0));

        // Unsubscribe from the event
        springEventBus.unsubscribe(TestDomainEvent.class, testEventHandler);

        // Publish another event and verify it's not handled
        springEventBus.publish(new TestDomainEvent("test-event-id-2", "test-data-2"));
        Thread.sleep(100);
        assertEquals(1, testEventHandler.getEvents().size());
    }

    /**
     * Test that SpringEventBus can handle multiple subscribers for the same event type.
     */
    @Test
    void testMultipleSubscribers() throws Exception {
        // Create multiple event handlers
        TestEventHandler handler1 = new TestEventHandler();
        TestEventHandler handler2 = new TestEventHandler();

        // Subscribe both handlers to the event
        springEventBus.subscribe(TestDomainEvent.class, handler1);
        springEventBus.subscribe(TestDomainEvent.class, handler2);

        // Publish the event
        springEventBus.publish(testDomainEvent);

        // Verify both handlers received the event
        Thread.sleep(100);
        assertEquals(1, handler1.getEvents().size());
        assertEquals(1, handler2.getEvents().size());
        assertEquals(testDomainEvent, handler1.getEvents().get(0));
        assertEquals(testDomainEvent, handler2.getEvents().get(0));
    }

    /**
     * Test that SpringEventBus can handle asynchronous event processing.
     */
    @Test
    void testAsyncEventProcessing() throws Exception {
        // Create a latch to wait for event processing
        CountDownLatch latch = new CountDownLatch(1);

        // Create a handler that counts down the latch
        EventHandler<TestDomainEvent> asyncHandler = event -> {
            try {
                // Simulate asynchronous processing
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch.countDown();
        };

        // Subscribe to the event
        springEventBus.subscribe(TestDomainEvent.class, asyncHandler);

        // Publish the event
        springEventBus.publish(testDomainEvent);

        // Wait for the event to be processed
        boolean processed = latch.await(1, TimeUnit.SECONDS);
        assertTrue(processed, "Event should be processed within 1 second");
    }

    /**
     * Test application class for Spring Boot test.
     */
    @SpringBootApplication
    @ComponentScan(basePackages = "com.hibuka.soda")
    @Import(SpringEventBusAutoConfiguration.class)
    static class TestApplication {
        // This class is used to configure the Spring Boot test application context
    }

    /**
     * Test domain event class.
     */
    static class TestDomainEvent extends AbstractDomainEvent {
        private String data;

        public TestDomainEvent(String eventId, String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }

        @Override
        public String getEventType() {
            return "TEST_EVENT";
        }
    }

    /**
     * Test event handler class that collects received events.
     */
    static class TestEventHandler implements EventHandler<TestDomainEvent> {
        private final List<TestDomainEvent> events = new ArrayList<>();

        @Override
        public void handle(TestDomainEvent event) {
            events.add(event);
        }

        public List<TestDomainEvent> getEvents() {
            return events;
        }

        public void clearEvents() {
            events.clear();
        }
    }
}
