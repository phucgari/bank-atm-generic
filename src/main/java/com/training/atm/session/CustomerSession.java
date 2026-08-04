package com.training.atm.session;

import com.training.atm.command.DepositCommand;
import com.training.atm.command.TransferCommand;
import com.training.atm.command.WithdrawCommand;
import com.training.atm.config.TransactionLimits;
import com.training.atm.dto.*;
import com.training.atm.model.*;
import com.training.atm.model.enums.*;
import com.training.atm.model.state.ActiveTransferState;
import com.training.atm.repository.*;
import com.training.atm.service.*;
import com.training.atm.util.*;

import java.time.LocalDate;
import java.util.*;

/**
 * Manages a single customer ATM session from card insertion to ejection.
 *
 * <h3>Command pattern</h3>
 * Withdrawal, deposit, and transfer operations are wrapped in
 * {@link WithdrawCommand}, {@link DepositCommand}, and {@link TransferCommand}
 * respectively.  This decouples the session UI from service method signatures
 * and makes each operation a first-class object (e.g. for future undo/redo
 * or audit-log enrichment).
 *
 * <h3>State pattern</h3>
 * Scheduled-transfer lifecycle operations ({@code pause}, {@code resume},
 * {@code cancel}) are delegated to state-machine methods on
 * {@link ScheduledTransfer} ({@link ScheduledTransfer#canPause()},
 * {@link ScheduledTransfer#pause()}, etc.) instead of raw
 * {@code if (status == ACTIVE)} comparisons.
 *
 * <h3>Design notes</h3>
 * DIP: all service and repository dependencies are injected as interfaces
 *      through {@link CustomerSessionDeps} — no concrete implementation is
 *      referenced here.<br>
 * SRP: this class owns only the customer-facing console flow; all business
 *      rules live in the dedicated service classes.
 */
public class CustomerSession {

    /** Replaces the naked literal {@code 3} used for PIN-entry loops. */
    private static final int PIN_ATTEMPT_LIMIT = CardScanner.MAX_PIN_ATTEMPTS;

    /** Replaces the naked literal {@code 10} used in transaction history. */
    private static final int HISTORY_DISPLAY_LIMIT = 10;

    // Unpacked from CustomerSessionDeps for cleaner method-body readability.
    private final DisplayScreen               screen;
    private final CardScanner                 cardScanner;
    private final CashDispenser               cashDispenser;
    private final ReceiptPrinter              receiptPrinter;
    private final WithdrawalService           withdrawalService;
    private final DepositService              depositService;
    private final TransferService             transferService;
    private final AccountRepository           accountRepo;
    private final TransactionRepository       txRepo;
    private final CustomerRepository          customerRepo;
    private final CardRepository              cardRepo;
    private final ScheduledTransferRepository schedRepo;
    private final ATMInfoRepository           atmInfo;

    /** Single-parameter constructor — no more Long Parameter List. */
    public CustomerSession(CustomerSessionDeps deps) {
        this.screen            = deps.screen();
        this.cardScanner       = deps.cardScanner();
        this.cashDispenser     = deps.cashDispenser();
        this.receiptPrinter    = deps.receiptPrinter();
        this.withdrawalService = deps.withdrawalService();
        this.depositService    = deps.depositService();
        this.transferService   = deps.transferService();
        this.accountRepo       = deps.accountRepo();
        this.txRepo            = deps.txRepo();
        this.customerRepo      = deps.customerRepo();
        this.cardRepo          = deps.cardRepo();
        this.schedRepo         = deps.schedRepo();
        this.atmInfo           = deps.atmInfo();
    }

