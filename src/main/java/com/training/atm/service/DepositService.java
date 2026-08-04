package com.training.atm.service;

import com.training.atm.dto.DepositResult;
import com.training.atm.model.Account;

/** Defines the contract for cash-deposit business logic. */
public interface DepositService {
    DepositResult deposit(Account account, long amount);
}
