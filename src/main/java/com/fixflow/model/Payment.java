package com.fixflow.model;

import java.util.Objects;

/**
 * Domain model representing a repair payment and invoice record.
 * Implements Section 7.4 and Section 8.12 of the FixFlow Project Specification.
 */
public class Payment {

    private int paymentId;
    private int jobId;
    private String paymentDate;
    private double totalRepairCost;
    private double amountPaid;
    private String paymentMethod = "CASH";
    private String paymentStatus = "PAID";
    private String createdAt;

    // Joined auxiliary fields for UI presentation and invoice generation
    private String customerName;
    private String customerPhone;
    private String deviceSummary;
    private String technicianName;
    private double labourCharges;
    private double partsCost;

    public Payment() {
    }

    public Payment(int jobId, String paymentDate, double totalRepairCost, double amountPaid) {
        this.jobId = jobId;
        this.paymentDate = paymentDate;
        this.totalRepairCost = totalRepairCost;
        this.amountPaid = amountPaid;
        this.paymentMethod = "CASH";
        this.paymentStatus = "PAID";
    }

    public Payment(int paymentId, int jobId, String paymentDate, double totalRepairCost, double amountPaid,
                   String paymentMethod, String paymentStatus, String createdAt) {
        this.paymentId = paymentId;
        this.jobId = jobId;
        this.paymentDate = paymentDate;
        this.totalRepairCost = totalRepairCost;
        this.amountPaid = amountPaid;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
    }

    // Getters and Setters

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }

    public double getTotalRepairCost() {
        return totalRepairCost;
    }

    public void setTotalRepairCost(double totalRepairCost) {
        this.totalRepairCost = totalRepairCost;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
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

    public String getTechnicianName() {
        return technicianName;
    }

    public void setTechnicianName(String technicianName) {
        this.technicianName = technicianName;
    }

    public double getLabourCharges() {
        return labourCharges;
    }

    public void setLabourCharges(double labourCharges) {
        this.labourCharges = labourCharges;
    }

    public double getPartsCost() {
        return partsCost;
    }

    public void setPartsCost(double partsCost) {
        this.partsCost = partsCost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return paymentId == payment.paymentId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId);
    }

    @Override
    public String toString() {
        return "Payment #" + paymentId + " [Job #" + jobId + "] - Amount: LKR " +
                String.format("%.2f", amountPaid) + " (" + paymentStatus + ")";
    }
}