    public void run() {
        screen.print("Please insert your card (Enter Card ID): ");
        String cardId = screen.acceptInput();

        Optional<ATMCard> cardOpt = cardScanner.acceptCard(cardId);
        if (cardOpt.isEmpty()) return;
        ATMCard card = cardOpt.get();
        cardScanner.readCard(card);

        boolean authenticated = false;
        for (int attempt = 0; attempt < PIN_ATTEMPT_LIMIT; attempt++) {
            String pin = screen.readPin("Enter PIN: ");
            if (!ValidationUtil.isValidPin(pin)) {
                screen.println("PIN must be exactly 4 numeric digits.");
                continue;
            }
            if (cardScanner.validatePin(card, pin)) { authenticated = true; break; }
            if (card.isBlocked()) return;   // State pattern: delegates to CardState
        }
        if (!authenticated) {
            screen.println("Session terminated after too many failed attempts.");
            return;
        }
        screen.println("Authentication successful!");

        Optional<Account> accOpt = accountRepo.findByAccountNumber(card.getAccountNumber());
        if (accOpt.isEmpty()) {
            screen.println("ERROR: Associated account not found. Please contact your bank.");
            cardScanner.ejectCard(card);
            return;
        }
        Account account = accOpt.get();
        String customerName = customerRepo.findByAccountNumber(account.getAccountNumber())
                .map(BankCustomer::getCustomerName).orElse("Customer");
        screen.println("Welcome, " + customerName + "!");

        transactionMenu(card, account);
        cardScanner.ejectCard(card);
    }

    // -----------------------------------------------------------------------
    // Transaction menu
    // -----------------------------------------------------------------------
    private void transactionMenu(ATMCard card, Account account) {
        while (true) {
            screen.println();
            screen.println("--- TRANSACTION MENU ---");
            screen.println("1. Cash Withdrawal");
            screen.println("2. Cash Deposit");
            screen.println("3. Balance Inquiry");
            screen.println("4. Transaction History");
            screen.println("5. Fund Transfer");
            screen.println("6. Change PIN");
            screen.println("7. Scheduled Transfers");
            screen.println("8. Eject Card & Exit");
            screen.print("Select option: ");
            String choice = screen.acceptInput();

            // Reload account to pick up any balance changes from previous operations.
            account = accountRepo.findByAccountNumber(account.getAccountNumber()).orElse(account);

            switch (choice) {
                case "1" -> doWithdrawal(card, account);
                case "2" -> doDeposit(account);
                case "3" -> doBalanceInquiry(card, account);
                case "4" -> doTransactionHistory(account);
                case "5" -> doTransfer(account);
                case "6" -> doPinChange(card);
                case "7" -> doScheduledTransfers(account);
                case "8" -> { return; }
                default  -> screen.println("Invalid option. Please try again.");
            }
        }
    }

    // -----------------------------------------------------------------------
    // FR-02: Cash Withdrawal — Command pattern
    // -----------------------------------------------------------------------
    private void doWithdrawal(ATMCard card, Account account) {
        screen.println();
        screen.println("=== CASH WITHDRAWAL ===");
        screen.println("Account Type    : " + account.getAccountTypeLabel());
        screen.println("Current Balance : " + FormatUtil.formatVND(account.getAccountBalance()));
        screen.println();
        screen.print("Enter withdrawal amount: ");
        long amount = parseAmount(screen.acceptInput());
        if (amount < 0) return;

        // Command pattern: encapsulate the operation.
        WithdrawCommand cmd = new WithdrawCommand(account, amount, withdrawalService);
        WithdrawalResult result = cmd.execute();
        if (!result.isSuccess()) { screen.println("ERROR: " + result.getMessage()); return; }

        screen.println(String.format("  %-20s: %s", "Amount",
                FormatUtil.formatVND(result.getTransaction().getAmount())));
        screen.println(String.format("  %-20s: %s", "New Balance",
                FormatUtil.formatVND(result.getTransaction().getBalanceAfter())));
        screen.println(String.format("  %-20s: %s", "Remaining ATM Cash",
                FormatUtil.formatVND(result.getRemainingAtmCash())));
        if (result.getDispensed() != null) receiptPrinter.printBillBreakdown(result.getDispensed());

        screen.print("Would you like a receipt? (Y/N): ");
        if (screen.acceptInput().equalsIgnoreCase("Y")) {
            receiptPrinter.printReceipt(
                    result.getTransaction().getDateTime(), card.getMaskedCardId(),
                    cmd.getType(), result.getTransaction().getAmount(),
                    result.getTransaction().getBalanceAfter(),
                    atmInfo.getBranchName(), result.getDispensed());
        }
    }

