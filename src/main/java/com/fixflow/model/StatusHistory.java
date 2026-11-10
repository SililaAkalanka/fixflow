package com.fixflow.model;

import com.fixflow.enums.RepairStatus;

/**
 * Model representing an audit record of a repair status transition.
 * Implements Section 7.10 of the FixFlow Project Specification.
 */
public class StatusHistory {

    private int historyId;
    private int jobId;
    private RepairStatus fromStatus;
    private RepairStatus toStatus;
    private String changedByRole;
    private String changeDate;
    private String notes;

    public StatusHistory() {
    }

    public StatusHistory(int jobId, RepairStatus fromStatus, RepairStatus toStatus, String changedByRole, String changeDate, String notes) {
        this.jobId = jobId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedByRole = changedByRole;
        this.changeDate = changeDate;
        this.notes = notes;
    }

    public StatusHistory(int historyId, int jobId, RepairStatus fromStatus, RepairStatus toStatus, String changedByRole, String changeDate, String notes) {
        this.historyId = historyId;
        this.jobId = jobId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedByRole = changedByRole;
        this.changeDate = changeDate;
        this.notes = notes;
    }

    public int getHistoryId() {
        return historyId;
    }

    public void setHistoryId(int historyId) {
        this.historyId = historyId;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public RepairStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(RepairStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public RepairStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(RepairStatus toStatus) {
        this.toStatus = toStatus;
    }

    public String getChangedByRole() {
        return changedByRole;
    }

    public void setChangedByRole(String changedByRole) {
        this.changedByRole = changedByRole;
    }

    public String getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(String changeDate) {
        this.changeDate = changeDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "[" + changeDate + "] " + (fromStatus != null ? fromStatus.getDisplayName() : "Initial Intake") +
                " -> " + toStatus.getDisplayName() + " (By: " + (changedByRole != null ? changedByRole : "System") +
                (notes != null && !notes.isEmpty() ? " - " + notes : "") + ")";
    }
}

