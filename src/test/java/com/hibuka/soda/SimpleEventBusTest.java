package com.hibuka.soda;

import com.hibuka.soda.core.SimpleEventBus;
import com.hibuka.soda.cqrs.handle.EventHandler;
import com.hibuka.soda.domain.DomainEvent;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class SimpleEventBusTest {
    @Test
    void testPublishNullThrowsException() {
        SimpleEventBus bus = new SimpleEventBus(new ArrayList<EventHandler<? extends DomainEvent>>());
        assertThrows(NullPointerException.class, () -> bus.publish(null));
    }
} 