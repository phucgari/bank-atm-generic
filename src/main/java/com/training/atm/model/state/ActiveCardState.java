package com.training.atm.model.state;

import com.training.atm.model.enums.CardStatus;

/** An active card: accepts PIN attempts; blocks itself after too many failures. */
public class ActiveCardState implements CardState {

    @Override public boolean   canAcceptPin()  { return true; }
    @Override public boolean   isBlocked()     { return false; }
    @Override public CardStatus toStatus()     { return CardStatus.ACTIVE; }

    @Override
    public CardState onPinSuccess() {
        return this; // stays ACTIVE
    }

    @Override
    public CardState onPinFailure(int totalFailures, int maxAttempts) {
        return totalFailures >= maxAttempts ? new BlockedCardState() : this;
    }

    @Override
    public CardState onAdminUnblock() {
        return this; // already active — no transition needed
    }
}
