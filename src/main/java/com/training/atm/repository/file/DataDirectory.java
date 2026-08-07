package com.training.atm.repository.file;

import com.training.atm.config.ApplicationProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves the writable data directory.
 *
 * Fix #11: declared {@code final} — utility classes should not be subclassed.
 */
public final class DataDirectory {
    private static final ApplicationProperties PROPERTIES = new ApplicationProperties();
    private static final String PROFILE = PROPERTIES.get("profile", "prod").trim().toLowerCase();
    private static final String DATA_DIR = PROPERTIES.get(
            "file.data-directory", defaultDirectoryFor(PROFILE));
    private static final String RESOURCE_DIR = PROPERTIES.get("file.data-resource", "");
    private static final String[] DATA_FILES = {
            "accounts.txt", "admin_log.txt", "atm.txt", "cards.txt",
            "customers.txt", "denominations.txt", "scheduled_transfers.txt",
            "transactions.txt"
    };
    public static String getPath(String filename) {
        return Path.of(DATA_DIR, filename).toString();
    }

    public static void ensureExists() {
        Path directory = Path.of(DATA_DIR);
        try {
            Files.createDirectories(directory);
            if (!RESOURCE_DIR.isBlank()) {
                for (String filename : DATA_FILES) {
                    Path target = directory.resolve(filename);
                    if (Files.notExists(target)) {
                        copyResource(filename, target);
                    }
                }
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Unable to initialize file data directory: " + directory, e);
        }
    }

    private static void copyResource(String filename, Path target) throws IOException {
        try (InputStream source = DataDirectory.class.getClassLoader()
                .getResourceAsStream(RESOURCE_DIR + "/" + filename)) {
            if (source == null) {
                throw new IOException("Missing test data resource: " + RESOURCE_DIR + "/" + filename);
            }
            Files.copy(source, target);
        }
    }

    private static String defaultDirectoryFor(String profile) {
        return switch (profile) {
            case "prod" -> "./data";
            case "test" -> "./target/test-data";
            default -> throw new IllegalArgumentException(
                    "Unsupported profile '" + profile + "'. Expected 'prod' or 'test'.");
        };
    }

    private DataDirectory() { /* utility class — no instances */ }
}
