package com.fixflow.enums;

/**
 * Payment statuses supported by FixFlow.
 * In accordance with business rules, only full cash payment is supported.
 */
public enum PaymentStatus {
    PENDING("Pending"),
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

