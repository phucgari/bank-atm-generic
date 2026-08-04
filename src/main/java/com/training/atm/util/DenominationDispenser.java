package com.training.atm.util;

import java.util.*;

/**
 * Greedy-first DFS algorithm that finds the minimum-bill combination for a
 * given withdrawal amount, respecting per-denomination availability limits.
 *
 * OCP fix: denomination values are no longer hardcoded in this class.
 * They are derived from the keys of the {@code available} map passed by the
 * caller, sorted largest-first.  Adding a new denomination to the ATM only
 * requires updating the repository data; this class needs no modification.
 */
public final class DenominationDispenser {
    public static final int MAX_BILLS_PER_TRANSACTION = 50;

    /**
     * Computes the optimal (minimum-bill) combination of denominations to
     * dispense {@code amount} VND.
     *
     * @param amount    exact amount to dispense (must be a multiple of the smallest denomination)
     * @param available map of denomination → available bill count (sorted or unsorted)
     * @return map of denomination → bills used, or {@code null} if dispensing is impossible
     */
    public static Map<Long, Integer> calculate(long amount, Map<Long, Integer> available) {
        // Sort denominations descending so the greedy approach tries large bills first.
        long[] denoms = available.keySet().stream()
                .mapToLong(Long::longValue)
                .boxed()
                .sorted(Comparator.reverseOrder())
                .mapToLong(Long::longValue)
                .toArray();

        int[] avail = new int[denoms.length];
        for (int i = 0; i < denoms.length; i++) {
            avail[i] = available.getOrDefault(denoms[i], 0);
        }

        int[] result = new int[denoms.length];
        if (!solve(amount, denoms, avail, 0, result, 0)) return null;

        Map<Long, Integer> dispensed = new LinkedHashMap<>();
        for (int i = 0; i < denoms.length; i++) {
            if (result[i] > 0) dispensed.put(denoms[i], result[i]);
        }
        return dispensed;
    }

    /** Recursive DFS: tries max-to-zero bills of denomination[idx], greedy-first. */
    private static boolean solve(long remaining, long[] denoms, int[] avail,
                                  int idx, int[] result, int billsUsed) {
        if (remaining == 0) return true;
        if (idx >= denoms.length || billsUsed >= MAX_BILLS_PER_TRANSACTION) return false;

        long denom = denoms[idx];
        int maxUse = (int) Math.min(avail[idx], remaining / denom);
        maxUse = Math.min(maxUse, MAX_BILLS_PER_TRANSACTION - billsUsed);

        for (int use = maxUse; use >= 0; use--) {
            result[idx] = use;
            if (solve(remaining - (long) use * denom, denoms, avail, idx + 1, result, billsUsed + use)) {
                return true;
            }
        }
        result[idx] = 0;
        return false;
    }

    public static int totalBills(Map<Long, Integer> dispensed) {
        return dispensed.values().stream().mapToInt(Integer::intValue).sum();
    }

    private DenominationDispenser() { /* utility class — no instances */ }
}
