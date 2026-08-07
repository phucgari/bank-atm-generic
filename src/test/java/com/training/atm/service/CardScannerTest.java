package com.training.atm.service;

import com.training.atm.model.ATMCard;
import com.training.atm.model.state.ActiveCardState;
import com.training.atm.model.state.BlockedCardState;
import com.training.atm.testutil.RecordingDisplayScreen;
import com.training.atm.testutil.TestRepositories.InMemoryCardRepository;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class CardScannerTest {

    private RecordingDisplayScreen screen;
    private InMemoryCardRepository cardRepo;
    private CardScanner scanner;

    @Before
    public void setUp() {
        screen = new RecordingDisplayScreen();
        cardRepo = new InMemoryCardRepository();
        scanner = new CardScanner(cardRepo, screen);
    }

    @Test
    public void acceptCardReturnsCardWhenValidAndActive() {
        ATMCard card = new ATMCard("1234567890123456", "1234", "ACC001", new ActiveCardState(), 0);
        cardRepo = new InMemoryCardRepository(card);
        scanner = new CardScanner(cardRepo, screen);

        Optional<ATMCard> result = scanner.acceptCard("1234567890123456");

        assertTrue(result.isPresent());
        assertEquals("1234567890123456", result.get().getCardId());
        assertTrue(screen.containsLine("Card accepted"));
    }

    @Test
    public void acceptCardReturnsEmptyWhenCardNotFound() {
        Optional<ATMCard> result = scanner.acceptCard("9999999999999999");

        assertFalse(result.isPresent());
        assertTrue(screen.containsLine("ERROR: Card ID not recognized"));
    }

    @Test
    public void acceptCardReturnsEmptyWhenCardIsBlocked() {
        ATMCard card = new ATMCard("1234567890123456", "1234", "ACC001", new BlockedCardState(), 3);
        cardRepo = new InMemoryCardRepository(card);
        scanner = new CardScanner(cardRepo, screen);

        Optional<ATMCard> result = scanner.acceptCard("1234567890123456");

        assertFalse(result.isPresent());
        assertTrue(screen.containsLine("ERROR: This card is BLOCKED"));
        assertTrue(screen.containsLine("contact your bank"));
    }

    @Test
    public void readCardDisplaysMaskedCardId() {
        ATMCard card = new ATMCard("1234567890123456", "1234", "ACC001", new ActiveCardState(), 0);
        screen.clear();

        scanner.readCard(card);

        assertTrue(screen.containsLine("Card verified: ****-****-****-3456"));
    }

    @Test
    public void ejectCardDisplaysMaskedCardId() {
        ATMCard card = new ATMCard("1234567890123456", "1234", "ACC001", new ActiveCardState(), 0);
        screen.clear();

        scanner.ejectCard(card);

        assertTrue(screen.containsLine("Card ejected"));
        assertTrue(screen.containsLine("****-****-****-3456"));
    }

    @Test
    public void validatePinReturnsTrueAndResetsAttemptsWhenCorrect() {
        ATMCard card = new ATMCard("1234567890123456", "hashed_pin", "ACC001", new ActiveCardState(), 2);
        cardRepo = new InMemoryCardRepository(card);
        scanner = new CardScanner(cardRepo, screen);

        boolean result = scanner.validatePin(card, "hashed_pin");

        assertTrue(result);
        assertEquals(0, card.getFailedAttempts());
    }

    @Test
    public void validatePinReturnsFalseAndIncrementsAttemptsWhenIncorrect() {
        ATMCard card = new ATMCard("1234567890123456", "correct_pin", "ACC001", new ActiveCardState(), 0);
        cardRepo = new InMemoryCardRepository(card);
        scanner = new CardScanner(cardRepo, screen);

        boolean result = scanner.validatePin(card, "wrong_pin");

        assertFalse(result);
        assertEquals(1, card.getFailedAttempts());
        assertTrue(screen.containsLine("ERROR: Incorrect PIN"));
        assertTrue(screen.containsLine("Attempts remaining: 2"));
    }

    @Test
    public void validatePinBlocksCardAfterMaxAttempts() {
        ATMCard card = new ATMCard("1234567890123456", "correct_pin", "ACC001", new ActiveCardState(), 2);
        cardRepo = new InMemoryCardRepository(card);
        scanner = new CardScanner(cardRepo, screen);

        boolean result = scanner.validatePin(card, "wrong_pin");

        assertFalse(result);
        assertTrue(card.isBlocked());
        assertTrue(screen.containsLine("ERROR: Too many failed attempts"));
        assertTrue(screen.containsLine("Your card has been BLOCKED"));
    }

    @Test
    public void validatePinShowsRemainingAttemptsCorrectly() {
        ATMCard card = new ATMCard("1234567890123456", "correct_pin", "ACC001", new ActiveCardState(), 1);
        cardRepo = new InMemoryCardRepository(card);
        scanner = new CardScanner(cardRepo, screen);

        scanner.validatePin(card, "wrong_pin");

        assertTrue(screen.containsLine("Attempts remaining: 1"));
    }
}
