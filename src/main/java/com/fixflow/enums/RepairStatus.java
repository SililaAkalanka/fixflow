package com.fixflow.enums;

/**
 * Valid lifecycle statuses for a repair job.
 * Simplified repair lifecycle:
 * RECEIVED -> DIAGNOSING -> IN_PROGRESS -> COMPLETED -> COLLECTED
 *                                       \-> CANCELLED
 */
public enum RepairStatus {
    RECEIVED("Received"),
    DIAGNOSING("Diagnosing"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    COLLECTED("Collected"),
    CANCELLED("Cancelled");

    private final String displayName;

    RepairStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this status is a terminal state (cannot transition further).
     *
     * @return true if the repair is COLLECTED or CANCELLED
     */
    public boolean isTerminal() {
        return this == COLLECTED || this == CANCELLED;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
