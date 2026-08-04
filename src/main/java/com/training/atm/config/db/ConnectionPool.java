package com.training.atm.config.db;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionPool {
    Connection getConnection() throws SQLException, InterruptedException;
    void releaseConnection(Connection connection);
    void shutdown();
}