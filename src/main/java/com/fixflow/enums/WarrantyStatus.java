package com.fixflow.enums;

/**
 * Lifecycle status of a 30-day repair warranty.
 * Implements Section 7.7 and Section 8.14 of the FixFlow Project Specification.
 */
public enum WarrantyStatus {
    ACTIVE("Active"),
    EXPIRED("Expired"),
    CLAIMED("Claimed");

    private final String displayName;

    WarrantyStatus(String displayName) {
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

