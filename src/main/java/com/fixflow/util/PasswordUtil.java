package com.fixflow.util;

import org.mindrot.jbcrypt.BCrypt;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for hashing and verifying passwords using jBCrypt.
 * In accordance with FixFlow security rules, passwords are never stored in plain text.
 */
public class PasswordUtil {

    private static final Logger LOGGER = Logger.getLogger(PasswordUtil.class.getName());
    private static final int BCRYPT_LOG_ROUNDS = 12;

    private PasswordUtil() {
        // Utility class
    }

    /**
     * Hashes a plain-text password using BCrypt with a secure salt.
     *
     * @param plainTextPassword plain-text password to hash
     * @return salted BCrypt password hash
     * @throws IllegalArgumentException if password is null or empty
     */
    public static String hashPassword(String plainTextPassword) {
        if (plainTextPassword == null || plainTextPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(BCRYPT_LOG_ROUNDS));
    }

    /**
     * Verifies whether a plain-text password matches a stored BCrypt hash.
     *
     * @param plainTextPassword plain-text password entered by the user
     * @param hashedPassword stored BCrypt hash
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String plainTextPassword, String hashedPassword) {
        if (plainTextPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainTextPassword, hashedPassword);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Password verification failed with exception: " + e.getMessage());
            return false;
        }
    }
}

