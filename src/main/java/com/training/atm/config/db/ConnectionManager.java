package com.training.atm.config.db;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionManager {
    /**
     * Retrieves a database connection.
     * 
     * @return a valid active database Connection
     * @throws SQLException if a database access error occurs
     */
    Connection getConnection() throws SQLException;

    /**
     * Gracefully closes or shuts down the manager/pool resources.
     */
    void shutdown();
}