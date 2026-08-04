package com.training.atm.util;

/**
 * Stateless input-validation helpers.
 *
 * Fix #11: declared {@code final} — utility classes should not be subclassed.
 */
public final class ValidationUtil {

    /** Returns {@code true} if {@code pin} is exactly 4 ASCII digits. */
    public static boolean isValidPin(String pin) {
        return pin != null && pin.matches("\\d{4}");
    }

    /**
     * Returns {@code true} if the PIN is trivially guessable: all identical
     * digits (e.g. "1111"), or a strictly ascending/descending run (e.g.
     * "1234", "9876").
     */
    public static boolean isWeakPin(String pin) {
        if (!isValidPin(pin)) return false;
        if (pin.chars().distinct().count() == 1) return true;

        boolean ascending  = true;
        boolean descending = true;
        for (int i = 1; i < 4; i++) {
            if (pin.charAt(i) - pin.charAt(i - 1) != 1)  ascending  = false;
            if (pin.charAt(i - 1) - pin.charAt(i) != 1)  descending = false;
        }
        return ascending || descending;
    }

    /** Returns {@code true} when {@code amount} is a positive multiple of {@code denominator}. */
    public static boolean isMultipleOf(long amount, long denominator) {
        return amount > 0 && amount % denominator == 0;
    }

    private ValidationUtil() { /* utility class — no instances */ }
}
