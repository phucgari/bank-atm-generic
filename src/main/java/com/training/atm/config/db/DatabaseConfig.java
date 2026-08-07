package com.training.atm.config.db;

import com.training.atm.config.ApplicationProperties;

/**
 * database configuration running
 * ({@code MODE=MySQL} in the JDBC URL).
 *
 * <p>MySQL-mode settings used:
 * <ul>
 *   <li>{@code DATABASE_TO_LOWER=TRUE} — MySQL lowers unquoted identifiers.</li>
 *   <li>{@code CASE_INSENSITIVE_IDENTIFIERS=TRUE} — case-insensitive object names.</li>
 *   <li>{@code DB_CLOSE_DELAY=-1} — keep the engine open for the JVM lifetime.</li>
 *   <li>{@code NON_KEYWORDS=action,type,status} — allow these MySQL-style column names.</li>
 * </ul>
 */
public class DatabaseConfig {
    private static final String DEFAULT_USER = "appuser";
    private static final String DEFAULT_PASSWORD = "apppass";
    private static final int DEFAULT_POOL_SIZE = 3;
    private static final String DEFAULT_URL ="jdbc:mysql://localhost:3306/appdb";

    protected String user;
    protected String password;
    protected String url;
    protected int poolSize;

    public DatabaseConfig() {
        ApplicationProperties properties = new ApplicationProperties();
        this.user = properties.get("database.user", DEFAULT_USER);
        this.password = properties.get("database.password", DEFAULT_PASSWORD);
        this.url = properties.get("database.url", DEFAULT_URL);
        this.poolSize = parsePoolSize(properties.get(
                "database.pool-size", String.valueOf(DEFAULT_POOL_SIZE)));
    }

    public DatabaseConfig(String user, String password, String url, int poolSize) {
        this.user = user;
        this.password = password;
        this.url = url;
        this.poolSize = poolSize;
    }

    public DatabaseConfig(DatabaseConfig databaseConfig) {
        this.url = databaseConfig.getUrl();
        this.user = databaseConfig.getUser();
        this.poolSize = databaseConfig.getPoolSize();
        this.password = databaseConfig.getPassword();
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getUrl() {
        return url;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    private static int parsePoolSize(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new IllegalArgumentException("database.pool-size must be positive");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("database.pool-size must be an integer", e);
        }
    }
}
