package com.training.atm.config.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;

public class JdbcTransactionManager implements TransactionManager {

    private final ConnectionManager connectionManager; // IMPORTANT: use the *delegate pooled* manager here
    private final TransactionContext txContext;

    public JdbcTransactionManager(ConnectionManager pooledManager, TransactionContext txContext) {
        this.connectionManager = pooledManager;
        this.txContext = txContext;
    }

    @Override
    public <T> T executeInTransaction(Callable<T> cb) {
        if (txContext.isActive()) {
            throw new IllegalStateException("Nested transactions not supported.");
        }

        try (Connection con = connectionManager.getConnection()) {
            boolean oldAutoCommit = con.getAutoCommit();
            con.setAutoCommit(false);

            txContext.bind(con);
            try {
                T result = cb.call();
                con.commit();
                return result;
            } catch (Throwable failure) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    failure.addSuppressed(ex);
                }
                if (failure instanceof Error error) {
                    throw error;
                }
                if (failure instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new RuntimeException(failure);
            } finally {
                txContext.clear();
                try {
                    con.setAutoCommit(oldAutoCommit);
                } catch (SQLException ignore) {
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to start transaction", e);
        }
    }
}