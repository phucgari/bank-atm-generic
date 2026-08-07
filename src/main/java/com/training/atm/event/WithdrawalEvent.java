package com.training.atm.event;

import com.training.atm.model.Transaction;

public final class WithdrawalEvent extends DomainEvent {
    private final Transaction transaction;

    public WithdrawalEvent(Transaction transaction) {
        this.transaction = transaction;
    }

    public Transaction getTransaction() {
        return transaction;
    }
}