    // -----------------------------------------------------------------------
    // FR-03: Cash Deposit — Command pattern
    // -----------------------------------------------------------------------
    private void doDeposit(Account account) {
        screen.println();
        screen.println("=== CASH DEPOSIT ===");
        screen.println("Account Type    : " + account.getAccountTypeLabel());
        screen.println("Current Balance : " + FormatUtil.formatVND(account.getAccountBalance()));
        screen.println();
        screen.print("Enter deposit amount: ");
        long amount = parseAmount(screen.acceptInput());
        if (amount < 0) return;

        screen.println();
        screen.println("--- Deposit Confirmation ---");
        screen.println("  Amount      : " + FormatUtil.formatVND(amount));
        screen.println("  New Balance : " + FormatUtil.formatVND(account.getAccountBalance() + amount));
        screen.print("Confirm deposit? (Y/N): ");
        if (!screen.acceptInput().equalsIgnoreCase("Y")) {
            screen.println("Deposit cancelled."); return;
        }

        // Command pattern: encapsulate the operation.
        DepositCommand cmd = new DepositCommand(account, amount, depositService);
        DepositResult result = cmd.execute();
        if (!result.isSuccess()) { screen.println("ERROR: " + result.getMessage()); return; }
        screen.println("Deposit successful!");
        screen.println("  Amount      : " + FormatUtil.formatVND(result.getTransaction().getAmount()));
        screen.println("  New Balance : " + FormatUtil.formatVND(result.getTransaction().getBalanceAfter()));
    }

    // -----------------------------------------------------------------------
    // FR-04: Balance Inquiry & Transaction History
    // -----------------------------------------------------------------------
    private void doBalanceInquiry(ATMCard card, Account account) {
        screen.println();
        screen.println("=== BALANCE INQUIRY ===");
        screen.println("Account Type    : " + account.getAccountTypeLabel());
        screen.println("Account Number  : " + account.getAccountNumber());
        screen.println("Current Balance : " + FormatUtil.formatVND(account.getAccountBalance()));
        screen.print("Would you like a receipt? (Y/N): ");
        if (screen.acceptInput().equalsIgnoreCase("Y")) {
            receiptPrinter.printReceipt(DateUtil.now(), card.getMaskedCardId(),
                    "BALANCE INQUIRY", 0, account.getAccountBalance(),
                    atmInfo.getBranchName(), null);
        }
    }

    private void doTransactionHistory(Account account) {
        screen.println();
        screen.println("=== TRANSACTION HISTORY (last " + HISTORY_DISPLAY_LIMIT + ") ===");
        List<Transaction> all = new ArrayList<>(txRepo.findByAccountNumber(account.getAccountNumber()));
        all.sort(Comparator.comparing(Transaction::getDateTime).reversed());
        List<Transaction> recent = all.subList(0, Math.min(HISTORY_DISPLAY_LIMIT, all.size()));

        if (recent.isEmpty()) { screen.println("No transaction history available."); return; }
        screen.println(String.format("%-22s %-14s %16s %16s",
                "Date/Time", "Type", "Amount", "Balance After"));
        screen.printSeparator();
        for (Transaction t : recent) {
            screen.println(String.format("%-22s %-14s %16s %16s",
                    t.getDateTime(), t.getType(),
                    FormatUtil.formatAmount(t.getAmount()),
                    FormatUtil.formatAmount(t.getBalanceAfter())));
        }
    }

    // -----------------------------------------------------------------------
    // FR-07: Fund Transfer — Command pattern
    // -----------------------------------------------------------------------
    private void doTransfer(Account account) {
        screen.println();
        screen.println("=== FUND TRANSFER ===");
        screen.println("Source Account  : " + account.getAccountNumber());
        screen.println("Current Balance : " + FormatUtil.formatVND(account.getAccountBalance()));
        screen.println();
        screen.print("Enter destination account number: ");
        String destAccNum = screen.acceptInput();
        screen.print("Enter transfer amount: ");
        long amount = parseAmount(screen.acceptInput());
        if (amount < 0) return;

        String destName = customerRepo.findByAccountNumber(destAccNum)
                .map(BankCustomer::getCustomerName).orElse(null);
        if (destName == null) {
            screen.println("ERROR: Destination account " + destAccNum + " not found."); return;
        }

        screen.println();
        screen.println("--- Transfer Confirmation ---");
        screen.println("  Destination Account  : " + destAccNum);
        screen.println("  Destination Holder   : " + destName);
        screen.println("  Amount               : " + FormatUtil.formatVND(amount));
        screen.print("Confirm transfer? (Y/N): ");
        if (!screen.acceptInput().equalsIgnoreCase("Y")) {
            screen.println("Transfer cancelled."); return;
        }

        // Command pattern: encapsulate the operation.
        TransferCommand cmd = new TransferCommand(account, destAccNum, amount, transferService);
        TransferResult result = cmd.execute();
        if (!result.isSuccess()) { screen.println("ERROR: " + result.getMessage()); return; }
        screen.println("Transfer successful!");
        screen.println("  Amount transferred   : " + FormatUtil.formatVND(result.getTransaction().getAmount()));
        screen.println("  New Balance          : " + FormatUtil.formatVND(result.getTransaction().getBalanceAfter()));
    }

