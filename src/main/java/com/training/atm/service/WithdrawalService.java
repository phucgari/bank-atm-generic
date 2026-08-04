package com.training.atm.service;

import com.training.atm.dto.WithdrawalResult;
import com.training.atm.model.Account;

/**
 * Defines the contract for cash-withdrawal business logic.
 * Separating this interface from the implementation follows both SRP and DIP:
 * the session layer depends on this abstraction, not on any concrete class.
 */
public interface WithdrawalService {
    WithdrawalResult withdraw(Account account, long amount);
}
