package com.training.atm.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventBus {
    private final Map<Class<? extends DomainEvent>,
            List<EventListener<? extends DomainEvent>>> listeners = new HashMap<>();

    public <E extends DomainEvent> void subscribe(Class<E> eventType, EventListener<E> listener) {
        listeners.computeIfAbsent(eventType, ignored -> new ArrayList<>()).add(listener);
    }

    @SuppressWarnings("unchecked")
    public <E extends DomainEvent> void publish(E event) {
        List<EventListener<? extends DomainEvent>> registered = listeners.get(event.getClass());
        if (registered == null) {
            return;
        }

        for (EventListener<? extends DomainEvent> listener : List.copyOf(registered)) {
            ((EventListener<E>) listener).onEvent(event);
        }
    }
}
