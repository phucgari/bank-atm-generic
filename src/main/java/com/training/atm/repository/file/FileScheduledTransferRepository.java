package com.training.atm.repository.file;

import com.training.atm.model.ScheduledTransfer;
import com.training.atm.model.enums.TransferFrequency;
import com.training.atm.model.enums.TransferStatus;
import com.training.atm.model.state.TransferLifecycleState;
import com.training.atm.repository.DataDirectory;
import com.training.atm.repository.ScheduledTransferRepository;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Text-file implementation of {@link ScheduledTransferRepository}.
 *
 * State pattern: the persisted {@link TransferStatus} enum value is used only
 * as a serialisation token.  On load, {@link ScheduledTransfer#stateFrom(TransferStatus)}
 * converts it back to the appropriate {@link TransferLifecycleState} object.
 */
public class FileScheduledTransferRepository implements ScheduledTransferRepository {
    private static final String FILE = DataDirectory.getPath("scheduled_transfers.txt");
    private final Map<String, ScheduledTransfer> transfers = new LinkedHashMap<>();

    public FileScheduledTransferRepository() { load(); }

    private void load() {
        File f = new File(FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\|", -1);
                if (p.length < 10) continue;
                TransferLifecycleState state =
                        ScheduledTransfer.stateFrom(TransferStatus.valueOf(p[6])); // State pattern
                ScheduledTransfer st = new ScheduledTransfer(
                        p[0], p[1], p[2], Long.parseLong(p[3]),
                        TransferFrequency.valueOf(p[4]), p[5],
                        state,
                        Integer.parseInt(p[7]), Integer.parseInt(p[8]), p[9]);
                transfers.put(st.getId(), st);
            }
        } catch (IOException e) {
            System.err.println("Error loading scheduled transfers: " + e.getMessage());
        }
    }

    private void saveAll() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
            for (ScheduledTransfer st : transfers.values()) {
                pw.println(st.getId() + "|" + st.getSourceAccount() + "|"
                        + st.getDestAccount() + "|" + st.getAmount() + "|"
                        + st.getFrequency() + "|" + st.getNextExecutionDate() + "|"
                        + st.getStatus() + "|" + st.getMaxRepeat() + "|"    // getStatus() calls state.toStatus()
                        + st.getRepeatCount() + "|" + st.getEndDate());
            }
        } catch (IOException e) {
            System.err.println("Error saving scheduled transfers: " + e.getMessage());
        }
    }

    @Override public void save(ScheduledTransfer st)   { update(st); }
    @Override public void update(ScheduledTransfer st) { transfers.put(st.getId(), st); saveAll(); }

    @Override public Optional<ScheduledTransfer> findById(String id) {
        return Optional.ofNullable(transfers.get(id));
    }

    @Override public List<ScheduledTransfer> findBySourceAccount(String accountNumber) {
        return transfers.values().stream()
                .filter(st -> st.getSourceAccount().equals(accountNumber))
                .collect(Collectors.toList());
    }

    @Override public List<ScheduledTransfer> findAllActive() {
        return transfers.values().stream()
                .filter(ScheduledTransfer::canExecute)   // delegates to state
                .collect(Collectors.toList());
    }

    @Override public long countActiveBySourceAccount(String accountNumber) {
        return transfers.values().stream()
                .filter(st -> st.getSourceAccount().equals(accountNumber) && st.canExecute())
                .count();
    }
}
