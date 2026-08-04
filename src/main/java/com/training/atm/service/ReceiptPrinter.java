package com.training.atm.service;

import com.training.atm.util.DenominationDispenser;
import com.training.atm.util.FormatUtil;

import java.util.Map;

/**
 * Prints receipts and bill breakdowns to the console.
 *
 * SRP: receipt formatting was previously mixed into {@link CashDispenser}.
 * CashDispenser's responsibility is cash management; ReceiptPrinter's
 * responsibility is display formatting — they change for different reasons.
 *
 * Fix #19: bill-breakdown printing was duplicated between {@code printReceipt}
 * and {@code printBillBreakdown}.  Extracted into the private helper
 * {@code printBillLines} so the format is defined exactly once.
 */
public class ReceiptPrinter {
    private final DisplayScreen screen;

    public ReceiptPrinter(DisplayScreen screen) {
        this.screen = screen;
    }

    public void printReceipt(String dateTime, String maskedCard, String type,
                              long amount, long balance, String branch,
                              Map<Long, Integer> dispensed) {
        screen.println("--- RECEIPT ---");
        screen.println(String.format("%-12s: %s", "Date",    dateTime));
        screen.println(String.format("%-12s: %s", "Card",    maskedCard));
        screen.println(String.format("%-12s: %s", "Type",    type));
        if (amount > 0) {
            screen.println(String.format("%-12s: %s", "Amount", FormatUtil.formatVND(amount)));
        }
        if (dispensed != null && !dispensed.isEmpty()) {
            screen.println("Bill breakdown:");
            printBillLines(dispensed, "  ");
        }
        screen.println(String.format("%-12s: %s", "Balance", FormatUtil.formatVND(balance)));
        screen.println(String.format("%-12s: %s", "Branch",  branch));
        screen.println("--- END RECEIPT ---");
    }

    public void printBillBreakdown(Map<Long, Integer> dispensed) {
        screen.println("  Bill breakdown:");
        printBillLines(dispensed, "    ");
        screen.println("  Total bills: " + DenominationDispenser.totalBills(dispensed));
    }

    // Fix #19: single private method owns the per-denomination line format.
    private void printBillLines(Map<Long, Integer> dispensed, String indent) {
        dispensed.forEach((denom, count) ->
                screen.println(indent + count + " x " + FormatUtil.formatVND(denom)));
        screen.println(indent + "Total bills: " + DenominationDispenser.totalBills(dispensed));
    }
}
