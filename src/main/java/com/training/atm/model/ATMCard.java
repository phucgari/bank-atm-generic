package com.training.atm.model;

import com.training.atm.model.enums.CardStatus;
import com.training.atm.model.state.ActiveCardState;
import com.training.atm.model.state.CardState;

/**
 * ATM card model.
 *
 * <h3>State pattern</h3>
 * Status-dependent behaviour is delegated to a {@link CardState} object.
 * Adding a new card state (e.g. {@code FROZEN}) requires only a new
 * {@code CardState} implementation — no conditionals in
 * {@link com.training.atm.service.CardScanner} or
 * {@link com.training.atm.session.AdminSession} need to change.
 *
 * <p>The {@link CardStatus} enum is retained for file serialisation only;
 * runtime behaviour is entirely determined by the state object.
 */
public class ATMCard implements Identifiable<String> {
    private String    cardId;
    private String    pin;
    private String    accountNumber;
    private CardState cardState;
    private int       failedAttempts;

    public ATMCard(String cardId, String pin, String accountNumber,
                   CardState cardState, int failedAttempts) {
        this.cardId         = cardId;
        this.pin            = pin;
        this.accountNumber  = accountNumber;
        this.cardState      = cardState;
        this.failedAttempts = failedAttempts;
    }

    // -----------------------------------------------------------------------
    // State-delegating operations
    // -----------------------------------------------------------------------

    /** Whether the card currently accepts PIN entry. */
    public boolean canAcceptPin() { return cardState.canAcceptPin(); }

    /** Whether the card is in the BLOCKED state. */
    public boolean isBlocked()    { return cardState.isBlocked(); }

    /**
     * Records a correct PIN: resets failed attempts and transitions the state.
     */
    public void recordPinSuccess() {
        failedAttempts = 0;
        cardState = cardState.onPinSuccess();
    }

    /**
     * Records an incorrect PIN: increments the counter and lets the state
     * decide whether to block the card.
     *
     * @param maxAttempts the limit supplied by {@link com.training.atm.service.CardScanner}
     */
    public void recordPinFailure(int maxAttempts) {
        failedAttempts++;
        cardState = cardState.onPinFailure(failedAttempts, maxAttempts);
    }

    /** Resets the card to ACTIVE. Called by the admin unblock operation. */
    public void adminUnblock() {
        failedAttempts = 0;
        cardState = cardState.onAdminUnblock();
    }

    // -----------------------------------------------------------------------
    // Serialisation bridge — CardStatus is only used for persistence
    // -----------------------------------------------------------------------

    /** Returns the serialisable status for file storage. */
    public CardStatus getStatus() { return cardState.toStatus(); }

    /**
     * Reconstructs the state from a persisted {@link CardStatus}.
     * Called only by the repository layer during file load.
     */
    public static CardState stateFrom(CardStatus status) {
        return switch (status) {
            case ACTIVE  -> new ActiveCardState();
            case BLOCKED -> new com.training.atm.model.state.BlockedCardState();
        };
    }

    // -----------------------------------------------------------------------
    // PIN management — kept here because PIN is sensitive data
    // -----------------------------------------------------------------------
    public String getPin()            { return pin; }
    public void   setPin(String pin)  { this.pin = pin; }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------
    public String getCardId()        { return cardId; }
    public String getAccountNumber() { return accountNumber; }
    public int    getFailedAttempts(){ return failedAttempts; }

    /** Direct setter — kept for repository layer backward-compatibility only. */
    public void setFailedAttempts(int n) { this.failedAttempts = n; }

    public String getMaskedCardId() {
        if (cardId == null || cardId.length() < 4) return cardId;
        return "****-****-****-" + cardId.substring(cardId.length() - 4);
    }

    // -----------------------------------------------------------------------
    // Identifiable<String> — card ID is the identity key
    // -----------------------------------------------------------------------
    @Override public String getId()            { return cardId; }
    @Override public void   setId(String id)   { this.cardId = id; }
}
