package com.training.atm.testutil;

import com.training.atm.config.ApplicationProperties;
import com.training.atm.repository.RepositoryContext;
import com.training.atm.repository.RepositoryFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Manages test data isolation and cleanup for file repository tests.
 * Resets test data directory before each test to ensure isolated state.
 */
public class TestDataManager {
    private static final ApplicationProperties PROPERTIES = new ApplicationProperties();
    private static final String TEST_DATA_DIR = PROPERTIES.get("file.data-directory", "./target/test-data");
    private static final String RESOURCE_DIR = PROPERTIES.get("file.data-resource", "data");
    private static final String[] DATA_FILES = {
            "accounts.txt", "admin_log.txt", "atm.txt", "cards.txt",
            "customers.txt", "denominations.txt", "scheduled_transfers.txt",
            "transactions.txt"
    };

    /**
     * Resets test data directory by deleting and recreating from test resources.
     * Should be called before each test or test class to ensure isolation.
     */
    public static void resetTestData() {
        try {
            Path testDataPath = Path.of(TEST_DATA_DIR);
            
            // Delete existing test data
            if (Files.exists(testDataPath)) {
                Files.walk(testDataPath)
                        .sorted((a, b) -> -a.compareTo(b)) // reverse order for deletion
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                throw new IllegalStateException("Failed to delete: " + path, e);
                            }
                        });
            }
            
            // Recreate directory
            Files.createDirectories(testDataPath);
            
            // Copy fresh test data from resources
            for (String filename : DATA_FILES) {
                Path targetPath = testDataPath.resolve(filename);
                copyResourceToFile(RESOURCE_DIR + "/" + filename, targetPath);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to reset test data", e);
        }
    }

    /**
     * Creates repositories using real file implementation with test profile.
     * Call resetTestData() before this to ensure clean state.
     */
    public static RepositoryContext createRepositories() {
        return RepositoryFactory.create();
    }

    private static void copyResourceToFile(String resourcePath, Path target) throws IOException {
        try (InputStream inputStream = TestDataManager.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Missing test resource: " + resourcePath);
            }
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
