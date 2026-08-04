package com.training.atm.service;

import com.training.atm.repository.DenominationRepository;
import com.training.atm.util.DenominationDispenser;

import java.util.Map;

/**
 * Manages ATM cash operations: dispensing bills for withdrawals and
 * accepting bills for deposits.
 *
 * SRP: receipt/bill-breakdown display is now handled by {@link ReceiptPrinter}.
 * DIP: depends on the {@link DenominationRepository} abstraction, not on any
 *      concrete file-storage class.
 */
public class CashDispenser {
    private final DenominationRepository denomRepo;

    public CashDispenser(DenominationRepository denomRepo) {
        this.denomRepo = denomRepo;
    }

    public long getAvailableCash() {
        return denomRepo.getTotalCash();
    }

    public boolean hasSufficientCash(long amount) {
        return denomRepo.getTotalCash() >= amount;
    }

    /**
     * Attempts to dispense exactly {@code amount} VND using available bills.
     * Returns the bill breakdown on success, or {@code null} if impossible.
     */
    public Map<Long, Integer> dispenseCash(long amount) {
        Map<Long, Integer> available = denomRepo.getDenominations();
        Map<Long, Integer> dispensed = DenominationDispenser.calculate(amount, available);
        if (dispensed == null) return null;
        denomRepo.dispenseBills(dispensed);
        return dispensed;
    }

    /** Accepts deposited cash and increases the ATM's available funds. */
    public void acceptAmount(long amount) {
        denomRepo.addDepositCash(amount);
    }
}
