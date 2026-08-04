package com.training.atm.repository;

import com.training.atm.model.Account;

import java.util.Collection;
import java.util.Optional;

/**
 * Repository abstraction for Account persistence.
 * High-level services depend on this interface (DIP), not on any file-I/O
 * implementation.  A different implementation (in-memory, JDBC…) can be
 * injected without touching service or session code.
 */
public interface AccountRepository {
    Optional<Account> findByAccountNumber(String accountNumber);
    Collection<Account> findAll();
    void update(Account account);
    void updateAll(Collection<Account> accounts);
}
