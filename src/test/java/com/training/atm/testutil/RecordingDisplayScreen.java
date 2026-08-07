package com.training.atm.testutil;

import java.util.ArrayList;
import java.util.List;

/**
 * Fake DisplayScreen that records all output for assertions.
 */
public class RecordingDisplayScreen extends com.training.atm.service.DisplayScreen {
    public final List<String> printed = new ArrayList<>();
    public final List<String> printedLines = new ArrayList<>();

    @Override
    public void print(String message) {
        printed.add(message);
    }

    @Override
    public void println(String message) {
        printedLines.add(message);
    }

    @Override
    public void println() {
        printedLines.add("");
    }

    @Override
    public void printSeparator() {
        printedLines.add("----------------------------------------");
    }

    @Override
    public void printDoubleSeparator() {
        printedLines.add("========================================");
    }

    public boolean containsLine(String substring) {
        return printedLines.stream().anyMatch(line -> line.contains(substring));
    }

    public boolean containsExactLine(String line) {
        return printedLines.contains(line);
    }

    public void clear() {
        printed.clear();
        printedLines.clear();
    }
}
