package com.training.atm.config.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class H2ConnectionPool extends DatabaseConfig implements ConnectionPool {

    private final BlockingQueue<Connection> pool;
    private final Vector<Connection> allConnections;

    public H2ConnectionPool(DatabaseConfig databaseConfig) {
        super(databaseConfig);
        this.pool = new ArrayBlockingQueue<>(poolSize);
        this.allConnections = new Vector<>();

        try {
            initializePool();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializePool() throws SQLException {
        for (int i = 0; i < poolSize; i++) {
            Connection connection = DriverManager.getConnection(url, user, password);
            allConnections.add(connection);
            pool.offer(connection);
        }
    }

    @Override
    public Connection getConnection() throws SQLException, InterruptedException {
        return pool.take(); 
    }

    @Override
    public void releaseConnection(Connection connection) {
        if (connection != null) {
            pool.offer(connection);
        }
    }

    @Override
    public void shutdown() {
        for (Connection conn : allConnections) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}