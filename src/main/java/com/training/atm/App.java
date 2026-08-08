package com.training.atm;

import com.training.atm.repository.*;
import com.training.atm.repository.file.DataDirectory;
import com.training.atm.config.db.TransactionManager;
import com.training.atm.service.*;
import com.training.atm.service.impl.DepositServiceImpl;
import com.training.atm.service.impl.InterestServiceImpl;
import com.training.atm.service.impl.TransferServiceImpl;
import com.training.atm.service.impl.WithdrawalServiceImpl;
import com.training.atm.session.*;

/**
 * Application entry point — the composition root.
 *
 * DIP: every class in service/session layers depends on interfaces.
 *      Only App.java knows the concrete types.  All wiring happens here,
 *      using the {@link CustomerSessionDeps} and {@link AdminSessionDeps}
 *      parameter objects so sessions never see a long argument list.
 */
public class App {
    public static void main(String[] args) {
        DataDirectory.ensureExists();

        RepositoryContext repositories = RepositoryFactory.create();
        ATMInfoRepository atmConfig = repositories.atmInfo();
        DenominationRepository denominationRepo = repositories.denominations();
        CustomerRepository customerRepo = repositories.customers();
        CardRepository cardRepo = repositories.cards();
        AccountRepository accountRepo = repositories.accounts();
        TransactionRepository txRepo = repositories.transactions();
        ScheduledTransferRepository schedRepo = repositories.scheduledTransfers();
        AdminLogRepository adminLog = repositories.adminLogs();
        TransactionManager transactionManager = repositories.transactionManager();

        // --- Hardware simulation layer ---
        DisplayScreen  screen         = new DisplayScreen();
        ReceiptPrinter receiptPrinter = new ReceiptPrinter(screen);
        CashDispenser  cashDispenser  = new CashDispenser(denominationRepo, transactionManager);
        CardScanner    cardScanner    = new CardScanner(cardRepo, screen);
        ATM            atm            = new ATM(atmConfig);

        // --- Business services ---
        WithdrawalService withdrawalService = new WithdrawalServiceImpl(
                accountRepo, txRepo, cashDispenser, transactionManager);
        DepositService    depositService    = new DepositServiceImpl(
                accountRepo, txRepo, cashDispenser, transactionManager);
        TransferService   transferService   = new TransferServiceImpl(accountRepo, txRepo, schedRepo, customerRepo, transactionManager);
        InterestService   interestService   = new InterestServiceImpl(accountRepo, txRepo);

        // Process any pending scheduled transfers at startup.
        System.out.println("[SYSTEM] Checking pending scheduled transfers...");
        transferService.processScheduledTransfers();

        // --- Session handlers (via parameter objects — no long argument list) ---
        CustomerSession customerSession = new CustomerSession(new CustomerSessionDeps(
                screen, cardScanner, cashDispenser, receiptPrinter,
                withdrawalService, depositService, transferService,
                accountRepo, txRepo, customerRepo, cardRepo, schedRepo, atmConfig));

        AdminSession adminSession = new AdminSession(new AdminSessionDeps(
                screen, interestService, atmConfig, denominationRepo,
                accountRepo, customerRepo, cardRepo, adminLog));

        // --- Main ATM loop ---
        while (true) {
            atm.show(screen);
            screen.println("1. Insert Card (Customer)");
            screen.println("2. Admin Login");
            screen.println("3. Shutdown ATM");
            screen.printSeparator();
            screen.print("Select option: ");
            String choice = screen.acceptInput();

            switch (choice) {
                case "1" -> customerSession.run();
                case "2" -> adminSession.run();
                case "3" -> { screen.println("ATM shutting down. Goodbye!"); System.exit(0); }
                default  -> screen.println("Invalid option. Please select 1, 2, or 3.");
            }
        }
    }
}
