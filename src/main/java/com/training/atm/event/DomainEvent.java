package com.training.atm.event;

import java.time.Instant;

public abstract class DomainEvent {
    private final Instant occurredAt;

    protected DomainEvent() {
        this(Instant.now());
    }

    protected DomainEvent(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
