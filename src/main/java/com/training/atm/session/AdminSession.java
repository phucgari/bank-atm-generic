package com.training.atm.session;

import com.training.atm.config.TransactionLimits;
import com.training.atm.model.*;
import com.training.atm.repository.*;
import com.training.atm.service.*;
import com.training.atm.util.*;

import java.util.*;

/**
 * Manages the admin console session (FR-08).
 *
 * <h3>State pattern</h3>
 * Card status checks and transitions are delegated to methods on
 * {@link ATMCard} that forward to the embedded
 * {@link com.training.atm.model.state.CardState} object:
 * <ul>
 *   <li>{@link ATMCard#isBlocked()} replaces {@code card.getStatus() == CardStatus.ACTIVE}</li>
 *   <li>{@link ATMCard#adminUnblock()} replaces the two-line sequence of
 *       {@code card.setStatus(CardStatus.ACTIVE); card.setFailedAttempts(0)}</li>
 * </ul>
 *
 * <h3>Design notes</h3>
 * DIP: all dependencies are injected as interfaces through
 *      {@link AdminSessionDeps} — no concrete class is referenced here.<br>
 * SRP: this class owns only the admin-facing console flow; business rules
 *      live in the dedicated service classes.
 */
public class AdminSession {
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "Admin@1234";

    private final DisplayScreen        screen;
    private final InterestService      interestService;
    private final ATMInfoRepository    atmInfo;
    private final DenominationRepository denomRepo;
    private final AccountRepository    accountRepo;
    private final CustomerRepository   customerRepo;
    private final CardRepository       cardRepo;
    private final AdminLogRepository   adminLog;

    /** Single-parameter constructor — no more Long Parameter List. */
    public AdminSession(AdminSessionDeps deps) {
        this.screen          = deps.screen();
        this.interestService = deps.interestService();
        this.atmInfo         = deps.atmInfo();
        this.denomRepo       = deps.denomRepo();
        this.accountRepo     = deps.accountRepo();
        this.customerRepo    = deps.customerRepo();
        this.cardRepo        = deps.cardRepo();
        this.adminLog        = deps.adminLog();
    }

    public void run() {
        screen.println();
        screen.println("=== ADMIN LOGIN ===");
        screen.print("Username: ");
        String user = screen.acceptInput();
        String pass = screen.readPin("Password: ");

        if (!ADMIN_USER.equals(user) || !ADMIN_PASS.equals(pass)) {
            screen.println("ERROR: Invalid admin credentials.");
            return;
        }

        adminLog.log(DateUtil.now(), user, "Admin login");
        screen.println("Admin access granted. Welcome, " + user + "!");

        adminMenu(user);
    }

    // -----------------------------------------------------------------------
    // Admin menu
    // -----------------------------------------------------------------------
    private void adminMenu(String user) {
        while (true) {
            screen.println();
            screen.println("--- ADMIN MENU ---");
            screen.println("1. View ATM Status");
            screen.println("2. Replenish Cash (by denomination)");
            screen.println("3. View All Customer Accounts");
            screen.println("4. Unblock a Card");
            screen.println("5. Calculate Interest for All Accounts");
            screen.println("6. View Denomination Inventory");
            screen.println("7. Logout");
            screen.print("Select option: ");
            String choice = screen.acceptInput();

            switch (choice) {
                case "1" -> viewAtmStatus(user);
                case "2" -> replenishCash(user);
                case "3" -> viewAllAccounts(user);
                case "4" -> unblockCard(user);
                case "5" -> calculateInterest(user);
                case "6" -> viewDenominations(user);
                case "7" -> { adminLog.log(DateUtil.now(), user, "Admin logout"); return; }
                default  -> screen.println("Invalid option.");
            }
        }
    }

    // -----------------------------------------------------------------------
    // FR-08 admin functions
    // -----------------------------------------------------------------------
    private void viewAtmStatus(String user) {
        screen.println();
        screen.println("=== ATM STATUS ===");
        screen.println("Location      : " + atmInfo.getLocation());
        screen.println("Branch        : " + atmInfo.getBranchName());
        screen.println("Available Cash: " + FormatUtil.formatVND(denomRepo.getTotalCash()));
        screen.println("Max Capacity  : " + FormatUtil.formatVND(atmInfo.getMaxCapacity()));
        adminLog.log(DateUtil.now(), user, "Viewed ATM status");
    }

