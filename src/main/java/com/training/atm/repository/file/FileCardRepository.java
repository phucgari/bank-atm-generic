package com.training.atm.repository.file;

import com.training.atm.model.ATMCard;
import com.training.atm.model.enums.CardStatus;
import com.training.atm.repository.CardRepository;

import java.io.*;
import java.util.*;

/**
 * Text-file implementation of {@link CardRepository}.
 * Format: cardId|pin|accountNumber|status|failedAttempts
 *
 * State pattern: the persisted {@link CardStatus} enum value is used only as
 * a serialisation token.  On load, {@link ATMCard#stateFrom(CardStatus)}
 * converts it back to the appropriate {@link com.training.atm.model.state.CardState}
 * object so the card can delegate behaviour at runtime.
 */
public class FileCardRepository implements CardRepository {
    private static final String FILE = DataDirectory.getPath("cards.txt");
    private final Map<String, ATMCard> cards = new LinkedHashMap<>();

    public FileCardRepository() { load(); }

    private void load() {
        File f = new File(FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\|", -1);
                if (p.length < 5) continue;
                CardStatus status = CardStatus.valueOf(p[3]);
                ATMCard card = new ATMCard(
                        p[0], p[1], p[2],
                        ATMCard.stateFrom(status),   // State pattern: reconstruct state object
                        Integer.parseInt(p[4]));
                cards.put(card.getCardId(), card);
            }
        } catch (IOException e) {
            System.err.println("Error loading cards: " + e.getMessage());
        }
    }

    private void saveAll() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
            for (ATMCard c : cards.values()) {
                pw.println(c.getCardId() + "|" + c.getPin() + "|" + c.getAccountNumber()
                        + "|" + c.getStatus() + "|" + c.getFailedAttempts());
            }
        } catch (IOException e) {
            System.err.println("Error saving cards: " + e.getMessage());
        }
    }

    @Override public Optional<ATMCard> findById(String cardId) {
        return Optional.ofNullable(cards.get(cardId));
    }

    @Override public ATMCard update(ATMCard card) {
        cards.put(card.getCardId(), card);
        saveAll();
        return card;
    }
}
