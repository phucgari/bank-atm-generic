package com.training.atm.event;

public final class CardBlockedEvent extends DomainEvent {
    private final String cardId;
    private final String accountNumber;

    public CardBlockedEvent(String cardId, String accountNumber) {
        this.cardId = cardId;
        this.accountNumber = accountNumber;
    }

    public String getCardId() {
        return cardId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