    private void replenishCash(String user) {
        screen.println();
        screen.println("=== REPLENISH CASH ===");
        screen.println("Supported denominations: 500,000 | 200,000 | 100,000 | 50,000");
        screen.print("Enter denomination: ");
        long denom;
        try { denom = Long.parseLong(screen.acceptInput().replace(",", "")); }
        catch (NumberFormatException e) { screen.println("Invalid denomination."); return; }

        if (!denomRepo.isValidDenomination(denom)) {
            screen.println("ERROR: Denomination must be 500000, 200000, 100000, or 50000."); return;
        }
        screen.print("Enter quantity of bills to add: ");
        int qty;
        try { qty = Integer.parseInt(screen.acceptInput()); }
        catch (NumberFormatException e) { screen.println("Invalid quantity."); return; }
        if (qty <= 0) { screen.println("ERROR: Quantity must be positive."); return; }

        long addAmount = denom * qty;
        if (!ValidationUtil.isMultipleOf(addAmount, TransactionLimits.REPLENISH_DENOMINATION)) {
            screen.println("ERROR: Replenishment amount must be a multiple of "
                    + FormatUtil.formatVND(TransactionLimits.REPLENISH_DENOMINATION) + "."); return;
        }
        if (!denomRepo.replenish(denom, qty)) {
            screen.println("ERROR: Replenishment would exceed the ATM maximum capacity of "
                    + FormatUtil.formatVND(atmInfo.getMaxCapacity())); return;
        }
        screen.println("Cash replenished: " + qty + " x "
                + FormatUtil.formatVND(denom) + " = " + FormatUtil.formatVND(addAmount));
        screen.println("New total cash: " + FormatUtil.formatVND(denomRepo.getTotalCash()));
        adminLog.log(DateUtil.now(), user, "Replenished " + qty + " x " + denom + " VND bills");
    }

    private void viewAllAccounts(String user) {
        screen.println();
        screen.println("=== ALL CUSTOMER ACCOUNTS ===");
        screen.println(String.format("%-14s %-22s %-10s %18s",
                "Account No.", "Customer Name", "Type", "Balance"));
        screen.printSeparator();
        for (BankCustomer c : customerRepo.findAll()) {
            accountRepo.findByAccountNumber(c.getAccountNumber()).ifPresent(acc ->
                screen.println(String.format("%-14s %-22s %-10s %18s",
                        acc.getAccountNumber(), c.getCustomerName(),
                        acc.getAccountType(), FormatUtil.formatVND(acc.getAccountBalance())))
            );
        }
        adminLog.log(DateUtil.now(), user, "Viewed all accounts");
    }

    private void unblockCard(String user) {
        screen.println();
        screen.println("=== UNBLOCK CARD ===");
        screen.print("Enter Card ID to unblock: ");
        String cardId = screen.acceptInput();
        Optional<ATMCard> cardOpt = cardRepo.findById(cardId);
        if (cardOpt.isEmpty()) { screen.println("ERROR: Card not found."); return; }
        ATMCard card = cardOpt.get();
        // State pattern: isBlocked() delegates to CardState; no CardStatus import needed.
        if (!card.isBlocked()) { screen.println("Card is already ACTIVE."); return; }
        card.adminUnblock();    // State pattern: resets failedAttempts + transitions to ActiveCardState
        cardRepo.update(card);
        screen.println("Card " + card.getMaskedCardId() + " has been unblocked and set to ACTIVE.");
        adminLog.log(DateUtil.now(), user, "Unblocked card: " + cardId);
    }

    private void calculateInterest(String user) {
        screen.println();
        screen.println("=== INTEREST CALCULATION ===");
        List<String> results = interestService.calculateInterestForAll();
        results.forEach(r -> screen.println("  " + r));
        adminLog.log(DateUtil.now(), user, "Triggered interest calculation");
    }

    private void viewDenominations(String user) {
        screen.println();
        screen.println("=== DENOMINATION INVENTORY ===");
        screen.println(String.format("%-14s %10s %20s", "Denomination", "Count", "Total Value"));
        screen.printSeparator();
        long grand = 0;
        for (Map.Entry<Long, Integer> e : denomRepo.getDenominations().entrySet()) {
            long total = e.getKey() * e.getValue();
            grand += total;
            screen.println(String.format("%-14s %10d %20s",
                    FormatUtil.formatVND(e.getKey()), e.getValue(), FormatUtil.formatVND(total)));
        }
        screen.printSeparator();
        screen.println(String.format("%-14s %10s %20s", "TOTAL", "", FormatUtil.formatVND(grand)));
        adminLog.log(DateUtil.now(), user, "Viewed denomination inventory");
    }
}