    // -----------------------------------------------------------------------
    // FR-05: PIN Change
    // -----------------------------------------------------------------------
    private void doPinChange(ATMCard card) {
        screen.println();
        screen.println("=== CHANGE PIN ===");
        String currentPin = screen.readPin("Enter current PIN: ");
        if (!card.getPin().equals(currentPin)) {
            screen.println("ERROR: Current PIN is incorrect."); return;
        }
        for (int attempt = 0; attempt < PIN_ATTEMPT_LIMIT; attempt++) {
            String newPin = screen.readPin("Enter new PIN: ");
            if (!ValidationUtil.isValidPin(newPin))   { screen.println("ERROR: PIN must be 4 numeric digits."); continue; }
            if (ValidationUtil.isWeakPin(newPin))      { screen.println("ERROR: PIN is too simple."); continue; }
            if (newPin.equals(card.getPin()))           { screen.println("ERROR: New PIN must differ from current."); continue; }
            if (!newPin.equals(screen.readPin("Confirm new PIN: "))) {
                screen.println("ERROR: PINs do not match."); continue;
            }
            card.setPin(newPin);
            cardRepo.update(card);
            screen.println("PIN changed successfully!");
            return;
        }
        screen.println("Too many failed attempts. PIN change cancelled.");
    }

    // -----------------------------------------------------------------------
    // FR-10: Scheduled Transfers — State pattern
    // -----------------------------------------------------------------------
    private void doScheduledTransfers(Account account) {
        while (true) {
            screen.println();
            screen.println("=== SCHEDULED TRANSFERS ===");
            screen.println("1. View  2. Create  3. Pause/Resume  4. Cancel  5. Back");
            screen.print("Select option: ");
            switch (screen.acceptInput()) {
                case "1" -> listScheduledTransfers(account);
                case "2" -> createScheduledTransfer(account);
                case "3" -> toggleScheduledTransfer(account);
                case "4" -> cancelScheduledTransfer(account);
                case "5" -> { return; }
                default  -> screen.println("Invalid option.");
            }
        }
    }

    private void listScheduledTransfers(Account account) {
        List<ScheduledTransfer> list = schedRepo.findBySourceAccount(account.getAccountNumber());
        if (list.isEmpty()) { screen.println("No scheduled transfers found."); return; }
        screen.println(String.format("%-14s %-14s %14s %-12s %-10s %-10s",
                "ID", "Destination", "Amount", "Next Date", "Frequency", "Status"));
        screen.printSeparator();
        list.forEach(st -> screen.println(String.format("%-14s %-14s %14s %-12s %-10s %-10s",
                st.getId(), st.getDestAccount(),
                FormatUtil.formatAmount(st.getAmount()),
                st.getNextExecutionDate(), st.getFrequency(), st.getStatus())));
    }

