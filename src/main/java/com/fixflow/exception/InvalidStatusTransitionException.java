package com.fixflow.exception;

import com.fixflow.enums.RepairStatus;

/**
 * Exception thrown when an invalid repair status transition is attempted.
 * Implements Section 7.3 and Section 12 (OOP Exception Handling) of the FixFlow Project Specification.
 */
public class InvalidStatusTransitionException extends Exception {

    private final RepairStatus fromStatus;
    private final RepairStatus toStatus;

    public InvalidStatusTransitionException(RepairStatus fromStatus, RepairStatus toStatus, String reason) {
        super("Invalid status transition from '" + (fromStatus != null ? fromStatus.getDisplayName() : "None") +
              "' to '" + (toStatus != null ? toStatus.getDisplayName() : "None") + "'. Reason: " + reason);
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    public RepairStatus getFromStatus() {
        return fromStatus;
    }

    public RepairStatus getToStatus() {
        return toStatus;
    }
}

