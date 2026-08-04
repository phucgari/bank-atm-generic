package com.training.atm.repository;

import com.training.atm.model.ATMCard;

import java.util.Optional;

/** Repository abstraction for ATMCard persistence. */
public interface CardRepository {
    Optional<ATMCard> findById(String cardId);
    void update(ATMCard card);
}
