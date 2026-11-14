package com.fixflow.service;

import com.fixflow.database.RepairDB;
import com.fixflow.database.TechnicianDB;
import com.fixflow.enums.RepairStatus;
import com.fixflow.exception.InvalidStatusTransitionException;
import com.fixflow.model.RepairJob;
import com.fixflow.model.Technician;

import java.sql.SQLException;
import java.util.List;

/**
 * Service class for repair job business logic, lifecycle transitions, and collection validation.
 */
public class RepairService {

    private RepairService() {
        // Business service
    }

    /**
     * Creates a new repair job.
     */
    public static int createRepairJob(RepairJob job) throws SQLException {
        return RepairDB.createRepairJob(job);
    }

    /**
     * Calculates the total cost for a repair job.
     */
    public static double calculateTotalCost(int jobId) throws SQLException {
        return PaymentService.calculateTotalCost(jobId);
    }

    /**
     * Advances or updates the repair job status with strict lifecycle and collection checks.
     * Lifecycle: RECEIVED -> DIAGNOSING -> IN_PROGRESS -> COMPLETED -> COLLECTED
     *                                                  \-> CANCELLED
     *
     * @param jobId repair job ID
     * @param targetStatus target status
     * @param notes notes or cancellation reason
     * @throws SQLException if a database error occurs
     * @throws InvalidStatusTransitionException if transition is invalid
     * @throws IllegalStateException if collecting an unpaid device
     * @throws IllegalArgumentException if cancellation reason is missing
     */
    public static void updateStatus(int jobId, RepairStatus targetStatus, String notes)
            throws SQLException, InvalidStatusTransitionException {

        RepairJob job = RepairDB.getRepairJobById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Repair Job #" + jobId + " not found.");
        }

        // Validate lifecycle transition rules in model
        job.validateTransition(targetStatus);

        // Guard: A device cannot be marked COLLECTED if there is an outstanding balance
        if (targetStatus == RepairStatus.COLLECTED) {
            if (!PaymentService.isJobFullyPaid(jobId)) {
                double balance = PaymentService.getRemainingBalance(jobId);
                throw new IllegalStateException(String.format(
                        "Cannot collect device: Repair Job #%d has an outstanding balance of LKR %.2f.",
                        jobId, balance));
            }
        }

        // Guard: Cancellation requires a reason
        if (targetStatus == RepairStatus.CANCELLED) {
            if (notes == null || notes.trim().isEmpty()) {
                throw new IllegalArgumentException("A cancellation reason must be provided when cancelling a repair job.");
            }
        }

        // Apply update in DB
        RepairDB.updateStatus(jobId, targetStatus, notes);
    }

    /**
     * Assigns a technician to a repair job after validating technician existence and active status.
     *
     * @param jobId repair job ID
     * @param technicianId technician ID
     * @return true if updated
     * @throws SQLException if a database error occurs
     * @throws IllegalArgumentException if technician does not exist or is inactive
     */
    public static boolean assignTechnician(int jobId, int technicianId) throws SQLException {
        Technician tech = TechnicianDB.getTechnicianById(technicianId);
        if (tech == null) {
            throw new IllegalArgumentException("Technician #" + technicianId + " not found.");
        }
        if (!tech.isActive()) {
            throw new IllegalArgumentException("Cannot assign technician '" + tech.getName() + "': Technician is marked as Inactive.");
        }
        return RepairDB.assignTechnician(jobId, technicianId);
    }

    /**
     * Records technician diagnosis and estimates.
     */
    public static void recordDiagnosis(int jobId, String findings, Double estimatedCost, double labourCharges,
                                       String estCompletionDate) throws SQLException {
        RepairDB.recordDiagnosis(jobId, findings, estimatedCost, labourCharges, estCompletionDate);
    }

    /**
     * Retrieves a repair job by ID.
     */
    public static RepairJob getRepairJobById(int jobId) throws SQLException {
        return RepairDB.getRepairJobById(jobId);
    }

    /**
     * Retrieves all repair jobs.
     */
    public static List<RepairJob> getAllRepairJobs() throws SQLException {
        return RepairDB.getAllRepairJobs();
    }

    /**
     * Retrieves repair jobs for a technician.
     */
    public static List<RepairJob> getRepairJobsForTechnician(int technicianId) throws SQLException {
        return RepairDB.getRepairJobsForTechnician(technicianId);
    }

    /**
     * Searches repair jobs with filters.
     */
    public static List<RepairJob> searchRepairJobs(String query, RepairStatus statusFilter, Integer technicianIdFilter)
            throws SQLException {
        return RepairDB.searchRepairJobs(query, statusFilter, technicianIdFilter);
    }

    /**
     * Deletes a repair job if safe.
     */
    public static boolean deleteRepairJob(int jobId) throws SQLException {
        return RepairDB.deleteRepairJob(jobId);
    }
}
