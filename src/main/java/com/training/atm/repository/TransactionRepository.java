package com.training.atm.repository;

import com.training.atm.model.Transaction;
import com.training.atm.model.enums.TransactionType;

import java.util.List;

/** Repository abstraction for Transaction persistence. */
public interface TransactionRepository {
    void save(Transaction transaction);
    List<Transaction> findByAccountNumber(String accountNumber);
    long sumByAccountNumberTypeAndDate(String accountNumber, TransactionType type, String date);
}
