package com.fixflow.model;

import com.fixflow.enums.RepairStatus;
import com.fixflow.exception.InvalidStatusTransitionException;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Model representing a repair job and its lifecycle.
 * Simplified repair lifecycle:
 * RECEIVED -> DIAGNOSING -> IN_PROGRESS -> COMPLETED -> COLLECTED
 *                                       \-> CANCELLED
 */
public class RepairJob {

    private int jobId;
    private int customerId;
    private int deviceId;
    private Integer technicianId;
    private String dateReceived;
    private String initialProblem;
    private RepairStatus currentStatus = RepairStatus.RECEIVED;
    private String diagnosisFindings;
    private Double estimatedCost;
    private double labourCharges = 0.0;
    private String estimatedCompletionDate;
    private String actualCompletionDate;
    private String notes;
    private String createdAt;
    private String updatedAt;

    // Transient fields joined from other tables for UI presentation
    private String customerName;
    private String customerPhone;
    private String deviceSummary;
    private String technicianName;

    public RepairJob() {
        this.currentStatus = RepairStatus.RECEIVED;
    }

    public RepairJob(int customerId, int deviceId, String initialProblem) {
        this.customerId = customerId;
        this.deviceId = deviceId;
        this.initialProblem = initialProblem;
        this.currentStatus = RepairStatus.RECEIVED;
    }

    /**
     * Determines all valid next statuses from the current status.
     *
     * @return set of valid destination statuses
     */
    public Set<RepairStatus> getValidNextStatuses() {
        if (currentStatus == null || currentStatus.isTerminal()) {
            return Collections.emptySet();
        }

        return switch (currentStatus) {
            case RECEIVED -> EnumSet.of(RepairStatus.DIAGNOSING, RepairStatus.CANCELLED);
            case DIAGNOSING -> EnumSet.of(RepairStatus.IN_PROGRESS, RepairStatus.CANCELLED);
            case IN_PROGRESS -> EnumSet.of(RepairStatus.COMPLETED, RepairStatus.CANCELLED);
            case COMPLETED -> EnumSet.of(RepairStatus.COLLECTED, RepairStatus.CANCELLED);
            case COLLECTED, CANCELLED -> Collections.emptySet();
        };
    }

    /**
     * Checks if a transition from current status to target status is valid.
     *
     * @param targetStatus proposed new status
     * @return true if valid, false otherwise
     */
    public boolean canTransitionTo(RepairStatus targetStatus) {
        if (targetStatus == null) return false;
        return getValidNextStatuses().contains(targetStatus);
    }

    /**
     * Validates that the transition to targetStatus satisfies lifecycle rules.
     *
     * @param targetStatus proposed new status
     * @throws InvalidStatusTransitionException if transition is invalid
     */
    public void validateTransition(RepairStatus targetStatus) throws InvalidStatusTransitionException {
        if (targetStatus == null) {
            throw new InvalidStatusTransitionException(currentStatus, null, "Target status cannot be null.");
        }

        if (currentStatus != null && currentStatus.isTerminal()) {
            throw new InvalidStatusTransitionException(currentStatus, targetStatus,
                    "Terminal state '" + currentStatus.getDisplayName() + "' cannot transition further.");
        }

        if (!canTransitionTo(targetStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, targetStatus,
                    "Transition not permitted. Permitted next statuses: " + getValidNextStatuses());
        }
    }

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
        return "RepairJob #" + jobId + " [" + (currentStatus != null ? currentStatus.getDisplayName() : "Unknown") + "]";
    }
}
