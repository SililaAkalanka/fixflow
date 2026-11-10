package com.fixflow.enums;

/**
 * Predefined shared user roles for FixFlow authentication and authorization.
 */
public enum UserRole {
    ADMINISTRATOR("Administrator"),
    MANAGER("Manager"),
    RECEPTIONIST("Receptionist"),
    TECHNICIAN("Technician"),
    INVENTORY_STAFF("Inventory Staff");

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

