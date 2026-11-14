package com.fixflow.enums;

/**
 * Payment statuses supported by FixFlow.
 * Supports partial payments and tracking outstanding balances.
 */
public enum PaymentStatus {
    UNPAID("Unpaid"),
    PARTIALLY_PAID("Partially Paid"),
    PAID("Paid");

    private final String displayName;

    PaymentStatus(String displayName) {
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
