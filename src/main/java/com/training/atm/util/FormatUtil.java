package com.training.atm.util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility methods for formatting amounts in Vietnamese Dong (VND).
 *
 * Fix #10: declared {@code final} — utility classes should not be subclassed.
 * Fix #9: removed {@code separator(int)} and {@code doubleSeparator(int)} —
 *         dead code never called from anywhere in the codebase.
 *         Separator strings live in {@link com.training.atm.service.DisplayScreen}
 *         which is the only class responsible for console output formatting.
 */
public final class FormatUtil {
    // NumberFormat is not thread-safe; acceptable here for a single-threaded
    // console application.
    private static final NumberFormat VND_FORMAT =
            NumberFormat.getInstance(Locale.of("vi", "VN"));

    public static String formatVND(long amount) {
        return VND_FORMAT.format(amount) + " VND";
    }

    public static String formatAmount(long amount) {
        return VND_FORMAT.format(amount);
    }

    private FormatUtil() { /* utility class — no instances */ }
}
