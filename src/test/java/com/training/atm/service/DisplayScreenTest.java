package com.training.atm.service;

import com.training.atm.testutil.RecordingDisplayScreen;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DisplayScreenTest {

    private RecordingDisplayScreen screen;

    @Before
    public void setUp() {
        screen = new RecordingDisplayScreen();
    }

    @Test
    public void printAddsToOutputWithoutNewline() {
        screen.print("Hello ");
        screen.print("World");

        assertEquals(2, screen.printed.size());
        assertEquals("Hello ", screen.printed.get(0));
        assertEquals("World", screen.printed.get(1));
    }

    @Test
    public void printlnAddsToOutputWithNewline() {
        screen.println("First line");
        screen.println("Second line");

        assertEquals(2, screen.printedLines.size());
        assertEquals("First line", screen.printedLines.get(0));
        assertEquals("Second line", screen.printedLines.get(1));
    }

    @Test
    public void printlnEmptyAddsEmptyLine() {
        screen.println();

        assertEquals(1, screen.printedLines.size());
        assertEquals("", screen.printedLines.get(0));
    }

    @Test
    public void printSeparatorDisplaysCorrectLength() {
        screen.printSeparator();

        assertEquals(1, screen.printedLines.size());
        assertEquals("----------------------------------------", screen.printedLines.get(0));
        assertEquals(40, screen.printedLines.get(0).length());
    }

    @Test
    public void printDoubleSeparatorDisplaysCorrectFormat() {
        screen.printDoubleSeparator();

        assertEquals(1, screen.printedLines.size());
        assertEquals("========================================", screen.printedLines.get(0));
        assertEquals(40, screen.printedLines.get(0).length());
    }

    @Test
    public void containsLineReturnsTrueForSubstring() {
        screen.println("This is a test message");

        assertTrue(screen.containsLine("test message"));
        assertTrue(screen.containsLine("This is"));
        assertFalse(screen.containsLine("not present"));
    }

    @Test
    public void containsExactLineReturnsTrueForExactMatch() {
        screen.println("Exact line");

        assertTrue(screen.containsExactLine("Exact line"));
        assertFalse(screen.containsExactLine("Exact"));
        assertFalse(screen.containsExactLine("Exact line "));
    }

    @Test
    public void clearRemovesAllOutput() {
        screen.print("Print output");
        screen.println("Line output");

        screen.clear();

        assertEquals(0, screen.printed.size());
        assertEquals(0, screen.printedLines.size());
    }
}
