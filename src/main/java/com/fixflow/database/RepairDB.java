package com.fixflow.database;

import com.fixflow.enums.RepairStatus;
import com.fixflow.model.RepairJob;
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
 * Data access operations for repair jobs, technician assignments, and status updates.
 */
public class RepairDB {

    private static final Logger LOGGER = Logger.getLogger(RepairDB.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private RepairDB() {
        // DAO utility
    }

    /**
     * Creates a new repair job with initial RECEIVED status.
     *
     * @param job repair job model
     * @return generated job_id
     * @throws SQLException if a database error occurs
     */
    public static int createRepairJob(RepairJob job) throws SQLException {
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        String sql = "INSERT INTO repair_jobs (customer_id, device_id, technician_id, date_received, initial_problem, " +
                     "current_status, labour_charges, notes, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

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
            ps.setString(8, job.getNotes());
            ps.setString(9, now);
            ps.setString(10, now);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int jobId = rs.getInt(1);
                    job.setJobId(jobId);
                    job.setCurrentStatus(RepairStatus.RECEIVED);
                    job.setDateReceived(now);
                    job.setCreatedAt(now);
                    job.setUpdatedAt(now);
                    return jobId;
                }
            }
        }
        throw new SQLException("Failed to retrieve generated job ID.");
    }

    /**
     * Persists a technician assignment to a repair job in the database.
     *
     * @param jobId repair job ID
     * @param technicianId technician ID
     * @return true if updated
     * @throws SQLException if a database error occurs
     */
    public static boolean assignTechnician(int jobId, int technicianId) throws SQLException {
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        String sql = "UPDATE repair_jobs SET technician_id = ?, updated_at = ? WHERE job_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, technicianId);
            ps.setString(2, now);
            ps.setInt(3, jobId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Records technician diagnosis findings, estimated cost, and estimated completion date.
     *
     * @param jobId repair job ID
     * @param findings diagnosis notes
     * @param estimatedCost estimated cost
     * @param labourCharges labour charges
     * @param estCompletionDate estimated completion date
     * @throws SQLException if a database error occurs
     */
    public static void recordDiagnosis(int jobId, String findings, Double estimatedCost, double labourCharges,
                                       String estCompletionDate) throws SQLException {
        String now = LocalDateTime.now().format(DATE_FORMATTER);
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
    }

    /**
     * Updates repair job status directly in the database.
     *
     * @param jobId repair job ID
     * @param targetStatus target status
     * @param notes optional notes or cancellation reason
     * @throws SQLException if a database error occurs
     */
    public static void updateStatus(int jobId, RepairStatus targetStatus, String notes) throws SQLException {
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        StringBuilder sql = new StringBuilder("UPDATE repair_jobs SET current_status = ?, updated_at = ? ");

        if (targetStatus == RepairStatus.COMPLETED) {
            sql.append(", actual_completion_date = ? ");
        }
        if (notes != null && !notes.trim().isEmpty()) {
            sql.append(", notes = ? ");
        }
        sql.append("WHERE job_id = ?");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, targetStatus.name());
            ps.setString(2, now);

            int pIdx = 3;
            if (targetStatus == RepairStatus.COMPLETED) {
                ps.setString(pIdx++, now);
            }
            if (notes != null && !notes.trim().isEmpty()) {
                ps.setString(pIdx++, notes.trim());
            }
            ps.setInt(pIdx, jobId);
            ps.executeUpdate();
        }
    }

    /**
     * Updates notes for a repair job.
     */
    public static void updateNotes(int jobId, String notes) throws SQLException {
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        String sql = "UPDATE repair_jobs SET notes = ?, updated_at = ? WHERE job_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, notes);
            ps.setString(2, now);
            ps.setInt(3, jobId);
            ps.executeUpdate();
        }
    }

    /**
     * Retrieves a repair job by ID with joined customer, device, and technician details.
     *
     * @param jobId repair job ID
     * @return RepairJob object, or null if not found
     * @throws SQLException if a database error occurs
     */
    public static RepairJob getRepairJobById(int jobId) throws SQLException {
        String sql = "SELECT j.job_id, j.customer_id, j.device_id, j.technician_id, j.date_received, j.initial_problem, " +
                     "j.current_status, j.diagnosis_findings, j.estimated_cost, j.labour_charges, j.estimated_completion_date, " +
                     "j.actual_completion_date, j.notes, j.created_at, j.updated_at, " +
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
                     "j.actual_completion_date, j.notes, j.created_at, j.updated_at, " +
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
     *
     * @param technicianId technician ID
     * @return list of repair jobs assigned to the technician
     * @throws SQLException if a database error occurs
     */
    public static List<RepairJob> getRepairJobsForTechnician(int technicianId) throws SQLException {
        List<RepairJob> list = new ArrayList<>();
        String sql = "SELECT j.job_id, j.customer_id, j.device_id, j.technician_id, j.date_received, j.initial_problem, " +
                     "j.current_status, j.diagnosis_findings, j.estimated_cost, j.labour_charges, j.estimated_completion_date, " +
                     "j.actual_completion_date, j.notes, j.created_at, j.updated_at, " +
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
                     "j.actual_completion_date, j.notes, j.created_at, j.updated_at, " +
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
                "j.actual_completion_date, j.notes, j.created_at, j.updated_at, " +
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
     * Deletes a repair job. Blocked if payment records exist.
     *
     * @param jobId repair job ID
     * @return true if deleted
     * @throws SQLException if deletion is blocked
     */
    public static boolean deleteRepairJob(int jobId) throws SQLException {
        String checkPaymentSql = "SELECT COUNT(*) FROM payments WHERE job_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkPaymentSql)) {
            ps.setInt(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new SQLException("Cannot delete Repair Job #" + jobId + ": Payment records exist for this job.");
                }
            }
        }

        // Also delete any part usage
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM repair_part_usage WHERE job_id = ?")) {
            ps.setInt(1, jobId);
            ps.executeUpdate();
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
        job.setNotes(rs.getString("notes"));

        job.setCreatedAt(rs.getString("created_at"));
        job.setUpdatedAt(rs.getString("updated_at"));

        job.setCustomerName(rs.getString("customer_name"));
        job.setCustomerPhone(rs.getString("customer_phone"));
        job.setDeviceSummary(rs.getString("device_summary"));
        job.setTechnicianName(rs.getString("technician_name"));

        return job;
    }
}
