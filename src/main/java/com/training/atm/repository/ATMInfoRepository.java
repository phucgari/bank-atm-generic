package com.training.atm.repository;

/**
 * Read-only view of ATM configuration (location and branch).
 * Segregated from denomination management (ISP) so that components
 * that only need ATM metadata (e.g. ATM display, receipt) do not depend
 * on the cash-inventory interface.
 */
public interface ATMInfoRepository {
    String getLocation();
    String getBranchName();
    long getMaxCapacity();
}
