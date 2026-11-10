package com.fixflow.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    @DisplayName("Verify password hashing produces a valid BCrypt hash")
    void testHashPassword() {
        String password = "adminPassword123";
        String hash = PasswordUtil.hashPassword(password);

        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"),
                "Hash should be a standard BCrypt hash starting with $2a$, $2b$, or $2y$");
        assertNotEquals(password, hash, "Password must not equal plain text hash");
    }

    @Test
    @DisplayName("Verify password verification matches correct password")
    void testVerifyPasswordSuccess() {
        String password = "mySecretPassword";
        String hash = PasswordUtil.hashPassword(password);

        assertTrue(PasswordUtil.verifyPassword(password, hash));
    }

    @Test
    @DisplayName("Verify password verification fails on wrong password")
    void testVerifyPasswordFailure() {
        String password = "mySecretPassword";
        String hash = PasswordUtil.hashPassword(password);

        assertFalse(PasswordUtil.verifyPassword("wrongPassword", hash));
        assertFalse(PasswordUtil.verifyPassword("", hash));
        assertFalse(PasswordUtil.verifyPassword(null, hash));
        assertFalse(PasswordUtil.verifyPassword(password, null));
    }

    @Test
    @DisplayName("Verify BCrypt uses unique salts across hashes")
    void testUniqueSalts() {
        String password = "samePassword";
        String hash1 = PasswordUtil.hashPassword(password);
        String hash2 = PasswordUtil.hashPassword(password);

        assertNotEquals(hash1, hash2, "BCrypt must generate different salts for each hashing call");
        assertTrue(PasswordUtil.verifyPassword(password, hash1));
        assertTrue(PasswordUtil.verifyPassword(password, hash2));
    }

    @Test
    @DisplayName("Verify hashing null or empty password throws IllegalArgumentException")
    void testEmptyPasswordThrows() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(""));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword("   "));
    }
}