    private void createScheduledTransfer(Account account) {
        if (schedRepo.countActiveBySourceAccount(account.getAccountNumber())
                >= TransactionLimits.MAX_SCHEDULED_ACTIVE) {
            screen.println("ERROR: Maximum " + TransactionLimits.MAX_SCHEDULED_ACTIVE
                    + " active transfers allowed."); return;
        }
        screen.print("Destination account: ");
        String destAcc = screen.acceptInput();
        if (accountRepo.findByAccountNumber(destAcc).isEmpty()) {
            screen.println("ERROR: Account not found."); return;
        }
        if (destAcc.equals(account.getAccountNumber())) {
            screen.println("ERROR: Source and destination cannot be the same."); return;
        }

        screen.print("Amount: ");
        long amount = parseAmount(screen.acceptInput());
        if (amount <= 0 || amount > TransactionLimits.MAX_TRANSFER_SINGLE) {
            screen.println("ERROR: Invalid amount."); return;
        }
        screen.println("Frequency: 1=ONE_TIME  2=DAILY  3=WEEKLY  4=MONTHLY");
        screen.print("Select: ");
        TransferFrequency freq = switch (screen.acceptInput()) {
            case "1" -> TransferFrequency.ONE_TIME;
            case "2" -> TransferFrequency.DAILY;
            case "3" -> TransferFrequency.WEEKLY;
            case "4" -> TransferFrequency.MONTHLY;
            default  -> null;
        };
        if (freq == null) { screen.println("Invalid frequency."); return; }

        screen.print("Start date (yyyy-MM-dd, tomorrow or later): ");
        String startDate = screen.acceptInput();
        try {
            if (!LocalDate.parse(startDate, DateUtil.DATE_FMT).isAfter(LocalDate.now())) {
                screen.println("ERROR: Must be a future date."); return;
            }
        } catch (Exception e) { screen.println("Invalid date format."); return; }

        int    maxRepeat = (freq == TransferFrequency.ONE_TIME) ? 1 : -1;
        String endDate   = "";
        if (freq != TransferFrequency.ONE_TIME) {
            screen.print("Max repeats (-1 for unlimited): ");
            try { maxRepeat = Integer.parseInt(screen.acceptInput()); } catch (Exception e) { maxRepeat = -1; }
            if (maxRepeat == -1) {
                screen.print("End date (yyyy-MM-dd, blank for none): ");
                endDate = screen.acceptInput().trim();
                if (!endDate.isEmpty()) {
                    try { LocalDate.parse(endDate, DateUtil.DATE_FMT); }
                    catch (Exception e) { screen.println("Invalid end date."); return; }
                }
            }
        }
        String id = "ST" + System.currentTimeMillis();
        // State pattern: pass ActiveTransferState directly instead of TransferStatus.ACTIVE enum.
        schedRepo.save(new ScheduledTransfer(id, account.getAccountNumber(), destAcc,
                amount, freq, startDate, new ActiveTransferState(), maxRepeat, 0, endDate));
        screen.println("Scheduled transfer created! ID: " + id);
    }

    private void toggleScheduledTransfer(Account account) {
        screen.print("Enter transfer ID: ");
        String id = screen.acceptInput();
        Optional<ScheduledTransfer> opt = schedRepo.findById(id);
        if (opt.isEmpty() || !opt.get().getSourceAccount().equals(account.getAccountNumber())) {
            screen.println("Transfer not found."); return;
        }
        ScheduledTransfer st = opt.get();
        // State pattern: query capability via canPause()/canResume() — no status comparisons.
        if (st.canPause()) {
            st.pause();
            schedRepo.update(st);
            screen.println("Transfer paused.");
        } else if (st.canResume()) {
            st.setNextExecutionDate(DateUtil.advanceDate(DateUtil.today(), st.getFrequency()));
            st.resume();
            schedRepo.update(st);
            screen.println("Transfer resumed. Next execution: " + st.getNextExecutionDate());
        } else {
            screen.println("Cannot toggle transfer with status: " + st.getStatus());
        }
    }

    private void cancelScheduledTransfer(Account account) {
        screen.print("Enter transfer ID to cancel: ");
        String id = screen.acceptInput();
        Optional<ScheduledTransfer> opt = schedRepo.findById(id);
        if (opt.isEmpty() || !opt.get().getSourceAccount().equals(account.getAccountNumber())) {
            screen.println("Transfer not found."); return;
        }
        ScheduledTransfer st = opt.get();
        // State pattern: query capability via canCancel() — no raw status comparisons.
        if (!st.canCancel()) {
            screen.println("Cannot cancel — status is already: " + st.getStatus()); return;
        }
        st.cancel();
        schedRepo.update(st);
        screen.println("Scheduled transfer cancelled.");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private long parseAmount(String input) {
        try {
            return Long.parseLong(input.replace(",", "").replace(".", ""));
        } catch (NumberFormatException e) {
            screen.println("Invalid amount entered.");
            return -1;
        }
    }
}
