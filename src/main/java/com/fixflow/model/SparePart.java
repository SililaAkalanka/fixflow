package com.fixflow.model;

import java.util.Objects;

/**
 * Model representing a spare part in inventory.
 * Implements Section 7.5 of the FixFlow Project Specification.
 */
public class SparePart {

    private int partId;
    private String partName;
    private String category;
    private String compatibleDeviceModel;
    private double purchaseCost;
    private double sellingPrice;
    private int availableQuantity;
    private int minStockLevel;
    private String createdAt;
    private String updatedAt;

    public SparePart() {
        this.availableQuantity = 0;
        this.minStockLevel = 5;
    }

    public SparePart(String partName, String category, String compatibleDeviceModel,
                     double purchaseCost, double sellingPrice, int availableQuantity, int minStockLevel) {
        this.partName = partName;
        this.category = category;
        this.compatibleDeviceModel = compatibleDeviceModel;
        this.purchaseCost = purchaseCost;
        this.sellingPrice = sellingPrice;
        this.availableQuantity = availableQuantity;
        this.minStockLevel = minStockLevel;
    }

    public SparePart(int partId, String partName, String category, String compatibleDeviceModel,
                     double purchaseCost, double sellingPrice, int availableQuantity, int minStockLevel,
                     String createdAt, String updatedAt) {
        this.partId = partId;
        this.partName = partName;
        this.category = category;
        this.compatibleDeviceModel = compatibleDeviceModel;
        this.purchaseCost = purchaseCost;
        this.sellingPrice = sellingPrice;
        this.availableQuantity = availableQuantity;
        this.minStockLevel = minStockLevel;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Checks if stock level is at or below the configured minimum threshold.
     * Implements Section 7.5 low-stock alert rule.
     *
     * @return true if available quantity <= minimum stock level
     */
    public boolean isLowStock() {
        return availableQuantity <= minStockLevel;
    }

    /**
     * Checks if the part is completely out of stock.
     *
     * @return true if available quantity is 0
     */
    public boolean isOutOfStock() {
        return availableQuantity == 0;
    }

    public int getPartId() {
        return partId;
    }

    public void setPartId(int partId) {
        this.partId = partId;
    }

    public String getPartName() {
        return partName;
    }

    public void setPartName(String partName) {
        this.partName = partName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCompatibleDeviceModel() {
        return compatibleDeviceModel;
    }

    public void setCompatibleDeviceModel(String compatibleDeviceModel) {
        this.compatibleDeviceModel = compatibleDeviceModel;
    }

    public double getPurchaseCost() {
        return purchaseCost;
    }

    public void setPurchaseCost(double purchaseCost) {
        this.purchaseCost = purchaseCost;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public int getMinStockLevel() {
        return minStockLevel;
    }

    public void setMinStockLevel(int minStockLevel) {
        this.minStockLevel = minStockLevel;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SparePart sparePart = (SparePart) o;
        return partId == sparePart.partId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(partId);
    }

    @Override
    public String toString() {
        return partName + " [" + compatibleDeviceModel + "] ($" + String.format("%.2f", sellingPrice) +
                ", Qty: " + availableQuantity + ")";
    }
}

