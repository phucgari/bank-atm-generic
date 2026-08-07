package com.training.atm.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ApplicationProperties {
    private final Properties values = new Properties();

    public ApplicationProperties() {
        load("application.properties", true);
    }

    public String get(String key, String defaultValue) {
        return System.getProperty(key, values.getProperty(key, defaultValue));
    }

    private void load(String resourceName, boolean optional) {
        try (InputStream input = ApplicationProperties.class.getClassLoader()
                .getResourceAsStream(resourceName)) {
            if (input == null) {
                if (optional) return;
                throw new IllegalStateException("Missing " + resourceName);
            }
            values.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load " + resourceName, e);
        }
    }
}
