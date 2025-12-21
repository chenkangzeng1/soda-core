package com.hibuka.soda.context;

import com.hibuka.soda.domain.aggregate.AbstractAggregateRoot;
import com.hibuka.soda.domain.event.AbstractDomainEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ContextPropagationTest {

    @Test
    public void testContextPropagation() {
        // 1. Setup Context (Simulate AOP)
        String expectedRequestId = "req-12345";
        String expectedUserName = "test-user";
        String expectedCallerUid = "888888";
        
        System.out.println("Step 1: Setting Context");
        DomainEventContext.setRequestId(expectedRequestId);
        DomainEventContext.setUserName(expectedUserName);
        DomainEventContext.setCallerUid(expectedCallerUid);
        
        try {
            // 2. Execute Aggregate Logic
            System.out.println("Step 2: Executing Aggregate Logic");
            TestAggregate aggregate = new TestAggregate();
            aggregate.doSomething();
            
            // 3. Verify Event
            System.out.println("Step 3: Verifying Event");
            Assertions.assertFalse(aggregate.getDomainEvents().isEmpty(), "Events should not be empty");
            
            TestEvent event = (TestEvent) aggregate.getDomainEvents().iterator().next();
            Assertions.assertEquals(expectedRequestId, event.getRequestId(), "RequestId should match");
            Assertions.assertEquals(expectedUserName, event.getUserName(), "UserName should match");
            Assertions.assertEquals(expectedCallerUid, event.getCallerUid(), "CallerUid should match");
            
            System.out.println("Verification Passed!");
            
        } finally {
            // 4. Cleanup
            DomainEventContext.clear();
        }
    }

    // --- Mock Classes ---
    
    public static class TestAggregate extends AbstractAggregateRoot {
        public void doSomething() {
            addPendingEvent(new TestEvent());
        }
    }
    
    public static class TestEvent extends AbstractDomainEvent {
        // No extra fields needed
    }
}
