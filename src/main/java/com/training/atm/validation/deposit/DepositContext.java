package com.training.atm.validation.deposit;

import com.training.atm.model.Account;

/** Carries all data required by the deposit validation chain. */
public record DepositContext(Account account, long amount) {}
