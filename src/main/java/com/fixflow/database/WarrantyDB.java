package com.fixflow.database;

import com.fixflow.enums.RepairStatus;
import com.fixflow.enums.WarrantyStatus;
import com.fixflow.model.RepairJob;
import com.fixflow.model.Warranty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data access operations for 30-day warranty tracking, validity verification, and warranty re-repair claims.
 * Implements Section 7.7, Section 8.14, and Section 8.15 of the FixFlow Project Specification.
 */
public class WarrantyDB {

    private static final Logger LOGGER = Logger.getLogger(WarrantyDB.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final int WARRANTY_DAYS = 30;

    /**
     * Automatically registers a fixed 30-day warranty when a repair job is collected.
     * Enforces Section 7.7 and Section 8.14.
     *
     * @param jobId completed repair job ID
     * @return generated warranty ID (or existing if already present)
     * @throws SQLException if a database error occurs
     */
    public static int autoCreateWarrantyForJob(int jobId) throws SQLException {
        Warranty existing = getWarrantyByJobId(jobId);
        if (existing != null) {
            return existing.getWarrantyId();
        }

        LocalDate today = LocalDate.now();
        LocalDate expiry = today.plusDays(WARRANTY_DAYS);

        String startDate = today.toString();
        String expiryDate = expiry.toString();
        String now = LocalDateTime.now().format(DATE_FORMATTER);

        String sql = "INSERT INTO warranties (job_id, start_date, expiry_date, status, created_at) VALUES (?, ?, ?, 'ACTIVE', ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, jobId);
            ps.setString(2, startDate);
            ps.setString(3, expiryDate);
            ps.setString(4, now);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int wId = rs.getInt(1);
                    LOGGER.info(String.format("30-day warranty #%d issued for Job #%d: Valid %s to %s.", wId, jobId, startDate, expiryDate));
                    return wId;
                }
            }
        }
        throw new SQLException("Failed to retrieve generated warranty ID.");
    }

    /**
     * Retrieves warranty details for a repair job.
     *
     * @param jobId repair job ID
     * @return {@link Warranty} record or null if not found
     * @throws SQLException if a database error occurs
     */
    public static Warranty getWarrantyByJobId(int jobId) throws SQLException {
        String sql = getWarrantySelectQuery() + " WHERE w.job_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return checkAndMapWarranty(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves warranty details by warranty ID.
     *
     * @param warrantyId warranty ID
     * @return {@link Warranty} record or null if not found
     * @throws SQLException if a database error occurs
     */
    public static Warranty getWarrantyById(int warrantyId) throws SQLException {
        String sql = getWarrantySelectQuery() + " WHERE w.warranty_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, warrantyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return checkAndMapWarranty(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all warranty records, automatically checking and updating overdue active warranties to EXPIRED.
     *
     * @return list of warranties
     * @throws SQLException if a database error occurs
     */
    public static List<Warranty> getAllWarranties() throws SQLException {
        refreshExpiredWarranties();

        List<Warranty> list = new ArrayList<>();
        String sql = getWarrantySelectQuery() + " ORDER BY w.warranty_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(checkAndMapWarranty(rs));
            }
        }
        return list;
    }

    /**
     * Searches warranties by customer name, phone, serial number, device model, or job/warranty ID.
     *
     * @param query search query
     * @param statusFilter optional status filter (ACTIVE, EXPIRED, CLAIMED, or null)
     * @return list of matching warranties
     * @throws SQLException if a database error occurs
     */
    public static List<Warranty> searchWarranties(String query, String statusFilter) throws SQLException {
        refreshExpiredWarranties();

        StringBuilder sql = new StringBuilder(getWarrantySelectQuery()).append(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            sql.append(" AND w.status = ? ");
            params.add(statusFilter.trim().toUpperCase());
        }

        if (query != null && !query.trim().isEmpty()) {
            String pattern = "%" + query.trim().toLowerCase() + "%";
            sql.append(" AND (LOWER(c.full_name) LIKE ? OR c.phone LIKE ? " +
                       "      OR LOWER(d.brand || ' ' || d.model) LIKE ? OR LOWER(d.serial_number) LIKE ? " +
                       "      OR CAST(w.job_id AS TEXT) LIKE ? OR CAST(w.warranty_id AS TEXT) LIKE ?) ");
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        sql.append(" ORDER BY w.warranty_id DESC");

        List<Warranty> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(checkAndMapWarranty(rs));
                }
            }
        }
        return list;
    }

    /**
     * Processes a warranty claim and initiates a new linked re-repair job.
     * Implements Section 7.7 and Section 8.15:
     * 1. Validates that warranty is ACTIVE and within 30 days.
     * 2. Updates warranty status to CLAIMED.
     * 3. Creates a new repair job linked to the original repair job (original_job_id).
     * 4. Logs audit entries for both jobs.
     *
     * @param warrantyId warranty ID
     * @param claimProblem reported failure or defect
     * @param recordedByRole role recording claim (Receptionist)
     * @return newly generated re-repair job ID
     * @throws SQLException if a database error occurs
     * @throws IllegalStateException if warranty is expired or already claimed
     */
    public static int claimWarranty(int warrantyId, String claimProblem, String recordedByRole) throws SQLException {
        Warranty warranty = getWarrantyById(warrantyId);
        if (warranty == null) {
            throw new SQLException("Warranty #" + warrantyId + " not found.");
        }

        if (!warranty.isCurrentlyValid()) {
            throw new IllegalStateException(String.format(
                    "Warranty #%d is ineligible for claim. Current status is %s (Expired on %s).",
                    warrantyId, warranty.getStatus().getDisplayName(), warranty.getExpiryDate()));
        }

        RepairJob origJob = RepairDB.getRepairJobById(warranty.getJobId());
        if (origJob == null) {
            throw new SQLException("Original Repair Job #" + warranty.getJobId() + " not found.");
        }

        String now = LocalDateTime.now().format(DATE_FORMATTER);
        String today = LocalDate.now().toString();

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Mark warranty as CLAIMED
                String updateWarrantySql = "UPDATE warranties SET status = 'CLAIMED' WHERE warranty_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateWarrantySql)) {
                    ps.setInt(1, warrantyId);
                    ps.executeUpdate();
                }

                // 2. Create new linked repair job
                String initialProblem = "[Warranty Claim for Job #" + origJob.getJobId() + "] " +
                                       (claimProblem != null && !claimProblem.trim().isEmpty() ? claimProblem.trim() : "Recurrent defect reported under warranty");

                String insertJobSql = "INSERT INTO repair_jobs (customer_id, device_id, technician_id, date_received, " +
                                      "initial_problem, current_status, diagnosis_findings, estimated_cost, labour_charges, " +
                                      "approval_status, approval_date, original_job_id, created_at, updated_at) " +
                                      "VALUES (?, ?, NULL, ?, ?, 'RECEIVED', 'Covered under 30-day warranty', 0.0, 0.0, 'APPROVED', ?, ?, ?, ?)";

                int newJobId;
                try (PreparedStatement psJob = conn.prepareStatement(insertJobSql, Statement.RETURN_GENERATED_KEYS)) {
                    psJob.setInt(1, origJob.getCustomerId());
                    psJob.setInt(2, origJob.getDeviceId());
                    psJob.setString(3, today);
                    psJob.setString(4, initialProblem);
                    psJob.setString(5, now); // pre-approved
                    psJob.setInt(6, origJob.getJobId()); // Section 7.7: linked to original repair
                    psJob.setString(7, now);
                    psJob.setString(8, now);
                    psJob.executeUpdate();

                    try (ResultSet rs = psJob.getGeneratedKeys()) {
                        if (rs.next()) {
                            newJobId = rs.getInt(1);
                        } else {
                            throw new SQLException("Failed to obtain ID for new warranty repair job.");
                        }
                    }
                }

                // 3. Status history for new job
                String histNew = "INSERT INTO repair_status_history (job_id, from_status, to_status, changed_by_role, change_date, notes) " +
                                 "VALUES (?, NULL, 'RECEIVED', ?, ?, ?)";
                try (PreparedStatement psHist = conn.prepareStatement(histNew)) {
                    psHist.setInt(1, newJobId);
                    psHist.setString(2, recordedByRole != null ? recordedByRole : "Receptionist");
                    psHist.setString(3, now);
                    psHist.setString(4, "Warranty re-repair initiated under Warranty #" + warrantyId + " (Original Job #" + origJob.getJobId() + ").");
                    psHist.executeUpdate();
                }

                // 4. Audit note in original job history
                String histOrig = "INSERT INTO repair_status_history (job_id, from_status, to_status, changed_by_role, change_date, notes) " +
                                  "VALUES (?, 'COLLECTED', 'COLLECTED', ?, ?, ?)";
                try (PreparedStatement psHist = conn.prepareStatement(histOrig)) {
                    psHist.setInt(1, origJob.getJobId());
                    psHist.setString(2, recordedByRole != null ? recordedByRole : "Receptionist");
                    psHist.setString(3, now);
                    psHist.setString(4, "Customer claimed 30-day warranty. Re-repair Job #" + newJobId + " created.");
                    psHist.executeUpdate();
                }

                conn.commit();
                LOGGER.info(String.format("Warranty #%d claimed. New linked re-repair Job #%d created for Original Job #%d.",
                        warrantyId, newJobId, origJob.getJobId()));
                return newJobId;
            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to claim warranty #" + warrantyId + ", rolled back.", e);
                if (e instanceof SQLException) throw (SQLException) e;
                throw new SQLException("Failed to process warranty claim: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Deletes a warranty record. Restricted to administrators.
     * Protected if warranty has already been claimed (§8.19).
     *
     * @param warrantyId warranty ID
     * @return true if deleted
     * @throws SQLException if warranty is CLAIMED or database error occurs
     */
    public static boolean deleteWarranty(int warrantyId) throws SQLException {
        Warranty w = getWarrantyById(warrantyId);
        if (w == null) return false;

        if (w.getStatus() == WarrantyStatus.CLAIMED) {
            throw new SQLException("Cannot delete Warranty #" + warrantyId + ": A linked warranty claim repair job exists (§8.19).");
        }

        String sql = "DELETE FROM warranties WHERE warranty_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, warrantyId);
            return ps.executeUpdate() > 0;
        }
    }

    private static void refreshExpiredWarranties() {
        String sql = "UPDATE warranties SET status = 'EXPIRED' WHERE status = 'ACTIVE' AND date(expiry_date) < date('now')";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to refresh expired warranties", e);
        }
    }

    private static String getWarrantySelectQuery() {
        return "SELECT w.warranty_id, w.job_id, w.start_date, w.expiry_date, w.status, w.created_at, " +
               "       c.full_name AS customer_name, c.phone AS customer_phone, " +
               "       (d.brand || ' ' || d.model) AS device_summary, d.serial_number, " +
               "       r.initial_problem " +
               "FROM warranties w " +
               "JOIN repair_jobs r ON w.job_id = r.job_id " +
               "JOIN customers c ON r.customer_id = c.customer_id " +
               "JOIN devices d ON r.device_id = d.device_id";
    }

    private static Warranty checkAndMapWarranty(ResultSet rs) throws SQLException {
        String statusStr = rs.getString("status");
        WarrantyStatus status;
        try {
            status = WarrantyStatus.valueOf(statusStr);
        } catch (Exception e) {
            status = WarrantyStatus.ACTIVE;
        }

        Warranty w = new Warranty(
                rs.getInt("warranty_id"),
                rs.getInt("job_id"),
                rs.getString("start_date"),
                rs.getString("expiry_date"),
                status,
                rs.getString("created_at")
        );
        w.setCustomerName(rs.getString("customer_name"));
        w.setCustomerPhone(rs.getString("customer_phone"));
        w.setDeviceSummary(rs.getString("device_summary"));
        w.setSerialNumber(rs.getString("serial_number"));
        w.setRepairProblem(rs.getString("initial_problem"));

        // If status in memory is ACTIVE but date is expired, reflect EXPIRED
        if (w.getStatus() == WarrantyStatus.ACTIVE && !w.isCurrentlyValid()) {
            w.setStatus(WarrantyStatus.EXPIRED);
        }
        return w;
    }
}

