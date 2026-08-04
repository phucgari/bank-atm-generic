package com.training.atm.service;

import com.training.atm.model.ATMCard;
import com.training.atm.repository.CardRepository;

import java.util.Optional;

/**
 * Simulates ATM card-reader hardware: validates, reads, and ejects cards.
 *
 * <h3>State pattern</h3>
 * Card status checks and transitions are delegated to methods on
 * {@link ATMCard} that forward to the embedded
 * {@link com.training.atm.model.state.CardState} object:
 * <ul>
 *   <li>{@link ATMCard#isBlocked()} replaces {@code card.getStatus() == CardStatus.BLOCKED}</li>
 *   <li>{@link ATMCard#recordPinSuccess()} replaces manually resetting failedAttempts + updating status</li>
 *   <li>{@link ATMCard#recordPinFailure(int)} replaces incrementing failedAttempts +
 *       conditionally setting status to BLOCKED</li>
 * </ul>
 * All transition logic now lives in the state objects, not scattered across
 * multiple callers.
 *
 * <h3>Design notes</h3>
 * DIP: depends on the {@link CardRepository} interface, not any concrete
 * file-storage implementation.
 */
public class CardScanner {
    /** Extracted from the literal {@code 3} that previously appeared in two places. */
    public static final int MAX_PIN_ATTEMPTS = 3;

    private final CardRepository cardRepo;
    private final DisplayScreen  screen;

    public CardScanner(CardRepository cardRepo, DisplayScreen screen) {
        this.cardRepo = cardRepo;
        this.screen   = screen;
    }

    /** Returns the card when it exists and is not blocked; prints an error otherwise. */
    public Optional<ATMCard> acceptCard(String cardId) {
        Optional<ATMCard> cardOpt = cardRepo.findById(cardId);
        if (cardOpt.isEmpty()) {
            screen.println("ERROR: Card ID not recognized. Please check your card number.");
            return Optional.empty();
        }
        ATMCard card = cardOpt.get();
        if (card.isBlocked()) {     // State pattern: delegates to CardState
            screen.println("ERROR: This card is BLOCKED due to multiple failed PIN attempts.");
            screen.println("       Please contact your bank to reactivate your card.");
            return Optional.empty();
        }
        screen.println("Card accepted. Reading card...");
        return Optional.of(card);
    }

    public void readCard(ATMCard card) {
        screen.println("Card verified: " + card.getMaskedCardId());
    }

    public void ejectCard(ATMCard card) {
        screen.println("Card ejected. Please take your card: " + card.getMaskedCardId());
    }

    /**
     * Validates the entered PIN.  On failure the card's state handles the
     * attempt counter and decides whether to transition to BLOCKED.
     *
     * @return {@code true} if the PIN matches
     */
    public boolean validatePin(ATMCard card, String enteredPin) {
        if (card.getPin().equals(enteredPin)) {
            card.recordPinSuccess();    // State pattern: resets counter, stays ACTIVE
            cardRepo.update(card);
            return true;
        }
        card.recordPinFailure(MAX_PIN_ATTEMPTS);    // State pattern: increments counter + may block
        cardRepo.update(card);
        if (card.isBlocked()) {
            screen.println("ERROR: Too many failed attempts. Your card has been BLOCKED.");
            screen.println("       Please contact your bank to reactivate it.");
        } else {
            int remaining = MAX_PIN_ATTEMPTS - card.getFailedAttempts();
            screen.println("ERROR: Incorrect PIN. Attempts remaining: " + remaining);
        }
        return false;
    }
}
