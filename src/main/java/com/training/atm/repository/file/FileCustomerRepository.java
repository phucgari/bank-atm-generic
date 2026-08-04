package com.training.atm.repository.file;

import com.training.atm.model.BankCustomer;
import com.training.atm.repository.CustomerRepository;
import com.training.atm.repository.DataDirectory;

import java.io.*;
import java.util.*;

/** Text-file implementation of {@link CustomerRepository}. Format: customerId|name|address|email|cardId|accountNumber */
public class FileCustomerRepository implements CustomerRepository {
    private static final String FILE = DataDirectory.getPath("customers.txt");
    private final Map<String, BankCustomer> byCustomerId    = new LinkedHashMap<>();
    private final Map<String, BankCustomer> byCardId        = new LinkedHashMap<>();
    private final Map<String, BankCustomer> byAccountNumber = new LinkedHashMap<>();

    public FileCustomerRepository() { load(); }

    private void load() {
        File f = new File(FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\|", -1);
                if (p.length < 6) continue;
                BankCustomer c = new BankCustomer(p[0], p[1], p[2], p[3], p[4], p[5]);
                byCustomerId.put(c.getCustomerId(), c);
                byCardId.put(c.getCardId(), c);
                byAccountNumber.put(c.getAccountNumber(), c);
            }
        } catch (IOException e) {
            System.err.println("Error loading customers: " + e.getMessage());
        }
    }

    @Override public Optional<BankCustomer> findByCardId(String cardId) {
        return Optional.ofNullable(byCardId.get(cardId));
    }

    @Override public Optional<BankCustomer> findByAccountNumber(String accountNumber) {
        return Optional.ofNullable(byAccountNumber.get(accountNumber));
    }

    @Override public List<BankCustomer> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(byCustomerId.values()));
    }
}
