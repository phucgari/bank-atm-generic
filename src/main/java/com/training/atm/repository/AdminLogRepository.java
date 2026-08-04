package com.training.atm.repository;

/** Repository abstraction for admin audit-log persistence. */
public interface AdminLogRepository {
    void log(String timestamp, String adminUser, String action);
}
