package com.training.atm.repository;

import com.training.atm.repository.file.FileAccountRepository;
import com.training.atm.repository.file.FileATMConfigRepository;
import com.training.atm.repository.file.FileCardRepository;
import com.training.atm.repository.file.FileCustomerRepository;
import com.training.atm.repository.file.FileScheduledTransferRepository;
import com.training.atm.repository.file.FileTransactionRepository;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class RepositoryFactoryTest {
    @Test
    public void testPropertiesSelectFileRepositories() {
        RepositoryContext context = RepositoryFactory.create();

        assertTrue(context.atmInfo() instanceof FileATMConfigRepository);
        assertTrue(context.customers() instanceof FileCustomerRepository);
        assertTrue(context.cards() instanceof FileCardRepository);
        assertTrue(context.accounts() instanceof FileAccountRepository);
        assertTrue(context.transactions() instanceof FileTransactionRepository);
        assertTrue(context.scheduledTransfers() instanceof FileScheduledTransferRepository);
    }
}
