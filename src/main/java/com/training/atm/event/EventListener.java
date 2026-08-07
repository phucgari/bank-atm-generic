package com.training.atm.event;

@FunctionalInterface
public interface EventListener<E extends DomainEvent> {
    void onEvent(E event);
}
