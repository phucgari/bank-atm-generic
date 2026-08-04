package com.training.atm.service;

import java.io.Console;
import java.util.Scanner;

/**
 * Abstracts console I/O for the ATM terminal.
 *
 * Fix #7: removed {@code prompt(String)} — it was an exact duplicate of
 * {@code print(String)}, providing no additional behaviour.
 * Fix #8: separator strings are constants, not magic literals.
 */
public class DisplayScreen {
    private static final String SEPARATOR        = "----------------------------------------";
    private static final String DOUBLE_SEPARATOR = "========================================";

    private final Scanner scanner;

    public DisplayScreen() {
        this.scanner = new Scanner(System.in);
    }

    public void print(String message)   { System.out.print(message); }
    public void println(String message) { System.out.println(message); }
    public void println()               { System.out.println(); }

    public String acceptInput() {
        return scanner.nextLine().trim();
    }

    /**
     * Reads a PIN without echoing characters when running in a real terminal.
     * Falls back to normal input in IDE or pipe environments.
     */
    public String readPin(String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] pin = console.readPassword(prompt);
            return (pin != null) ? new String(pin) : "";
        }
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public void printSeparator()       { System.out.println(SEPARATOR); }
    public void printDoubleSeparator() { System.out.println(DOUBLE_SEPARATOR); }
}
