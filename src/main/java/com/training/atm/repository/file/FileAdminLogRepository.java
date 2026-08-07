package com.training.atm.repository.file;

import com.training.atm.repository.AdminLogRepository;

import java.io.*;

/** Append-only text-file implementation of {@link AdminLogRepository}. Format: timestamp|adminUser|action */
public class FileAdminLogRepository implements AdminLogRepository {
    private static final String FILE = DataDirectory.getPath("admin_log.txt");

    @Override
    public void log(String timestamp, String adminUser, String action) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE, true))) {
            pw.println(timestamp + "|" + adminUser + "|" + action);
        } catch (IOException e) {
            System.err.println("Error writing admin log: " + e.getMessage());
        }
    }
}
