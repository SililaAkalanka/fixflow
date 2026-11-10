package com.fixflow.model;

import com.fixflow.enums.RepairStatus;
import com.fixflow.exception.InvalidStatusTransitionException;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Model representing a repair job and its complete lifecycle.
 * Implements Section 7.3, Section 8, and Section 12 (OOP Encapsulation & Business Rules).
 */
public class RepairJob {

    private int jobId;
    private int customerId;
    private int deviceId;
    private Integer technicianId;
    private String dateReceived;
    private String initialProblem;
    private RepairStatus currentStatus;
    private String diagnosisFindings;
    private Double estimatedCost;
    private double labourCharges;
    private String estimatedCompletionDate;
    private String actualCompletionDate;
    private String approvalStatus; // "PENDING", "APPROVED", "REJECTED"
    private String approvalDate;
    private String cancellationReason;
    private Integer originalJobId; // For warranty claims
    private String createdAt;
    private String updatedAt;

    // Transient fields joined from other tables for UI presentation
    private String customerName;
    private String customerPhone;
    private String deviceSummary;
    private String technicianName;

    public RepairJob() {
        this.currentStatus = RepairStatus.RECEIVED;
        this.approvalStatus = "PENDING";
        this.labourCharges = 0.0;
    }

    public RepairJob(int customerId, int deviceId, String initialProblem) {
        this.customerId = customerId;
        this.deviceId = deviceId;
        this.initialProblem = initialProblem;
        this.currentStatus = RepairStatus.RECEIVED;
        this.approvalStatus = "PENDING";
        this.labourCharges = 0.0;
    }

    /**
     * Determines all valid next statuses from the current status.
     * Enforces the status transition matrix from Section 7.3 and Section 8.8.
     *
     * @return set of valid destination {@link RepairStatus}es
     */
    public Set<RepairStatus> getValidNextStatuses() {
        if (currentStatus == null || currentStatus.isTerminal()) {
            return Collections.emptySet();
        }

        return switch (currentStatus) {
            case RECEIVED -> EnumSet.of(RepairStatus.DIAGNOSING, RepairStatus.CANCELLED);
            case DIAGNOSING -> EnumSet.of(RepairStatus.WAITING_FOR_APPROVAL, RepairStatus.CANCELLED);
            case WAITING_FOR_APPROVAL -> EnumSet.of(RepairStatus.REPAIRING, RepairStatus.CANCELLED);
            case REPAIRING -> EnumSet.of(RepairStatus.WAITING_FOR_PARTS, RepairStatus.READY_FOR_COLLECTION, RepairStatus.CANCELLED);
            case WAITING_FOR_PARTS -> EnumSet.of(RepairStatus.REPAIRING, RepairStatus.CANCELLED);
            case READY_FOR_COLLECTION -> EnumSet.of(RepairStatus.COLLECTED, RepairStatus.CANCELLED);
            case COLLECTED, CANCELLED -> Collections.emptySet();
        };
    }

    /**
     * Checks if a transition from current status to target status is valid.
     *
     * @param targetStatus proposed new status
     * @return true if valid according to business rules, false otherwise
     */
    public boolean canTransitionTo(RepairStatus targetStatus) {
        if (targetStatus == null) return false;
        return getValidNextStatuses().contains(targetStatus);
    }

    /**
     * Validates that the transition to targetStatus satisfies all business rules.
     *
     * @param targetStatus proposed new status
     * @throws InvalidStatusTransitionException if transition violates lifecycle or business rules
     */
    public void validateTransition(RepairStatus targetStatus) throws InvalidStatusTransitionException {
        if (targetStatus == null) {
            throw new InvalidStatusTransitionException(currentStatus, null, "Target status cannot be null.");
        }

        if (currentStatus != null && currentStatus.isTerminal()) {
            throw new InvalidStatusTransitionException(currentStatus, targetStatus,
                    "Terminal state '" + currentStatus.getDisplayName() + "' cannot transition to any other status.");
        }

        if (!canTransitionTo(targetStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, targetStatus,
                    "Transition not permitted by lifecycle rules. Permitted next statuses: " + getValidNextStatuses());
        }

        // Business Rule §7.3 & §8.6: Customer approval is required before repair work begins
        if (targetStatus == RepairStatus.REPAIRING && currentStatus == RepairStatus.WAITING_FOR_APPROVAL) {
            if (!"APPROVED".equalsIgnoreCase(approvalStatus)) {
                throw new InvalidStatusTransitionException(currentStatus, targetStatus,
                        "A repair job cannot enter the REPAIRING stage without recorded customer approval.");
            }
        }
    }

    // Getters and Setters

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Integer technicianId) {
        this.technicianId = technicianId;
    }

    public String getDateReceived() {
        return dateReceived;
    }

    public void setDateReceived(String dateReceived) {
        this.dateReceived = dateReceived;
    }

    public String getInitialProblem() {
        return initialProblem;
    }

    public void setInitialProblem(String initialProblem) {
        this.initialProblem = initialProblem;
    }

    public RepairStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(RepairStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getDiagnosisFindings() {
        return diagnosisFindings;
    }

    public void setDiagnosisFindings(String diagnosisFindings) {
        this.diagnosisFindings = diagnosisFindings;
    }

    public Double getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(Double estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public double getLabourCharges() {
        return labourCharges;
    }

    public void setLabourCharges(double labourCharges) {
        this.labourCharges = labourCharges;
    }

    public String getEstimatedCompletionDate() {
        return estimatedCompletionDate;
    }

    public void setEstimatedCompletionDate(String estimatedCompletionDate) {
        this.estimatedCompletionDate = estimatedCompletionDate;
    }

    public String getActualCompletionDate() {
        return actualCompletionDate;
    }

    public void setActualCompletionDate(String actualCompletionDate) {
        this.actualCompletionDate = actualCompletionDate;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(String approvalDate) {
        this.approvalDate = approvalDate;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Integer getOriginalJobId() {
        return originalJobId;
    }

    public void setOriginalJobId(Integer originalJobId) {
        this.originalJobId = originalJobId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RepairJob repairJob = (RepairJob) o;
        return jobId == repairJob.jobId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId);
    }

    @Override
    public String toString() {
        return "Job #" + jobId + " [" + currentStatus.getDisplayName() + "] - " +
                (deviceSummary != null ? deviceSummary : "Device #" + deviceId);
    }
}

