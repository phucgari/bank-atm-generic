package com.training.atm.util;

import com.training.atm.model.enums.TransferFrequency;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Central date/time utility.
 * Moved out of ATMService (SRP: date formatting is a separate concern from
 * transaction business logic).
 */
public final class DateUtil {
    public static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter YM_FMT    = DateTimeFormatter.ofPattern("yyyy-MM");

    public static String now()              { return LocalDateTime.now().format(DT_FMT); }
    public static String today()            { return LocalDate.now().format(DATE_FMT); }
    public static String currentYearMonth() { return LocalDate.now().format(YM_FMT); }

    public static String generateTxId() {
        return "TX" + System.currentTimeMillis();
    }

    public static String advanceDate(String dateStr, TransferFrequency freq) {
        LocalDate d = LocalDate.parse(dateStr, DATE_FMT);
        return switch (freq) {
            case DAILY   -> d.plusDays(1).format(DATE_FMT);
            case WEEKLY  -> d.plusWeeks(1).format(DATE_FMT);
            case MONTHLY -> d.plusMonths(1).format(DATE_FMT);
            default      -> dateStr;
        };
    }

    private DateUtil() { /* utility class — no instances */ }
}
