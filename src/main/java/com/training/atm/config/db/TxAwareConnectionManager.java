package com.training.atm.config.db;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

public class TxAwareConnectionManager implements ConnectionManager {

    private final ConnectionManager delegate;      // your PooledConnectionManager
    private final TransactionContext txContext;

    public TxAwareConnectionManager(ConnectionManager delegate, TransactionContext txContext) {
        this.delegate = delegate;
        this.txContext = txContext;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (!txContext.isActive()) {
            // No transaction: normal behavior (close returns to pool)
            return delegate.getConnection();
        }

        // Transaction active: return a proxy over the tx-bound connection
        Connection txCon = txContext.get();
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("close")) {
                        // Do NOT release to pool; tx manager will close at the end
                        return null;
                    }
                    return method.invoke(txCon, args);
                }
        );
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }
}