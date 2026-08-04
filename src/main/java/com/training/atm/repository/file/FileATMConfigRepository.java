package com.training.atm.repository.file;

import com.training.atm.config.TransactionLimits;
import com.training.atm.repository.ATMInfoRepository;
import com.training.atm.repository.DataDirectory;
import com.training.atm.repository.DenominationRepository;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unified text-file implementation of both {@link ATMInfoRepository} and
 * {@link DenominationRepository}.
 *
 * Both concern ATM hardware configuration and share the same data files, but
 * they are exposed to consumers through two separate interfaces (ISP):
 * - Components needing only ATM metadata depend on {@link ATMInfoRepository}.
 * - Components needing cash management depend on {@link DenominationRepository}.
 *
 * atm.txt format:    location|branchName
 * denominations.txt: denomination|count  (one per line)
 *
 * Fix #17: replaced the magic literal {@code 50_000L} in {@code addDepositCash()}
 * with the named constant {@code DEPOSIT_INTAKE_DENOMINATION}, documented below.
 */
public class FileATMConfigRepository implements ATMInfoRepository, DenominationRepository {
    private static final String ATM_FILE   = DataDirectory.getPath("atm.txt");
    private static final String DENOM_FILE = DataDirectory.getPath("denominations.txt");
    private static final long   MAX_CAPACITY = 500_000_000L;

    /** Denominations available for dispensing, ordered largest-first. */
    private static final long[] DENOM_ORDER = {500_000L, 200_000L, 100_000L, 50_000L};

    /**
     * The denomination slot that receives deposited cash.
     * The ATM's smallest cassette denomination is 50,000 VND; all deposits
     * are credited to that slot (deposited bills of smaller face value are
     * equivalent in inventory value).
     *
     * Fix #17: this was previously a naked {@code 50_000L} magic literal.
     * Using {@link TransactionLimits#WITHDRAWAL_DENOMINATION} here would be
     * semantically wrong (withdrawal and intake are different concepts), so
     * the constant lives in this class which owns the denomination model.
     */
    private static final long DEPOSIT_INTAKE_DENOMINATION = 50_000L;

    private String location;
    private String branchName;
    private final Map<Long, Integer> denominations = new LinkedHashMap<>();

    public FileATMConfigRepository() {
        loadAtm();
        loadDenominations();
    }

    private void loadAtm() {
        File f = new File(ATM_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line = br.readLine();
            if (line != null) {
                String[] p = line.trim().split("\\|", -1);
                if (p.length >= 2) { location = p[0]; branchName = p[1]; }
            }
        } catch (IOException e) {
            System.err.println("Error loading ATM config: " + e.getMessage());
        }
    }

    private void loadDenominations() {
        for (long d : DENOM_ORDER) denominations.put(d, 0);
        File f = new File(DENOM_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\|");
                if (p.length >= 2) denominations.put(Long.parseLong(p[0]), Integer.parseInt(p[1]));
            }
        } catch (IOException e) {
            System.err.println("Error loading denominations: " + e.getMessage());
        }
    }

    private void saveDenominations() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(DENOM_FILE))) {
            for (long d : DENOM_ORDER) pw.println(d + "|" + denominations.getOrDefault(d, 0));
        } catch (IOException e) {
            System.err.println("Error saving denominations: " + e.getMessage());
        }
    }

    // --- ATMInfoRepository ---
    @Override public String getLocation()   { return location; }
    @Override public String getBranchName() { return branchName; }
    @Override public long   getMaxCapacity(){ return MAX_CAPACITY; }

    // --- DenominationRepository ---
    @Override public long getTotalCash() {
        return denominations.entrySet().stream().mapToLong(e -> e.getKey() * e.getValue()).sum();
    }

    @Override public Map<Long, Integer> getDenominations() {
        return new LinkedHashMap<>(denominations);
    }

    @Override public boolean isValidDenomination(long denom) {
        for (long d : DENOM_ORDER) if (d == denom) return true;
        return false;
    }

    @Override public void dispenseBills(Map<Long, Integer> dispensed) {
        dispensed.forEach((denom, count) -> denominations.merge(denom, -count, Integer::sum));
        saveDenominations();
    }

    @Override public void addDepositCash(long amount) {
        int billCount = (int) (amount / DEPOSIT_INTAKE_DENOMINATION);
        denominations.merge(DEPOSIT_INTAKE_DENOMINATION, billCount, Integer::sum);
        saveDenominations();
    }

    @Override public boolean replenish(long denom, int count) {
        if (!isValidDenomination(denom)) return false;
        if (getTotalCash() + denom * count > MAX_CAPACITY) return false;
        denominations.merge(denom, count, Integer::sum);
        saveDenominations();
        return true;
    }
}
