package com.training.atm.repository;

import java.io.File;

/**
 * Resolves paths to the application data directory.
 *
 * Fix #11: declared {@code final} — utility classes should not be subclassed.
 */
public final class DataDirectory {
    private static final String DATA_DIR = "data";

    public static String getPath(String filename) {
        return DATA_DIR + File.separator + filename;
    }

    public static void ensureExists() {
        new File(DATA_DIR).mkdirs();
    }

    private DataDirectory() { /* utility class — no instances */ }
}
