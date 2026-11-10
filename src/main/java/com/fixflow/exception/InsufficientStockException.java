package com.fixflow.exception;

/**
 * Exception thrown when attempting to issue more spare parts than are available in inventory.
 * Implements Section 7.5, Section 8.17, and Section 12 (OOP Exception Handling) of the Project Specification.
 */
public class InsufficientStockException extends Exception {

    private final int partId;
    private final String partName;
    private final int availableQuantity;
    private final int requestedQuantity;

    public InsufficientStockException(int partId, String partName, int availableQuantity, int requestedQuantity) {
        super("Insufficient stock for part '" + partName + "' (Part #" + partId + "). Available: " +
              availableQuantity + ", Requested: " + requestedQuantity);
        this.partId = partId;
        this.partName = partName;
        this.availableQuantity = availableQuantity;
        this.requestedQuantity = requestedQuantity;
    }

    public int getPartId() {
        return partId;
    }

    public String getPartName() {
        return partName;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }
}

