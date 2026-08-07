package com.training.atm.command;

import com.training.atm.dto.ServiceResult;
import com.training.atm.dto.WithdrawalResult;
import com.training.atm.model.Account;
import com.training.atm.service.WithdrawalService;
import com.training.atm.util.FormatUtil;

/**
 * Command that withdraws cash from an account.
 */
public class WithdrawCommand implements TransactionCommand<WithdrawalResult> {
    private final Account account;
    private final long amount;
    private final WithdrawalService service;

    public WithdrawCommand(Account account, long amount, WithdrawalService service) {
        this.account = account;
        this.amount = amount;
        this.service = service;
    }

    @Override
    public WithdrawalResult call() {
        return service.withdraw(account, amount);
    }

    @Override
    public String getType() {
        return "WITHDRAWAL";
    }

    @Override
    public String describe() {
        return "WITHDRAW " + FormatUtil.formatVND(amount)
                + " from " + account.getAccountNumber();
    }
}
