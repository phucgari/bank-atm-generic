package com.training.atm.config.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the schema and seeds it on first run using standard JDBC.
 *
 * <p>Idempotent: the schema uses {@code CREATE TABLE IF NOT EXISTS}, and the
 * seed script is only executed when the {@code accounts} table is empty.
 */
public final class DatabaseInitializer {
    private static final String SCHEMA_SCRIPT = "/db/schema.sql";
    private static final String SEED_SCRIPT   = "/db/seed.sql";

    private ConnectionManager connectionManager;

    public DatabaseInitializer(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void initialize() {
        try (Connection conn = connectionManager.getConnection()) {
            runScript(conn, SCHEMA_SCRIPT);
            if (isTableEmpty(conn, "accounts")) {
                runScript(conn, SEED_SCRIPT);
            }
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    /**
     * Reads a SQL script from the classpath and executes statements line by line,
     * handling statements split across multiple lines separated by semicolons.
     */
    private void runScript(Connection conn, String resource) throws SQLException {
        try (InputStream in = DatabaseInitializer.class.getResourceAsStream(resource);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

            if (in == null) {
                System.err.println("Missing database script on classpath: " + resource);
                return;
            }

            StringBuilder sqlBuilder = new StringBuilder();
            String line;

            try (Statement statement = conn.createStatement()) {
                while ((line = reader.readLine()) != null) {
                    String trimmedLine = line.trim();

                    // Skip SQL comments and empty lines
                    if (trimmedLine.startsWith("--") || trimmedLine.isEmpty()) {
                        continue;
                    }

                    sqlBuilder.append(line).append("\n");

                    // If the line ends with a semicolon, execute the accumulated statement
                    if (trimmedLine.endsWith(";")) {
                        String sql = sqlBuilder.toString().trim();
                        statement.execute(sql);
                        sqlBuilder.setLength(0); // Reset for the next statement
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading database script " + resource + ": " + e.getMessage());
        }
    }

    private boolean isTableEmpty(Connection conn, String table) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getLong(1) == 0;
        }
    }
}