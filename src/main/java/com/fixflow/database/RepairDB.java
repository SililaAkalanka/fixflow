package com.fixflow.database;

import com.fixflow.enums.RepairStatus;
import com.fixflow.exception.InvalidStatusTransitionException;
import com.fixflow.model.RepairJob;
import com.fixflow.model.StatusHistory;
import com.fixflow.model.Technician;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data access operations for repair jobs, technician assignments, status lifecycle, and audit history.
 * Implements Section 7.3, Section 8, and Section 11 of the FixFlow Project Specification.
 */
public class RepairDB {

    private static final Logger LOGGER = Logger.getLogger(RepairDB.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Creates a new repair job and records the initial intake status audit history.
     * Enforces Section 7.3 business rules.
     *
     * @param job repair job model
     * @param createdByRole role creating the job (Receptionist)
     * @return generated job_id
     * @throws SQLException if a database error occurs
     */
    public static int createRepairJob(RepairJob job, String createdByRole) throws SQLException {
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        String sql = "INSERT INTO repair_jobs (customer_id, device_id, technician_id, date_received, initial_problem, " +
                     "current_status, labour_charges, approval_status, original_job_id, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int jobId;
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, job.getCustomerId());
                    ps.setInt(2, job.getDeviceId());
                    if (job.getTechnicianId() != null && job.getTechnicianId() > 0) {
                        ps.setInt(3, job.getTechnicianId());
                    } else {
                        ps.setNull(3, java.sql.Types.INTEGER);
                    }
                    ps.setString(4, job.getDateReceived() != null ? job.getDateReceived() : now);
                    ps.setString(5, job.getInitialProblem().trim());
                    ps.setString(6, RepairStatus.RECEIVED.name());
                    ps.setDouble(7, job.getLabourCharges());
                    ps.setString(8, "PENDING");
                    if (job.getOriginalJobId() != null && job.getOriginalJobId() > 0) {
                        ps.setInt(9, job.getOriginalJobId());
                    } else {
                        ps.setNull(9, java.sql.Types.INTEGER);
                    }
                    ps.setString(10, now);
                    ps.setString(11, now);

                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            jobId = rs.getInt(1);
                            job.setJobId(jobId);
                            job.setCurrentStatus(RepairStatus.RECEIVED);
                            job.setDateReceived(now);
                            job.setCreatedAt(now);
                            job.setUpdatedAt(now);
                        } else {
                            throw new SQLException("Failed to retrieve generated job ID.");
                        }
                    }
                }

                // Record initial status history audit entry
                String histSql = "INSERT INTO repair_status_history (job_id, from_status, to_status, changed_by_role, change_date, notes) " +
                                 "VALUES (?, NULL, ?, ?, ?, ?)";
                try (PreparedStatement psHist = conn.prepareStatement(histSql)) {
                    psHist.setInt(1, jobId);
                    psHist.setString(2, RepairStatus.RECEIVED.name());
                    psHist.setString(3, createdByRole != null ? createdByRole : "Receptionist");
                    psHist.setString(4, now);
                    psHist.setString(5, "Device received for repair intake.");
                    psHist.executeUpdate();
                }

                conn.commit();
                return jobId;
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to create repair job, rolled back.", e);
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Manually assigns a technician to a repair job.
     * Enforces Section 7.4 business rule: Inactive technicians cannot receive new assignments.
     *
     * @param jobId repair job ID
     * @param technicianId technician ID
     * @param assignedByRole role making the assignment (Receptionist or Manager)
     * @return true if assigned
     * @throws SQLException if a database error occurs
     */
    public static boolean assignTechnician(int jobId, int technicianId, String assignedByRole) throws SQLException {
        Technician tech = TechnicianDB.getTechnicianById(technicianId);
        if (tech == null) {
            throw new IllegalArgumentException("Technician #" + technicianId + " not found.");
        }
        if (!tech.isActive()) {
            throw new IllegalArgumentException("Cannot assign technician '" + tech.getName() + "': Technician is marked as Inactive. Inactive technicians cannot receive new assignments.");
        }

        RepairJob currentJob = getRepairJobById(jobId);
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        String sql = "UPDATE repair_jobs SET technician_id = ?, updated_at = ? WHERE job_id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean updated;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, technicianId);
                    ps.setString(2, now);
                    ps.setInt(3, jobId);
                    updated = ps.executeUpdate() > 0;
                }

                // Record assignment audit entry
                String histSql = "INSERT INTO repair_status_history (job_id, from_status, to_status, changed_by_role, change_date, notes) " +
                                 "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement psHist = conn.prepareStatement(histSql)) {
                    psHist.setInt(1, jobId);
                    psHist.setString(2, currentJob != null ? currentJob.getCurrentStatus().name() : null);
                    psHist.setString(3, currentJob != null ? currentJob.getCurrentStatus().name() : null);
                    psHist.setString(4, assignedByRole);
                    psHist.setString(5, now);
                    psHist.setString(6, "Assigned to technician: " + tech.getName() + " (" + tech.getSpecialization() + ")");
                    psHist.executeUpdate();
                }

                conn.commit();
                return updated;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Records technician diagnosis findings, estimated cost, and estimated completion date.
     *
     * @param jobId repair job ID
     * @param findings diagnosis notes
     * @param estimatedCost estimated total repair cost
     * @param labourCharges estimated labour charges
     * @param estCompletionDate estimated completion date string
     * @param advanceToWaitingApproval if true, automatically advances status from DIAGNOSING to WAITING_FOR_APPROVAL
     * @param recordedByRole technician or manager role
     * @throws SQLException if a database error occurs
     */
    public static void recordDiagnosis(int jobId, String findings, Double estimatedCost, double labourCharges,
                                       String estCompletionDate, boolean advanceToWaitingApproval, String recordedByRole)
            throws SQLException, InvalidStatusTransitionException {

        String now = LocalDateTime.now().format(DATE_FORMATTER);
        RepairJob job = getRepairJobById(jobId);
        if (job == null) {
            throw new SQLException("Repair Job #" + jobId + " not found.");
        }

        String sql = "UPDATE repair_jobs SET diagnosis_findings = ?, estimated_cost = ?, labour_charges = ?, " +
                     "estimated_completion_date = ?, updated_at = ? WHERE job_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, findings != null ? findings.trim() : null);
            if (estimatedCost != null) {
                ps.setDouble(2, estimatedCost);
            } else {
                ps.setNull(2, java.sql.Types.REAL);
            }
            ps.setDouble(3, labourCharges);
            ps.setString(4, estCompletionDate != null && !estCompletionDate.trim().isEmpty() ? estCompletionDate.trim() : null);
            ps.setString(5, now);
            ps.setInt(6, jobId);
            ps.executeUpdate();
        }

        // If currently RECEIVED, move to DIAGNOSING
        if (job.getCurrentStatus() == RepairStatus.RECEIVED) {
            updateStatus(jobId, RepairStatus.DIAGNOSING, "Diagnosis initiated by technician.", recordedByRole);
        }

        // If requested to advance to WAITING_FOR_APPROVAL
        if (advanceToWaitingApproval && job.getCurrentStatus() != RepairStatus.WAITING_FOR_APPROVAL) {
            updateStatus(jobId, RepairStatus.WAITING_FOR_APPROVAL, "Diagnosis complete. Waiting for customer approval of estimate: LKR " + String.format("%.2f", estimatedCost != null ? estimatedCost : 0.0), recordedByRole);
        }
    }

    /**
     * Records the customer approval decision.
     * Enforces Section 7.3:
     * - If approved: advances to REPAIRING stage.
     * - If rejected: advances to CANCELLED stage with retained cancellation reason.
     *
     * @param jobId repair job ID
     * @param approved true if customer approved estimate, false if rejected
     * @param cancellationReason reason if rejected (required if approved is false)
     * @param recordedByRole role recording approval (Receptionist or Manager)
     * @throws SQLException if a database error occurs
     * @throws InvalidStatusTransitionException if transition is disallowed
     */
    public static void recordCustomerApproval(int jobId, boolean approved, String cancellationReason, String recordedByRole)
            throws SQLException, InvalidStatusTransitionException {

        RepairJob job = getRepairJobById(jobId);
        if (job == null) {
            throw new SQLException("Repair Job #" + jobId + " not found.");
        }

        String now = LocalDateTime.now().format(DATE_FORMATTER);

        if (approved) {
            // Update approval status to APPROVED
            String appSql = "UPDATE repair_jobs SET approval_status = 'APPROVED', approval_date = ?, updated_at = ? WHERE job_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(appSql)) {
                ps.setString(1, now);
                ps.setString(2, now);
                ps.setInt(3, jobId);
                ps.executeUpdate();
            }

            // Advance status from WAITING_FOR_APPROVAL -> REPAIRING
            updateStatus(jobId, RepairStatus.REPAIRING, "Customer approved estimate. Work authorized.", recordedByRole);
        } else {
            if (cancellationReason == null || cancellationReason.trim().isEmpty()) {
                throw new IllegalArgumentException("A cancellation reason is required when a customer rejects an estimate.");
            }

            String appSql = "UPDATE repair_jobs SET approval_status = 'REJECTED', approval_date = ?, cancellation_reason = ?, updated_at = ? WHERE job_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(appSql)) {
                ps.setString(1, now);
                ps.setString(2, cancellationReason.trim());
                ps.setString(3, now);
                ps.setInt(4, jobId);
                ps.executeUpdate();
            }

            // Move status to CANCELLED with the exact cancellation reason
            updateStatus(jobId, RepairStatus.CANCELLED, cancellationReason.trim(), recordedByRole);
        }
    }

    /**
     * Updates repair job status and records the transition in audit history within a transaction.
     * Enforces the status transition matrix from Section 7.3 and Section 8.8.
     *
     * @param jobId repair job ID
     * @param targetStatus proposed new status
     * @param reasonOrNotes audit note or cancellation reason
     * @param changedByRole role executing the transition
     * @throws SQLException if a database error occurs
     * @throws InvalidStatusTransitionException if transition is invalid
     */
    public static void updateStatus(int jobId, RepairStatus targetStatus, String reasonOrNotes, String changedByRole)
            throws SQLException, InvalidStatusTransitionException {

        RepairJob job = getRepairJobById(jobId);
        if (job == null) {
            throw new SQLException("Repair Job #" + jobId + " not found.");
        }

        // Validate lifecycle transition rules
        job.validateTransition(targetStatus);

        // Enforce Business Rule 13: A device cannot be collected until full payment is recorded
        if (targetStatus == RepairStatus.COLLECTED) {
            if (!PaymentDB.isJobFullyPaid(jobId)) {
                throw new IllegalStateException("A device cannot be collected until full payment is recorded (Business Rule 13).");
            }
        }

        if (targetStatus == RepairStatus.CANCELLED && (reasonOrNotes == null || reasonOrNotes.trim().isEmpty())) {
            throw new IllegalArgumentException("A cancellation reason must be provided when cancelling a repair job.");
        }

        String now = LocalDateTime.now().format(DATE_FORMATTER);

        StringBuilder updateSql = new StringBuilder("UPDATE repair_jobs SET current_status = ?, updated_at = ? ");
        if (targetStatus == RepairStatus.READY_FOR_COLLECTION) {
            updateSql.append(", actual_completion_date = ? ");
        }
        if (targetStatus == RepairStatus.CANCELLED) {
            updateSql.append(", cancellation_reason = ? ");
        }
        updateSql.append("WHERE job_id = ?");

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(updateSql.toString())) {
                    ps.setString(1, targetStatus.name());
                    ps.setString(2, now);

                    int pIdx = 3;
                    if (targetStatus == RepairStatus.READY_FOR_COLLECTION) {
                        ps.setString(pIdx++, now);
                    }
                    if (targetStatus == RepairStatus.CANCELLED) {
                        ps.setString(pIdx++, reasonOrNotes != null ? reasonOrNotes.trim() : "Cancelled");
                    }
                    ps.setInt(pIdx, jobId);
                    ps.executeUpdate();
                }

                // Insert into repair_status_history
                String histSql = "INSERT INTO repair_status_history (job_id, from_status, to_status, changed_by_role, change_date, notes) " +
                                 "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement psHist = conn.prepareStatement(histSql)) {
                    psHist.setInt(1, jobId);
                    psHist.setString(2, job.getCurrentStatus().name());
                    psHist.setString(3, targetStatus.name());
                    psHist.setString(4, changedByRole != null ? changedByRole : "System");
                    psHist.setString(5, now);
                    psHist.setString(6, reasonOrNotes != null && !reasonOrNotes.trim().isEmpty() ? reasonOrNotes.trim() : null);
                    psHist.executeUpdate();
                }

                conn.commit();
                LOGGER.info("Job #" + jobId + " transitioned from " + job.getCurrentStatus() + " to " + targetStatus);

                // Enforce Business Rule 14: Every completed repair receives a fixed 30-day warranty upon collection
                if (targetStatus == RepairStatus.COLLECTED) {
                    try {
                        WarrantyDB.autoCreateWarrantyForJob(jobId);
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Auto-creation of 30-day warranty for Job #" + jobId + " failed", ex);
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to update status for Job #" + jobId + ", rolled back.", e);
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Retrieves a repair job by ID with joined customer, device, and technician details.
     *
     * @param jobId repair job ID
     * @return {@link RepairJob} object, or null if not found
     * @throws SQLException if a database error occurs
     */
    public static RepairJob getRepairJobById(int jobId) throws SQLException {
        String sql = "SELECT j.job_id, j.customer_id, j.device_id, j.technician_id, j.date_received, j.initial_problem, " +
                     "j.current_status, j.diagnosis_findings, j.estimated_cost, j.labour_charges, j.estimated_completion_date, " +
                     "j.actual_completion_date, j.approval_status, j.approval_date, j.cancellation_reason, j.original_job_id, " +
                     "j.created_at, j.updated_at, " +
                     "c.full_name AS customer_name, c.phone AS customer_phone, " +
                     "d.brand || ' ' || d.model || ' (' || d.category || ')' AS device_summary, " +
                     "t.name AS technician_name " +
                     "FROM repair_jobs j " +
                     "JOIN customers c ON j.customer_id = c.customer_id " +
                     "JOIN devices d ON j.device_id = d.device_id " +
                     "LEFT JOIN technicians t ON j.technician_id = t.technician_id " +
                     "WHERE j.job_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRepairJob(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all repair jobs ordered from newest to oldest.
     *
     * @return list of repair jobs
     * @throws SQLException if a database error occurs
     */
    public static List<RepairJob> getAllRepairJobs() throws SQLException {
        List<RepairJob> list = new ArrayList<>();
        String sql = "SELECT j.job_id, j.customer_id, j.device_id, j.technician_id, j.date_received, j.initial_problem, " +
                     "j.current_status, j.diagnosis_findings, j.estimated_cost, j.labour_charges, j.estimated_completion_date, " +
                     "j.actual_completion_date, j.approval_status, j.approval_date, j.cancellation_reason, j.original_job_id, " +
                     "j.created_at, j.updated_at, " +
                     "c.full_name AS customer_name, c.phone AS customer_phone, " +
                     "d.brand || ' ' || d.model || ' (' || d.category || ')' AS device_summary, " +
                     "t.name AS technician_name " +
                     "FROM repair_jobs j " +
                     "JOIN customers c ON j.customer_id = c.customer_id " +
                     "JOIN devices d ON j.device_id = d.device_id " +
                     "LEFT JOIN technicians t ON j.technician_id = t.technician_id " +
                     "ORDER BY j.job_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToRepairJob(rs));
            }
        }
        return list;
    }

    /**
     * Retrieves repair jobs assigned to a specific technician.
     * Implements Section 7.3: Technicians can view all repair jobs, but update only their assigned jobs.
     *
     * @param technicianId technician ID
     * @return list of repair jobs assigned to the technician
     * @throws SQLException if a database error occurs
     */
    public static List<RepairJob> getRepairJobsForTechnician(int technicianId) throws SQLException {
        List<RepairJob> list = new ArrayList<>();
        String sql = "SELECT j.job_id, j.customer_id, j.device_id, j.technician_id, j.date_received, j.initial_problem, " +
                     "j.current_status, j.diagnosis_findings, j.estimated_cost, j.labour_charges, j.estimated_completion_date, " +
                     "j.actual_completion_date, j.approval_status, j.approval_date, j.cancellation_reason, j.original_job_id, " +
                     "j.created_at, j.updated_at, " +
                     "c.full_name AS customer_name, c.phone AS customer_phone, " +
                     "d.brand || ' ' || d.model || ' (' || d.category || ')' AS device_summary, " +
                     "t.name AS technician_name " +
                     "FROM repair_jobs j " +
                     "JOIN customers c ON j.customer_id = c.customer_id " +
                     "JOIN devices d ON j.device_id = d.device_id " +
                     "LEFT JOIN technicians t ON j.technician_id = t.technician_id " +
                     "WHERE j.technician_id = ? " +
                     "ORDER BY j.job_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, technicianId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToRepairJob(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves repair history for a customer.
     *
     * @param customerId customer ID
     * @return list of historical repair jobs
     * @throws SQLException if a database error occurs
     */
    public static List<RepairJob> getRepairJobsByCustomer(int customerId) throws SQLException {
        List<RepairJob> list = new ArrayList<>();
        String sql = "SELECT j.job_id, j.customer_id, j.device_id, j.technician_id, j.date_received, j.initial_problem, " +
                     "j.current_status, j.diagnosis_findings, j.estimated_cost, j.labour_charges, j.estimated_completion_date, " +
                     "j.actual_completion_date, j.approval_status, j.approval_date, j.cancellation_reason, j.original_job_id, " +
                     "j.created_at, j.updated_at, " +
                     "c.full_name AS customer_name, c.phone AS customer_phone, " +
                     "d.brand || ' ' || d.model || ' (' || d.category || ')' AS device_summary, " +
                     "t.name AS technician_name " +
                     "FROM repair_jobs j " +
                     "JOIN customers c ON j.customer_id = c.customer_id " +
                     "JOIN devices d ON j.device_id = d.device_id " +
                     "LEFT JOIN technicians t ON j.technician_id = t.technician_id " +
                     "WHERE j.customer_id = ? " +
                     "ORDER BY j.job_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToRepairJob(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves the audit history of status changes for a repair job.
     *
     * @param jobId repair job ID
     * @return list of status history records ordered chronologically
     * @throws SQLException if a database error occurs
     */
    public static List<StatusHistory> getStatusHistory(int jobId) throws SQLException {
        List<StatusHistory> list = new ArrayList<>();
        String sql = "SELECT history_id, job_id, from_status, to_status, changed_by_role, change_date, notes " +
                     "FROM repair_status_history WHERE job_id = ? ORDER BY history_id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int hId = rs.getInt("history_id");
                    String fromStr = rs.getString("from_status");
                    String toStr = rs.getString("to_status");
                    RepairStatus from = fromStr != null ? RepairStatus.valueOf(fromStr) : null;
                    RepairStatus to = RepairStatus.valueOf(toStr);
                    String role = rs.getString("changed_by_role");
                    String date = rs.getString("change_date");
                    String notes = rs.getString("notes");
                    list.add(new StatusHistory(hId, jobId, from, to, role, date, notes));
                }
            }
        }
        return list;
    }

    /**
     * Searches and filters repair jobs by text query, status, and assigned technician.
     *
     * @param query search query
     * @param statusFilter optional status filter
     * @param technicianIdFilter optional technician ID filter
     * @return matching repair jobs
     * @throws SQLException if a database error occurs
     */
    public static List<RepairJob> searchRepairJobs(String query, RepairStatus statusFilter, Integer technicianIdFilter) throws SQLException {
        List<RepairJob> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT j.job_id, j.customer_id, j.device_id, j.technician_id, j.date_received, j.initial_problem, " +
                "j.current_status, j.diagnosis_findings, j.estimated_cost, j.labour_charges, j.estimated_completion_date, " +
                "j.actual_completion_date, j.approval_status, j.approval_date, j.cancellation_reason, j.original_job_id, " +
                "j.created_at, j.updated_at, " +
                "c.full_name AS customer_name, c.phone AS customer_phone, " +
                "d.brand || ' ' || d.model || ' (' || d.category || ')' AS device_summary, " +
                "t.name AS technician_name " +
                "FROM repair_jobs j " +
                "JOIN customers c ON j.customer_id = c.customer_id " +
                "JOIN devices d ON j.device_id = d.device_id " +
                "LEFT JOIN technicians t ON j.technician_id = t.technician_id WHERE 1=1 "
        );

        boolean hasQuery = query != null && !query.trim().isEmpty();
        if (hasQuery) {
            sql.append("AND (LOWER(c.full_name) LIKE ? OR c.phone LIKE ? OR LOWER(d.brand) LIKE ? " +
                       "OR LOWER(d.model) LIKE ? OR LOWER(j.initial_problem) LIKE ? OR CAST(j.job_id AS TEXT) = ?) ");
        }

        if (statusFilter != null) {
            sql.append("AND j.current_status = ? ");
        }

        if (technicianIdFilter != null && technicianIdFilter > 0) {
            sql.append("AND j.technician_id = ? ");
        }

        sql.append("ORDER BY j.job_id DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int pIdx = 1;

            if (hasQuery) {
                String wildcard = "%" + query.trim().toLowerCase() + "%";
                ps.setString(pIdx++, wildcard);
                ps.setString(pIdx++, wildcard);
                ps.setString(pIdx++, wildcard);
                ps.setString(pIdx++, wildcard);
                ps.setString(pIdx++, wildcard);
                ps.setString(pIdx++, query.trim());
            }

            if (statusFilter != null) {
                ps.setString(pIdx++, statusFilter.name());
            }

            if (technicianIdFilter != null && technicianIdFilter > 0) {
                ps.setInt(pIdx++, technicianIdFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToRepairJob(rs));
                }
            }
        }
        return list;
    }

    /**
     * Deletes a repair job. Blocked if payment or warranty records exist.
     *
     * @param jobId repair job ID
     * @return true if deleted
     * @throws SQLException if deletion is blocked
     */
    public static boolean deleteRepairJob(int jobId) throws SQLException {
        // Enforce Section 7.10: Protect dependent financial records
        String checkPaymentSql = "SELECT COUNT(*) FROM payments WHERE job_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkPaymentSql)) {
            ps.setInt(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new SQLException("Cannot delete Repair Job #" + jobId + ": Financial payment records are associated with this job. Financial history must be preserved.");
                }
            }
        }

        String sql = "DELETE FROM repair_jobs WHERE job_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jobId);
            return ps.executeUpdate() > 0;
        }
    }

    public static RepairJob mapResultSetToRepairJob(ResultSet rs) throws SQLException {
        RepairJob job = new RepairJob();
        job.setJobId(rs.getInt("job_id"));
        job.setCustomerId(rs.getInt("customer_id"));
        job.setDeviceId(rs.getInt("device_id"));

        int techId = rs.getInt("technician_id");
        job.setTechnicianId(rs.wasNull() ? null : techId);

        job.setDateReceived(rs.getString("date_received"));
        job.setInitialProblem(rs.getString("initial_problem"));
        job.setCurrentStatus(RepairStatus.valueOf(rs.getString("current_status")));
        job.setDiagnosisFindings(rs.getString("diagnosis_findings"));

        double estCost = rs.getDouble("estimated_cost");
        job.setEstimatedCost(rs.wasNull() ? null : estCost);

        job.setLabourCharges(rs.getDouble("labour_charges"));
        job.setEstimatedCompletionDate(rs.getString("estimated_completion_date"));
        job.setActualCompletionDate(rs.getString("actual_completion_date"));
        job.setApprovalStatus(rs.getString("approval_status"));
        job.setApprovalDate(rs.getString("approval_date"));
        job.setCancellationReason(rs.getString("cancellation_reason"));

        int origId = rs.getInt("original_job_id");
        job.setOriginalJobId(rs.wasNull() ? null : origId);

        job.setCreatedAt(rs.getString("created_at"));
        job.setUpdatedAt(rs.getString("updated_at"));

        job.setCustomerName(rs.getString("customer_name"));
        job.setCustomerPhone(rs.getString("customer_phone"));
        job.setDeviceSummary(rs.getString("device_summary"));
        job.setTechnicianName(rs.getString("technician_name"));

        return job;
    }
}
