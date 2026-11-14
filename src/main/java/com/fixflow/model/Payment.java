package com.fixflow.model;

import java.util.Objects;

/**
 * Domain model representing a repair payment transaction.
 * Supports multiple partial cash payments per repair job.
 */
public class Payment {

    private int paymentId;
    private int jobId;
    private String paymentDate;
    private double amountPaid;
    private String paymentMethod = "CASH";
    private String notes;
    private String createdAt;

    // Joined auxiliary fields for UI presentation and invoice generation
    private String customerName;
    private String customerPhone;
    private String deviceSummary;
    private String technicianName;
    private double labourCharges;
    private double partsCost;
    private double totalRepairCost;
    private String paymentStatus;

    public Payment() {
        this.paymentMethod = "CASH";
    }

    public Payment(int jobId, String paymentDate, double amountPaid, String notes) {
        this.jobId = jobId;
        this.paymentDate = paymentDate;
        this.amountPaid = amountPaid;
        this.paymentMethod = "CASH";
        this.notes = notes;
    }

    public Payment(int paymentId, int jobId, String paymentDate, double amountPaid,
                   String paymentMethod, String notes, String createdAt) {
        this.paymentId = paymentId;
        this.jobId = jobId;
        this.paymentDate = paymentDate;
        this.amountPaid = amountPaid;
        this.paymentMethod = paymentMethod != null ? paymentMethod : "CASH";
        this.notes = notes;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public double getTotalRepairCost() {
        return totalRepairCost;
    }

    public void setTotalRepairCost(double totalRepairCost) {
        this.totalRepairCost = totalRepairCost;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
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
                String.format("%.2f", amountPaid);
    }
}
