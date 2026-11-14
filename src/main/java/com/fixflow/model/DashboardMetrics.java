package com.fixflow.model;

import com.fixflow.enums.RepairStatus;

import java.util.EnumMap;
import java.util.Map;

/**
 * Aggregated metric snapshot for the operational dashboard.
 */
public class DashboardMetrics {

    private int totalJobs;
    private int todayJobs;
    private int activeJobs;
    private int completedJobs;
    private int collectedJobs;
    private int cancelledJobs;

    private int totalCustomers;
    private int totalTechnicians;

    private double totalRevenue;
    private double todayRevenue;
    private double pendingRevenue;

    private int lowStockCount;
    private int outOfStockCount;
    private double totalInventoryValuation;

    private int techAssignedActive;
    private int techAssignedCompletedToday;

    private final Map<RepairStatus, Integer> statusCounts = new EnumMap<>(RepairStatus.class);

    public DashboardMetrics() {
        for (RepairStatus s : RepairStatus.values()) {
            statusCounts.put(s, 0);
        }
    }

    public int getTotalJobs() {
        return totalJobs;
    }

    public void setTotalJobs(int totalJobs) {
        this.totalJobs = totalJobs;
    }

    public int getTodayJobs() {
        return todayJobs;
    }

    public void setTodayJobs(int todayJobs) {
        this.todayJobs = todayJobs;
    }

    public int getActiveJobs() {
        return activeJobs;
    }

    public void setActiveJobs(int activeJobs) {
        this.activeJobs = activeJobs;
    }

    public int getCompletedJobs() {
        return completedJobs;
    }

    public void setCompletedJobs(int completedJobs) {
        this.completedJobs = completedJobs;
    }

    public int getCollectedJobs() {
        return collectedJobs;
    }

    public void setCollectedJobs(int collectedJobs) {
        this.collectedJobs = collectedJobs;
    }

    public int getCancelledJobs() {
        return cancelledJobs;
    }

    public void setCancelledJobs(int cancelledJobs) {
        this.cancelledJobs = cancelledJobs;
    }

    public int getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(int totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public int getTotalTechnicians() {
        return totalTechnicians;
    }

    public void setTotalTechnicians(int totalTechnicians) {
        this.totalTechnicians = totalTechnicians;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public double getTodayRevenue() {
        return todayRevenue;
    }

    public void setTodayRevenue(double todayRevenue) {
        this.todayRevenue = todayRevenue;
    }

    public double getPendingRevenue() {
        return pendingRevenue;
    }

    public void setPendingRevenue(double pendingRevenue) {
        this.pendingRevenue = pendingRevenue;
    }

    public int getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(int lowStockCount) {
        this.lowStockCount = lowStockCount;
    }

    public int getOutOfStockCount() {
        return outOfStockCount;
    }

    public void setOutOfStockCount(int outOfStockCount) {
        this.outOfStockCount = outOfStockCount;
    }

    public double getTotalInventoryValuation() {
        return totalInventoryValuation;
    }

    public void setTotalInventoryValuation(double totalInventoryValuation) {
        this.totalInventoryValuation = totalInventoryValuation;
    }

    public int getTechAssignedActive() {
        return techAssignedActive;
    }

    public void setTechAssignedActive(int techAssignedActive) {
        this.techAssignedActive = techAssignedActive;
    }

    public int getTechAssignedCompletedToday() {
        return techAssignedCompletedToday;
    }

    public void setTechAssignedCompletedToday(int techAssignedCompletedToday) {
        this.techAssignedCompletedToday = techAssignedCompletedToday;
    }

    public int getStatusCount(RepairStatus status) {
        return statusCounts.getOrDefault(status, 0);
    }

    public void setStatusCount(RepairStatus status, int count) {
        if (status != null) {
            statusCounts.put(status, count);
        }
    }

    public Map<RepairStatus, Integer> getStatusCounts() {
        return statusCounts;
    }
}
