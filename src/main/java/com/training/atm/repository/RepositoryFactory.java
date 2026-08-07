package com.training.atm.repository;

import com.training.atm.config.ApplicationProperties;
import com.training.atm.config.db.*;
import com.training.atm.repository.db.*;
import com.training.atm.repository.file.*;

import java.util.concurrent.Callable;

public final class RepositoryFactory {
    private RepositoryFactory() {}

    public static RepositoryContext create() {
        String storage = new ApplicationProperties().get("storage.mode", "db").trim().toLowerCase();
        return switch (storage) {
            case "file" -> createFileContext();
            case "db", "database", "jdbc" -> createDatabaseContext();
            default -> throw new IllegalArgumentException(
                    "Unsupported storage.mode '" + storage + "'. Expected 'file' or 'db'.");
        };
    }

    private static RepositoryContext createFileContext() {
        DataDirectory.ensureExists();
        FileATMConfigRepository atm = new FileATMConfigRepository();
        return new RepositoryContext(
                atm, atm,
                new FileCustomerRepository(),
                new FileCardRepository(),
                new FileAccountRepository(),
                new FileTransactionRepository(),
                new FileScheduledTransferRepository(),
                new FileAdminLogRepository(),
                RepositoryFactory::executeWithoutDatabaseTransaction);
    }

    private static RepositoryContext createDatabaseContext() {
        DatabaseConfig databaseConfig = new DatabaseConfig();
        ConnectionPool connectionPool = new JdbcConnectionPool(databaseConfig);
        ConnectionManager pooledConnectionManager = new PooledConnectionManager(connectionPool);
        TransactionContext transactionContext = new TransactionContext();
        ConnectionManager connectionManager =
                new TxAwareConnectionManager(pooledConnectionManager, transactionContext);
        TransactionManager transactionManager =
                new JdbcTransactionManager(pooledConnectionManager, transactionContext);
        new DatabaseInitializer(connectionManager).initialize();

        JdbcATMConfigRepository atm = new JdbcATMConfigRepository(connectionManager);
        return new RepositoryContext(
                atm, atm,
                new JdbcCustomerRepository(connectionManager),
                new JdbcCardRepository(connectionManager),
                new JdbcAccountRepository(connectionManager),
                new JdbcTransactionRepository(connectionManager),
                new JdbcScheduledTransferRepository(connectionManager),
                new JdbcAdminLogRepository(connectionManager),
                transactionManager);
    }

    private static <T> T executeWithoutDatabaseTransaction(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new IllegalStateException("File repository operation failed", e);
        }
    }
}
