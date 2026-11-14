package com.fixflow.enums;

/**
 * Predefined user roles for FixFlow authentication and authorization.
 * Simplified for Administrator and Technician roles.
 */
public enum UserRole {
    ADMINISTRATOR("Administrator"),
    TECHNICIAN("Technician");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
