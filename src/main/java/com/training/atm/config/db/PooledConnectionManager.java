package com.training.atm.config.db;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

public class PooledConnectionManager implements ConnectionManager {

    private final ConnectionPool connectionPool;

    public PooledConnectionManager(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            // Borrow the raw connection from the custom pool
            Connection rawConnection = connectionPool.getConnection();

            // Wrap the raw connection in a dynamic proxy
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[] { Connection.class },
                    (proxy, method, args) -> {
                        // Intercept the close() method to return the connection to the pool instead
                        if (method.getName().equals("close")) {
                            connectionPool.releaseConnection(rawConnection);
                            return null; // close() returns void
                        }

                        // Delegate all other method calls to the actual underlying Connection
                        try {
                            return method.invoke(rawConnection, args);
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            // Unpack the underlying exception thrown by the driver
                            throw e.getTargetException();
                        }
                    }
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Thread was interrupted while waiting for a database connection.", e);
        }
    }

    @Override
    public void shutdown() {
        if (connectionPool != null) {
            connectionPool.shutdown();
        }
    }
}