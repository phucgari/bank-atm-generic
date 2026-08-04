package com.training.atm.model.state;

import com.training.atm.model.enums.CardStatus;

/**
 * State interface for the ATM card lifecycle.
 *
 * Each concrete state knows which transitions are legal and what the next
 * state should be.  {@link com.training.atm.model.ATMCard} delegates all
 * status-dependent behaviour here, eliminating {@code if (status == BLOCKED)}
 * conditionals from {@link com.training.atm.service.CardScanner} and
 * {@link com.training.atm.session.AdminSession}.
 */
public interface CardState {
    /** Whether the card can currently accept a PIN entry. */
    boolean canAcceptPin();

    /** Transition fired when the correct PIN is entered. */
    CardState onPinSuccess();

    /**
     * Transition fired on a wrong PIN.
     *
     * @param totalFailures cumulative failed attempts (already incremented)
     * @param maxAttempts   limit before the card is blocked
     */
    CardState onPinFailure(int totalFailures, int maxAttempts);

    /** Transition fired when an administrator unblocks the card. */
    CardState onAdminUnblock();

    /** Whether this state represents a blocked card. */
    boolean isBlocked();

    /** Serialisation hook — maps the state back to the stored {@link CardStatus}. */
    CardStatus toStatus();
}
