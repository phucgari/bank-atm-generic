package com.training.atm.repository;

import com.training.atm.model.BankCustomer;

import java.util.Collection;
import java.util.Optional;

/** Repository abstraction for BankCustomer persistence. */
public interface CustomerRepository {
    Optional<BankCustomer> findByCardId(String cardId);
    Optional<BankCustomer> findByAccountNumber(String accountNumber);
    Collection<BankCustomer> findAll();
}
