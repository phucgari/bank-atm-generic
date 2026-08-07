package com.training.atm.command;

import com.training.atm.dto.DepositResult;
import com.training.atm.dto.ServiceResult;
import com.training.atm.model.Account;
import com.training.atm.service.DepositService;
import com.training.atm.util.FormatUtil;

/**
 * Command that deposits cash into an account.
 */
public class DepositCommand implements TransactionCommand<DepositResult> {
    private final Account account;
    private final long amount;
    private final DepositService service;

    public DepositCommand(Account account, long amount, DepositService service) {
        this.account = account;
        this.amount = amount;
        this.service = service;
    }

    @Override
    public DepositResult call() {
        return service.deposit(account, amount);
    }

    @Override
    public String getType() {
        return "DEPOSIT";
    }

    @Override
    public String describe() {
        return "DEPOSIT " + FormatUtil.formatVND(amount)
                + " into " + account.getAccountNumber();
    }
}
