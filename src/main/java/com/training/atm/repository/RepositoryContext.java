package com.training.atm.repository;

import com.training.atm.config.db.TransactionManager;

public record RepositoryContext(
        ATMInfoRepository atmInfo,
        DenominationRepository denominations,
        CustomerRepository customers,
        CardRepository cards,
        AccountRepository accounts,
        TransactionRepository transactions,
        ScheduledTransferRepository scheduledTransfers,
        AdminLogRepository adminLogs,
        TransactionManager transactionManager
) {}
