package com.training.atm.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

public class SecurityUtil {
    private static final Pattern SHA256_HEX = Pattern.compile("[0-9a-fA-F]{64}");

    public static String hashPin(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static boolean matchesPin(String rawPin, String storedHash) {
        return rawPin != null
                && storedHash != null
                && hashPin(rawPin).equalsIgnoreCase(storedHash);
    }

    public static String normalizeStoredPin(String pin) {
        if (pin == null || SHA256_HEX.matcher(pin).matches()) {
            return pin;
        }
        return hashPin(pin);
    }
}
