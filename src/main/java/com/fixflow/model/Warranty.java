package com.fixflow.model;

import com.fixflow.enums.WarrantyStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Domain model representing a 30-day repair warranty record.
 * Implements Section 7.7 and Section 8.14 of the FixFlow Project Specification.
 */
public class Warranty {

    private int warrantyId;
    private int jobId;
    private String startDate;
    private String expiryDate;
    private WarrantyStatus status = WarrantyStatus.ACTIVE;
    private String createdAt;

    // Joined auxiliary fields for presentation and warranty claims
    private String customerName;
    private String customerPhone;
    private String deviceSummary;
    private String serialNumber;
    private String repairProblem;

    public Warranty() {
    }

    public Warranty(int jobId, String startDate, String expiryDate) {
        this.jobId = jobId;
        this.startDate = startDate;
        this.expiryDate = expiryDate;
        this.status = WarrantyStatus.ACTIVE;
    }

    public Warranty(int warrantyId, int jobId, String startDate, String expiryDate, WarrantyStatus status, String createdAt) {
        this.warrantyId = warrantyId;
        this.jobId = jobId;
        this.startDate = startDate;
        this.expiryDate = expiryDate;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * Checks if the warranty is currently valid and eligible for a warranty claim.
     * Rule: Status must be ACTIVE and current date must not be past expiry date.
     *
     * @return true if warranty is active and within 30 days
     */
    public boolean isCurrentlyValid() {
        if (status != WarrantyStatus.ACTIVE || expiryDate == null) {
            return false;
        }
        try {
            LocalDate exp = LocalDate.parse(expiryDate);
            return !LocalDate.now().isAfter(exp);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calculates the remaining calendar days before expiration.
     *
     * @return remaining days (negative if expired)
     */
    public long getDaysRemaining() {
        if (expiryDate == null) return 0;
        try {
            LocalDate exp = LocalDate.parse(expiryDate);
            return ChronoUnit.DAYS.between(LocalDate.now(), exp);
        } catch (Exception e) {
            return 0;
        }
    }

    // Getters and Setters

    public int getWarrantyId() {
        return warrantyId;
    }

    public void setWarrantyId(int warrantyId) {
        this.warrantyId = warrantyId;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public WarrantyStatus getStatus() {
        return status;
    }

    public void setStatus(WarrantyStatus status) {
        this.status = status;
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

    public String getDeviceSummary() {
        return deviceSummary;
    }

    public void setDeviceSummary(String deviceSummary) {
        this.deviceSummary = deviceSummary;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getRepairProblem() {
        return repairProblem;
    }

    public void setRepairProblem(String repairProblem) {
        this.repairProblem = repairProblem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Warranty warranty = (Warranty) o;
        return warrantyId == warranty.warrantyId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(warrantyId);
    }

    @Override
    public String toString() {
        return "Warranty #" + warrantyId + " [Job #" + jobId + "] - Valid until: " + expiryDate + " (" + status + ")";
    }
}

