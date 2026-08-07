package com.training.atm.event;

import com.training.atm.model.Transaction;

public final class TransferEvent extends DomainEvent {
    private final Transaction transaction;

    public TransferEvent(Transaction transaction) {
        this.transaction = transaction;
    }

    public Transaction getTransaction() {
        return transaction;
    }
}
