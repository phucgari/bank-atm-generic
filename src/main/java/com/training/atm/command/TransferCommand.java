package com.training.atm.command;

import com.training.atm.dto.ServiceResult;
import com.training.atm.dto.TransferResult;
import com.training.atm.model.Account;
import com.training.atm.service.TransferService;
import com.training.atm.util.FormatUtil;

/**
 * Command that transfers funds between two accounts.
 * <p>
 * This is the primary entry point used by the scheduled-transfer scheduler
 * in {@link com.training.atm.service.impl.TransferServiceImpl}: a
 * {@code TransferCommand} is built from each active {@code ScheduledTransfer}
 * and executed uniformly, decoupling the scheduler loop from the transfer
 * business logic.
 */
public class TransferCommand implements TransactionCommand<TransferResult> {
    private final Account source;
    private final String destAccountNumber;
    private final long amount;
    private final TransferService service;

    public TransferCommand(Account source, String destAccountNumber,
                           long amount, TransferService service) {
        this.source = source;
        this.destAccountNumber = destAccountNumber;
        this.amount = amount;
        this.service = service;
    }

    @Override
    public TransferResult call() {
        return service.transfer(source, destAccountNumber, amount);
    }

    @Override
    public String getType() {
        return "TRANSFER";
    }

    @Override
    public String describe() {
        return "TRANSFER " + FormatUtil.formatVND(amount)
                + " from " + source.getAccountNumber()
                + " to " + destAccountNumber;
    }
}
