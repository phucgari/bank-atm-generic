package com.training.atm.repository.file;

import com.training.atm.model.Transaction;
import com.training.atm.model.enums.TransactionType;
import com.training.atm.repository.DataDirectory;
import com.training.atm.repository.TransactionRepository;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/** Text-file implementation of {@link TransactionRepository}. Format: txId|accountNumber|dateTime|type|amount|balanceAfter|description */
public class FileTransactionRepository implements TransactionRepository {
    private static final String FILE = DataDirectory.getPath("transactions.txt");
    private final List<Transaction> transactions = new ArrayList<>();

    public FileTransactionRepository() { load(); }

    private void load() {
        File f = new File(FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\|", -1);
                if (p.length < 7) continue;
                transactions.add(new Transaction(p[0], p[1], p[2],
                        TransactionType.valueOf(p[3]), Long.parseLong(p[4]),
                        Long.parseLong(p[5]), p[6]));
            }
        } catch (IOException e) {
            System.err.println("Error loading transactions: " + e.getMessage());
        }
    }

    @Override
    public void save(Transaction t) {
        transactions.add(t);
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE, true))) {
            pw.println(t.getTransactionId() + "|" + t.getAccountNumber() + "|" +
                    t.getDateTime() + "|" + t.getType() + "|" +
                    t.getAmount() + "|" + t.getBalanceAfter() + "|" + t.getDescription());
        } catch (IOException e) {
            System.err.println("Error saving transaction: " + e.getMessage());
        }
    }

    @Override
    public List<Transaction> findByAccountNumber(String accountNumber) {
        return transactions.stream()
                .filter(t -> t.getAccountNumber().equals(accountNumber))
                .collect(Collectors.toList());
    }

    @Override
    public long sumByAccountNumberTypeAndDate(String accountNumber, TransactionType type, String date) {
        return transactions.stream()
                .filter(t -> t.getAccountNumber().equals(accountNumber)
                        && t.getType() == type
                        && t.getDateTime().startsWith(date))
                .mapToLong(Transaction::getAmount)
                .sum();
    }
}
