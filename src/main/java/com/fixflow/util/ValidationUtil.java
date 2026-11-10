package com.fixflow.util;

import java.util.regex.Pattern;

/**
 * Utility for user input validation across business modules.
 * Implements Section 13 of the FixFlow Project Specification.
 */
public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[0-9+()\\s-]{7,20}$"
    );

    private ValidationUtil() {
        // Utility class
    }

    /**
     * Checks if a string is non-null and contains non-whitespace characters.
     *
     * @param str string to check
     * @return true if non-empty, false otherwise
     */
    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Validates a phone number format (allows digits, spaces, hyphens, plus sign, parentheses).
     *
     * @param phone phone number string
     * @return true if valid phone format, false otherwise
     */
    public static boolean isValidPhone(String phone) {
        if (!isNotEmpty(phone)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * Validates an email address format (optional field).
     * Returns true if email is empty (as it's optional) or if it matches standard email pattern.
     *
     * @param email email string
     * @return true if valid or empty, false if non-empty but invalid format
     */
    public static boolean isValidOptionalEmail(String email) {
        if (!isNotEmpty(email)) {
            return true; // Optional field
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Trims a string and returns null if empty.
     *
     * @param str input string
     * @return trimmed string, or null if null/empty
     */
    public static String sanitize(String str) {
        if (str == null) return null;
        String trimmed = str.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

