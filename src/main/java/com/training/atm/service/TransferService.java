package com.training.atm.service;

import com.training.atm.dto.TransferResult;
import com.training.atm.model.Account;

/** Defines the contract for fund-transfer and scheduled-transfer business logic. */
public interface TransferService {
    TransferResult transfer(Account source, String destAccountNumber, long amount);
    void processScheduledTransfers();
}
