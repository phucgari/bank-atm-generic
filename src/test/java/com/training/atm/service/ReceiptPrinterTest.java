package com.training.atm.service;

import com.training.atm.testutil.RecordingDisplayScreen;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ReceiptPrinterTest {

    private RecordingDisplayScreen screen;
    private ReceiptPrinter printer;

    @Before
    public void setUp() {
        screen = new RecordingDisplayScreen();
        printer = new ReceiptPrinter(screen);
    }

    @Test
    public void printReceiptDisplaysAllInformation() {
        Map<Long, Integer> dispensed = new LinkedHashMap<>();
        dispensed.put(100_000L, 2);
        dispensed.put(50_000L, 1);

        printer.printReceipt("2026-08-07 10:30:00", "****-****-****-1234",
                "WITHDRAWAL", 250_000, 750_000, "Main Branch", dispensed);

        assertTrue(screen.containsLine("--- RECEIPT ---"));
        assertTrue(screen.containsLine("Date"));
        assertTrue(screen.containsLine("2026-08-07 10:30:00"));
        assertTrue(screen.containsLine("Card"));
        assertTrue(screen.containsLine("****-****-****-1234"));
        assertTrue(screen.containsLine("Type"));
        assertTrue(screen.containsLine("WITHDRAWAL"));
        assertTrue(screen.containsLine("Amount"));
        assertTrue(screen.containsLine("250,000 VND"));
        assertTrue(screen.containsLine("Balance"));
        assertTrue(screen.containsLine("750,000 VND"));
        assertTrue(screen.containsLine("Branch"));
        assertTrue(screen.containsLine("Main Branch"));
        assertTrue(screen.containsLine("Bill breakdown:"));
        assertTrue(screen.containsLine("--- END RECEIPT ---"));
    }

    @Test
    public void printReceiptDisplaysBillBreakdown() {
        Map<Long, Integer> dispensed = new LinkedHashMap<>();
        dispensed.put(500_000L, 1);
        dispensed.put(100_000L, 3);
        dispensed.put(50_000L, 2);

        printer.printReceipt("2026-08-07 10:30:00", "****-1234",
                "WITHDRAWAL", 900_000, 100_000, "Branch", dispensed);

        assertTrue(screen.containsLine("Bill breakdown:"));
        assertTrue(screen.containsLine("1 x 500,000 VND"));
        assertTrue(screen.containsLine("3 x 100,000 VND"));
        assertTrue(screen.containsLine("2 x 50,000 VND"));
        assertTrue(screen.containsLine("Total bills: 6"));
    }

    @Test
    public void printReceiptHandlesZeroAmount() {
        printer.printReceipt("2026-08-07 10:30:00", "****-1234",
                "BALANCE", 0, 1_000_000, "Branch", null);

        assertFalse(screen.containsLine("Amount"));
        assertTrue(screen.containsLine("Balance"));
        assertTrue(screen.containsLine("1,000,000 VND"));
    }

    @Test
    public void printReceiptHandlesNullDispensed() {
        printer.printReceipt("2026-08-07 10:30:00", "****-1234",
                "DEPOSIT", 100_000, 600_000, "Branch", null);

        assertFalse(screen.containsLine("Bill breakdown:"));
        assertTrue(screen.containsLine("Balance"));
    }

    @Test
    public void printReceiptHandlesEmptyDispensed() {
        Map<Long, Integer> dispensed = new LinkedHashMap<>();

        printer.printReceipt("2026-08-07 10:30:00", "****-1234",
                "TRANSFER", 50_000, 450_000, "Branch", dispensed);

        assertFalse(screen.containsLine("Bill breakdown:"));
    }

    @Test
    public void printBillBreakdownDisplaysAllDenominations() {
        Map<Long, Integer> dispensed = new LinkedHashMap<>();
        dispensed.put(200_000L, 2);
        dispensed.put(100_000L, 1);

        printer.printBillBreakdown(dispensed);

        assertTrue(screen.containsLine("Bill breakdown:"));
        assertTrue(screen.containsLine("2 x 200,000 VND"));
        assertTrue(screen.containsLine("1 x 100,000 VND"));
        assertTrue(screen.containsLine("Total bills: 3"));
    }

    @Test
    public void printBillBreakdownHandlesEmptyMap() {
        Map<Long, Integer> dispensed = new LinkedHashMap<>();

        printer.printBillBreakdown(dispensed);

        assertTrue(screen.containsLine("Bill breakdown:"));
        assertTrue(screen.containsLine("Total bills: 0"));
    }
}
