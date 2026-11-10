package com.fixflow.enums;

/**
 * Valid lifecycle statuses for a repair job.
 */
public enum RepairStatus {
    RECEIVED("Received"),
    DIAGNOSING("Diagnosing"),
    WAITING_FOR_APPROVAL("Waiting for Approval"),
    WAITING_FOR_PARTS("Waiting for Parts"),
    REPAIRING("Repairing"),
    READY_FOR_COLLECTION("Ready for Collection"),
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

