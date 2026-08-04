package com.training.atm.session;

import com.training.atm.repository.*;
import com.training.atm.service.*;

/**
 * Parameter object (Fix #14) that groups the 13 dependencies of
 * {@link CustomerSession} into a single cohesive value.
 *
 * Using a Java {@code record} gives us:
 * - an immutable, all-args constructor
 * - auto-generated accessor methods
 * - {@code equals}, {@code hashCode}, and {@code toString} for free
 *
 * The {@link com.training.atm.App} composition root constructs one instance
 * and passes it to the session; every other class in the system is unaffected.
 */
public record CustomerSessionDeps(
        DisplayScreen               screen,
        CardScanner                 cardScanner,
        CashDispenser               cashDispenser,
        ReceiptPrinter              receiptPrinter,
        WithdrawalService           withdrawalService,
        DepositService              depositService,
        TransferService             transferService,
        AccountRepository           accountRepo,
        TransactionRepository       txRepo,
        CustomerRepository          customerRepo,
        CardRepository              cardRepo,
        ScheduledTransferRepository schedRepo,
        ATMInfoRepository           atmInfo
) {}
