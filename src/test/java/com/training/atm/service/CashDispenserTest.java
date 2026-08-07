package com.training.atm.service;

import com.training.atm.testutil.TestRepositories.InMemoryDenominationRepository;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class CashDispenserTest {

    private InMemoryDenominationRepository denomRepo;
    private CashDispenser dispenser;

    @Before
    public void setUp() {
        denomRepo = new InMemoryDenominationRepository();
        dispenser = new CashDispenser(denomRepo);
    }

    @Test
    public void getAvailableCashReturnsZeroWhenEmpty() {
        assertEquals(0, dispenser.getAvailableCash());
    }

    @Test
    public void getAvailableCashReturnsTotalOfAllDenominations() {
        denomRepo.denominations.put(500_000L, 10);
        denomRepo.denominations.put(100_000L, 20);
        denomRepo.denominations.put(50_000L, 30);

        assertEquals(8_500_000L, dispenser.getAvailableCash());
    }

    @Test
    public void hasSufficientCashReturnsTrueWhenEnoughCash() {
        denomRepo.denominations.put(100_000L, 10);

        assertTrue(dispenser.hasSufficientCash(500_000));
        assertTrue(dispenser.hasSufficientCash(1_000_000));
    }

    @Test
    public void hasSufficientCashReturnsFalseWhenNotEnoughCash() {
        denomRepo.denominations.put(100_000L, 5);

        assertFalse(dispenser.hasSufficientCash(600_000));
    }

    @Test
    public void dispenseCashReturnsBreakdownAndReducesDenominations() {
        denomRepo.denominations.put(100_000L, 10);
        denomRepo.denominations.put(50_000L, 10);

        Map<Long, Integer> dispensed = dispenser.dispenseCash(200_000);

        assertNotNull(dispensed);
        assertEquals(1, dispensed.size());
        assertEquals(8, denomRepo.denominations.get(100_000L).intValue());
    }

    @Test
    public void dispenseCashReturnsNullWhenCannotDispenseExactAmount() {
        denomRepo.denominations.put(100_000L, 3);

        Map<Long, Integer> dispensed = dispenser.dispenseCash(150_000);

        assertNull(dispensed);
        assertEquals(3, denomRepo.denominations.get(100_000L).intValue());
    }

    @Test
    public void dispenseCashPrefersLargerDenominations() {
        denomRepo.denominations.put(500_000L, 5);
        denomRepo.denominations.put(100_000L, 10);

        Map<Long, Integer> dispensed = dispenser.dispenseCash(600_000);

        assertNotNull(dispensed);
        assertEquals(1, dispensed.get(500_000L).intValue());
        assertEquals(1, dispensed.get(100_000L).intValue());
    }

    @Test
    public void acceptAmountIncreasesDepositedAmount() {
        dispenser.acceptAmount(100_000);
        dispenser.acceptAmount(50_000);

        assertEquals(150_000, denomRepo.depositedAmount);
    }

    @Test
    public void acceptAmountDoesNotAffectAvailableCash() {
        denomRepo.denominations.put(100_000L, 5);
        long initialCash = dispenser.getAvailableCash();

        dispenser.acceptAmount(200_000);

        assertEquals(initialCash, dispenser.getAvailableCash());
        assertEquals(200_000, denomRepo.depositedAmount);
    }
}
