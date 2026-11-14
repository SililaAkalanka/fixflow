package com.fixflow.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Model representing an electronic device registered for repair.
 * Includes basic device warranty information and validity checking.
 */
public class Device {

    private int deviceId;
    private int customerId;
    private String category;
    private String brand;
    private String model;
    private String serialNumber;
    private String colour;
    private String accessories;
    private Integer warrantyMonths = 0;
    private String warrantyStartDate;
    private String warrantyExpiryDate;
    private String createdAt;

    // Transient fields joined from customer table for display convenience
    private String customerName;
    private String customerPhone;

    public Device() {
    }

    public Device(int customerId, String category, String brand, String model, String serialNumber, String colour, String accessories) {
        this.customerId = customerId;
        this.category = category;
        this.brand = brand;
        this.model = model;
        this.serialNumber = serialNumber;
        this.colour = colour;
        this.accessories = accessories;
    }

    public Device(int deviceId, int customerId, String category, String brand, String model, String serialNumber, String colour, String accessories, String createdAt) {
        this.deviceId = deviceId;
        this.customerId = customerId;
        this.category = category;
        this.brand = brand;
        this.model = model;
        this.serialNumber = serialNumber;
        this.colour = colour;
        this.accessories = accessories;
        this.createdAt = createdAt;
    }

    public Device(int deviceId, int customerId, String category, String brand, String model, String serialNumber,
                  String colour, String accessories, Integer warrantyMonths, String warrantyStartDate,
                  String warrantyExpiryDate, String createdAt) {
        this.deviceId = deviceId;
        this.customerId = customerId;
        this.category = category;
        this.brand = brand;
        this.model = model;
        this.serialNumber = serialNumber;
        this.colour = colour;
        this.accessories = accessories;
        this.warrantyMonths = warrantyMonths;
        this.warrantyStartDate = warrantyStartDate;
        this.warrantyExpiryDate = warrantyExpiryDate;
        this.createdAt = createdAt;
    }

    /**
     * Checks whether the device is currently under active warranty.
     *
     * @return true if currently under warranty, false otherwise
     */
    public boolean isUnderWarranty() {
        if (warrantyExpiryDate == null || warrantyExpiryDate.trim().isEmpty()) {
            return false;
        }
        try {
            return LocalDate.now().compareTo(LocalDate.parse(warrantyExpiryDate.trim())) <= 0;
        } catch (Exception e) {
            return false;
        }
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public String getAccessories() {
        return accessories;
    }

    public void setAccessories(String accessories) {
        this.accessories = accessories;
    }

    public Integer getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(Integer warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    public String getWarrantyStartDate() {
        return warrantyStartDate;
    }

    public void setWarrantyStartDate(String warrantyStartDate) {
        this.warrantyStartDate = warrantyStartDate;
    }

    public String getWarrantyExpiryDate() {
        return warrantyExpiryDate;
    }

    public void setWarrantyExpiryDate(String warrantyExpiryDate) {
        this.warrantyExpiryDate = warrantyExpiryDate;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Device device = (Device) o;
        return deviceId == device.deviceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId);
    }

    @Override
    public String toString() {
        return brand + " " + model + (serialNumber != null ? " (SN: " + serialNumber + ")" : "");
    }
}
