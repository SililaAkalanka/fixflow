package com.fixflow.model;

import java.util.Objects;

/**
 * Model representing a spare part issued and used for a specific repair job.
 * Implements Section 7.5 and Section 7.6 of the FixFlow Project Specification.
 */
public class RepairPartUsage {

    private int usageId;
    private int jobId;
    private int partId;
    private int quantity;
    private double sellingPriceAtTime; // Snapshot of selling price at issuance (§7.6)
    private String dateUsed;

    // Transient fields joined from spare_parts table
    private String partName;
    private String partCategory;
    private String compatibleDeviceModel;

    public RepairPartUsage() {
    }

    public RepairPartUsage(int jobId, int partId, int quantity, double sellingPriceAtTime, String dateUsed) {
        this.jobId = jobId;
        this.partId = partId;
        this.quantity = quantity;
        this.sellingPriceAtTime = sellingPriceAtTime;
        this.dateUsed = dateUsed;
    }

    public RepairPartUsage(int usageId, int jobId, int partId, int quantity, double sellingPriceAtTime, String dateUsed) {
        this.usageId = usageId;
        this.jobId = jobId;
        this.partId = partId;
        this.quantity = quantity;
        this.sellingPriceAtTime = sellingPriceAtTime;
        this.dateUsed = dateUsed;
    }

    /**
     * Calculates the total cost for this part usage line item.
     *
     * @return quantity * selling_price_at_time
     */
    public double getTotalCost() {
        return quantity * sellingPriceAtTime;
    }

    public int getUsageId() {
        return usageId;
    }

    public void setUsageId(int usageId) {
        this.usageId = usageId;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public int getPartId() {
        return partId;
    }

    public void setPartId(int partId) {
        this.partId = partId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getSellingPriceAtTime() {
        return sellingPriceAtTime;
    }

    public void setSellingPriceAtTime(double sellingPriceAtTime) {
        this.sellingPriceAtTime = sellingPriceAtTime;
    }

    public String getDateUsed() {
        return dateUsed;
    }

    public void setDateUsed(String dateUsed) {
        this.dateUsed = dateUsed;
    }

    public String getPartName() {
        return partName;
    }

    public void setPartName(String partName) {
        this.partName = partName;
    }

    public String getPartCategory() {
        return partCategory;
    }

    public void setPartCategory(String partCategory) {
        this.partCategory = partCategory;
    }

    public String getCompatibleDeviceModel() {
        return compatibleDeviceModel;
    }

    public void setCompatibleDeviceModel(String compatibleDeviceModel) {
        this.compatibleDeviceModel = compatibleDeviceModel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RepairPartUsage that = (RepairPartUsage) o;
        return usageId == that.usageId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(usageId);
    }

    @Override
    public String toString() {
        return partName + " (Qty: " + quantity + " @ $" + String.format("%.2f", sellingPriceAtTime) +
                " = $" + String.format("%.2f", getTotalCost()) + ")";
    }
}

