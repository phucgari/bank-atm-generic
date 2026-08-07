package com.training.atm.service;

import com.training.atm.testutil.RecordingDisplayScreen;
import com.training.atm.testutil.TestRepositories.InMemoryATMInfoRepository;
import org.junit.Test;

import static org.junit.Assert.*;

public class ATMTest {

    @Test
    public void showDisplaysATMInformationWithSeparators() {
        InMemoryATMInfoRepository atmInfo = new InMemoryATMInfoRepository("Downtown", "Main Branch");
        RecordingDisplayScreen screen = new RecordingDisplayScreen();
        ATM atm = new ATM(atmInfo);

        atm.show(screen);

        assertTrue(screen.containsLine("BANK ATM SYSTEM"));
        assertTrue(screen.containsLine("Location: Downtown"));
        assertTrue(screen.containsLine("Branch:   Main Branch"));
        assertTrue(screen.containsExactLine("========================================"));
        assertEquals(5, screen.printedLines.size());
    }

    @Test
    public void showDisplaysCorrectSeparatorFormat() {
        InMemoryATMInfoRepository atmInfo = new InMemoryATMInfoRepository("", "");
        RecordingDisplayScreen screen = new RecordingDisplayScreen();
        ATM atm = new ATM(atmInfo);

        atm.show(screen);

        assertEquals("========================================", screen.printedLines.get(0));
        assertEquals("========================================", screen.printedLines.get(4));
    }

    @Test
    public void showHandlesEmptyLocationAndBranch() {
        InMemoryATMInfoRepository atmInfo = new InMemoryATMInfoRepository("", "");
        RecordingDisplayScreen screen = new RecordingDisplayScreen();
        ATM atm = new ATM(atmInfo);

        atm.show(screen);

        assertTrue(screen.containsLine("Location: "));
        assertTrue(screen.containsLine("Branch:   "));
    }
}
