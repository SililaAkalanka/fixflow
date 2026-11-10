package com.fixflow.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilTest {

    @Test
    @DisplayName("Verify isNotEmpty correctly identifies empty or whitespace strings")
    void testIsNotEmpty() {
        assertTrue(ValidationUtil.isNotEmpty("Hello"));
        assertTrue(ValidationUtil.isNotEmpty("  Valid  "));
        assertFalse(ValidationUtil.isNotEmpty(""));
        assertFalse(ValidationUtil.isNotEmpty("   "));
        assertFalse(ValidationUtil.isNotEmpty(null));
    }

    @Test
    @DisplayName("Verify isValidPhone validates typical phone number formats")
    void testIsValidPhone() {
        assertTrue(ValidationUtil.isValidPhone("0771234567"));
        assertTrue(ValidationUtil.isValidPhone("+94 77 123 4567"));
        assertTrue(ValidationUtil.isValidPhone("(011) 234-5678"));
        assertTrue(ValidationUtil.isValidPhone("011-2345678"));

        assertFalse(ValidationUtil.isValidPhone("123")); // Too short
        assertFalse(ValidationUtil.isValidPhone("phone123")); // Letters
        assertFalse(ValidationUtil.isValidPhone(""));
        assertFalse(ValidationUtil.isValidPhone(null));
    }

    @Test
    @DisplayName("Verify isValidOptionalEmail permits empty or valid emails")
    void testIsValidOptionalEmail() {
        assertTrue(ValidationUtil.isValidOptionalEmail(""));
        assertTrue(ValidationUtil.isValidOptionalEmail("   "));
        assertTrue(ValidationUtil.isValidOptionalEmail(null));
        assertTrue(ValidationUtil.isValidOptionalEmail("alice@example.com"));
        assertTrue(ValidationUtil.isValidOptionalEmail("customer.service@fixflow.co.uk"));

        assertFalse(ValidationUtil.isValidOptionalEmail("invalid-email"));
        assertFalse(ValidationUtil.isValidOptionalEmail("missing@domain"));
        assertFalse(ValidationUtil.isValidOptionalEmail("@nodomain.com"));
    }

    @Test
    @DisplayName("Verify sanitize trims strings and returns null for blanks")
    void testSanitize() {
        assertEquals("Clean", ValidationUtil.sanitize("  Clean  "));
        assertNull(ValidationUtil.sanitize("   "));
        assertNull(ValidationUtil.sanitize(null));
    }
}

