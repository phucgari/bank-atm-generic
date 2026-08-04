package com.training.atm.config.db;

import org.h2.tools.RunScript;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the H2 schema and seeds it on first run.
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
            System.err.println("Error initializing H2 database: " + e.getMessage());
        }
    }

    private void runScript(Connection conn, String resource) throws SQLException {
        try (InputStream in = DatabaseInitializer.class.getResourceAsStream(resource)) {
            if (in == null) {
                System.err.println("Missing database script on classpath: " + resource);
                return;
            }
            RunScript.execute(conn, new InputStreamReader(in, StandardCharsets.UTF_8));
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

    private DatabaseInitializer() { /* utility class — no instances */ }
}
