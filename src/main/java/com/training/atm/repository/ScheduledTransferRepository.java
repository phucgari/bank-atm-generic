package com.training.atm.repository;

import com.training.atm.model.ScheduledTransfer;

import java.util.List;
import java.util.Optional;

/** Repository abstraction for ScheduledTransfer persistence. */
public interface ScheduledTransferRepository {
    void save(ScheduledTransfer scheduledTransfer);
    void update(ScheduledTransfer scheduledTransfer);
    Optional<ScheduledTransfer> findById(String id);
    List<ScheduledTransfer> findBySourceAccount(String accountNumber);
    List<ScheduledTransfer> findAllActive();
    long countActiveBySourceAccount(String accountNumber);
}
