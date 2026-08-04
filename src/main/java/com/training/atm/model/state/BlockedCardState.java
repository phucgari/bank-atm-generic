package com.training.atm.model.state;

import com.training.atm.model.enums.CardStatus;

/** A blocked card: rejects all PIN attempts; can only be restored by admin. */
public class BlockedCardState implements CardState {

    @Override public boolean    canAcceptPin()  { return false; }
    @Override public boolean    isBlocked()     { return true; }
    @Override public CardStatus toStatus()      { return CardStatus.BLOCKED; }

    @Override
    public CardState onPinSuccess() {
        return this; // blocked — cannot succeed
    }

    @Override
    public CardState onPinFailure(int totalFailures, int maxAttempts) {
        return this; // already blocked — no further transition
    }

    @Override
    public CardState onAdminUnblock() {
        return new ActiveCardState();
    }
}
