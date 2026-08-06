package com.training.atm.config.db;

import java.sql.Connection;

public class TransactionContext {
    private static final ThreadLocal<Connection> TX_CONNECTION = new ThreadLocal<>();

    public boolean isActive() {
        return TX_CONNECTION.get() != null;
    }

    public Connection get() {
        return TX_CONNECTION.get();
    }

    public void bind(Connection con) {
        TX_CONNECTION.set(con);
    }

    public void clear() {
        TX_CONNECTION.remove();
    }
}