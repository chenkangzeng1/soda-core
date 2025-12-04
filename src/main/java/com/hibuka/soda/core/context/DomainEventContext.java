package com.hibuka.soda.core.context;

import com.hibuka.soda.domain.AbstractDomainEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class DomainEventContext {
    private static final ThreadLocal<List<AbstractDomainEvent>> holder = ThreadLocal.withInitial(ArrayList::new);

    public static void add(AbstractDomainEvent event) {
        if (event != null) {
            holder.get().add(event);
        }
    }

    public static void addAll(Collection<AbstractDomainEvent> events) {
        if (events != null && !events.isEmpty()) {
            holder.get().addAll(events);
        }
    }

    public static List<AbstractDomainEvent> getEvents() {
        return new ArrayList<>(holder.get());
    }

    public static void clear() {
        holder.remove();
    }
}
