package com.fixflow.database;

import com.fixflow.enums.RepairStatus;
import com.fixflow.model.Payment;
import com.fixflow.model.RepairJob;

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
 * Data access operations for payments and billing.
 * Implements Section 7.4, Section 8.11, Section 8.12, and Section 8.13 of the FixFlow Project Specification.
 */
public class PaymentDB {

    private static final Logger LOGGER = Logger.getLogger(PaymentDB.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Calculates the exact total repair cost for a job.
     * Formula: Total = Labour Charges + Spare-part Selling Costs (with frozen price snapshots).
     *
     * @param jobId repair job ID
     * @return calculated total repair cost
     * @throws SQLException if a database error occurs
     */
    public static double calculateJobTotalCost(int jobId) throws SQLException {
        RepairJob job = RepairDB.getRepairJobById(jobId);
        if (job == null) {
            throw new SQLException("Repair Job #" + jobId + " not found.");
        }
        double labour = job.getLabourCharges();
        double partsCost = InventoryDB.getTotalPartsCostForJob(jobId);
        return labour + partsCost;
    }

    /**
     * Records a full cash payment for a completed repair job.
     * Business rules:
     * - Section 8.12: Payment is full cash payment only, after repair completion.
     * - Section 7.4: Partial payments are not supported.
     * - Prevents duplicate payment records for the same job.
     *
     * @param jobId repair job ID
     * @param amountPaid cash amount paid (must be >= calculated total cost)
     * @param paymentDate payment timestamp (defaults to current time if null)
     * @param recordedByRole role recording the payment
     * @return generated payment ID
     * @throws SQLException if database error occurs
     * @throws IllegalArgumentException if job is not completed, already paid, or amount is less than total
     */
    public static int recordCashPayment(int jobId, double amountPaid, String paymentDate, String recordedByRole)
            throws SQLException {

        RepairJob job = RepairDB.getRepairJobById(jobId);
        if (job == null) {
            throw new SQLException("Repair Job #" + jobId + " not found.");
        }

        // Must be in a completed stage: READY_FOR_COLLECTION or COLLECTED
        if (job.getCurrentStatus() != RepairStatus.READY_FOR_COLLECTION && job.getCurrentStatus() != RepairStatus.COLLECTED) {
            throw new IllegalArgumentException("Payment can only be recorded after repair completion (Current status: "
                    + job.getCurrentStatus().getDisplayName() + ").");
        }

        if (isJobFullyPaid(jobId)) {
            throw new IllegalStateException("Payment has already been recorded for Repair Job #" + jobId + ".");
        }

        double totalCost = calculateJobTotalCost(jobId);

        // Section 7.4 & 8.12: Full cash payment only. No partial payments.
        if (amountPaid < totalCost - 0.001) {
            throw new IllegalArgumentException(String.format(
                    "Partial payments are not supported. Full amount of $%.2f is required (Attempted: $%.2f).",
                    totalCost, amountPaid));
        }

        String now = LocalDateTime.now().format(DATE_FORMATTER);
        String finalPaymentDate = (paymentDate != null && !paymentDate.trim().isEmpty()) ? paymentDate.trim() : now;

        String insertSql = "INSERT INTO payments (job_id, payment_date, total_repair_cost, amount_paid, payment_method, payment_status, created_at) " +
                           "VALUES (?, ?, ?, ?, 'CASH', 'PAID', ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int paymentId;
                try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, jobId);
                    ps.setString(2, finalPaymentDate);
                    ps.setDouble(3, totalCost);
                    ps.setDouble(4, totalCost); // amount_paid recorded as settled total
                    ps.setString(5, now);
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            paymentId = rs.getInt(1);
                        } else {
                            throw new SQLException("Failed to retrieve generated payment ID.");
                        }
                    }
                }

                // Audit in repair_status_history
                String histSql = "INSERT INTO repair_status_history (job_id, from_status, to_status, changed_by_role, change_date, notes) " +
                                 "SELECT job_id, current_status, current_status, ?, ?, ? FROM repair_jobs WHERE job_id = ?";
                try (PreparedStatement psHist = conn.prepareStatement(histSql)) {
                    psHist.setString(1, recordedByRole != null ? recordedByRole : "Receptionist");
                    psHist.setString(2, now);
                    psHist.setString(3, String.format("Payment recorded: $%.2f (CASH settled in full). Payment #%d.", totalCost, paymentId));
                    psHist.setInt(4, jobId);
                    psHist.executeUpdate();
                }

                conn.commit();
                LOGGER.info(String.format("Payment #%d successfully recorded for Job #%d: $%.2f CASH.", paymentId, jobId, totalCost));
                return paymentId;
            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to record payment for Job #" + jobId + ", rolled back.", e);
                if (e instanceof SQLException) throw (SQLException) e;
                throw new SQLException("Error recording payment: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Checks if a repair job has a settled payment record.
     * Enforces Section 8.13: A device cannot be collected until full payment is recorded.
     *
     * @param jobId repair job ID
     * @return true if paid, false otherwise
     * @throws SQLException if a database error occurs
     */
    public static boolean isJobFullyPaid(int jobId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM payments WHERE job_id = ? AND payment_status = 'PAID'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Retrieves payment details by payment ID with joined customer, device, and job data.
     *
     * @param paymentId payment ID
     * @return {@link Payment} record or null if not found
     * @throws SQLException if a database error occurs
     */
    public static Payment getPaymentById(int paymentId) throws SQLException {
        String sql = getPaymentSelectQuery() + " WHERE p.payment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPayment(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves payment details for a specific repair job.
     *
     * @param jobId repair job ID
     * @return {@link Payment} record or null if not found
     * @throws SQLException if a database error occurs
     */
    public static Payment getPaymentByJobId(int jobId) throws SQLException {
        String sql = getPaymentSelectQuery() + " WHERE p.job_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPayment(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all recorded payments ordered chronologically.
     *
     * @return list of payments
     * @throws SQLException if a database error occurs
     */
    public static List<Payment> getAllPayments() throws SQLException {
        List<Payment> list = new ArrayList<>();
        String sql = getPaymentSelectQuery() + " ORDER BY p.payment_date DESC, p.payment_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToPayment(rs));
            }
        }
        return list;
    }

    /**
     * Searches payments by customer name, phone, device summary, or job number.
     *
     * @param query text query (matches name, phone, or model)
     * @return list of matching payments
     * @throws SQLException if a database error occurs
     */
    public static List<Payment> searchPayments(String query) throws SQLException {
        if (query == null || query.trim().isEmpty()) {
            return getAllPayments();
        }

        String pattern = "%" + query.trim().toLowerCase() + "%";
        String sql = getPaymentSelectQuery() +
                     " WHERE LOWER(c.full_name) LIKE ? OR c.phone LIKE ? " +
                     "    OR LOWER(d.brand || ' ' || d.model) LIKE ? " +
                     "    OR CAST(p.job_id AS TEXT) LIKE ? OR CAST(p.payment_id AS TEXT) LIKE ? " +
                     " ORDER BY p.payment_date DESC";

        List<Payment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            ps.setString(5, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPayment(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves completed repair jobs that are currently awaiting payment settlement.
     * Jobs with status READY_FOR_COLLECTION or COLLECTED without a settled payment.
     *
     * @return list of unpaid repair jobs
     * @throws SQLException if a database error occurs
     */
    public static List<RepairJob> getUnpaidCompletedJobs() throws SQLException {
        List<RepairJob> list = new ArrayList<>();
        String sql = "SELECT r.job_id, r.customer_id, r.device_id, r.technician_id, r.date_received, r.initial_problem, " +
                     "       r.current_status, r.diagnosis_findings, r.estimated_cost, r.labour_charges, " +
                     "       r.estimated_completion_date, r.actual_completion_date, r.approval_status, r.approval_date, " +
                     "       r.cancellation_reason, r.original_job_id, r.created_at, r.updated_at, " +
                     "       c.full_name AS customer_name, c.phone AS customer_phone, " +
                     "       (d.brand || ' ' || d.model) AS device_summary, " +
                     "       t.name AS technician_name " +
                     "FROM repair_jobs r " +
                     "JOIN customers c ON r.customer_id = c.customer_id " +
                     "JOIN devices d ON r.device_id = d.device_id " +
                     "LEFT JOIN technicians t ON r.technician_id = t.technician_id " +
                     "WHERE r.current_status IN ('READY_FOR_COLLECTION', 'COLLECTED') " +
                     "  AND r.job_id NOT IN (SELECT job_id FROM payments WHERE payment_status = 'PAID') " +
                     "ORDER BY r.job_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(RepairDB.mapResultSetToRepairJob(rs));
            }
        }
        return list;
    }

    /**
     * Deletes a payment record. Strictly protected: only administrators may invoke.
     * Enforces Section 7.10 and Section 8.19 (financial record protection).
     *
     * @param paymentId payment ID to delete
     * @return true if deleted
     * @throws SQLException if dependent records exist or job is COLLECTED
     */
    public static boolean deletePayment(int paymentId) throws SQLException {
        Payment payment = getPaymentById(paymentId);
        if (payment == null) return false;

        RepairJob job = RepairDB.getRepairJobById(payment.getJobId());
        if (job != null && job.getCurrentStatus() == RepairStatus.COLLECTED) {
            throw new SQLException("Cannot delete payment: Repair Job #" + job.getJobId()
                    + " has already been COLLECTED. Historical financial records are protected (§8.19).");
        }

        String sql = "DELETE FROM payments WHERE payment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, paymentId);
            return ps.executeUpdate() > 0;
        }
    }

    private static String getPaymentSelectQuery() {
        return "SELECT p.payment_id, p.job_id, p.payment_date, p.total_repair_cost, p.amount_paid, " +
               "       p.payment_method, p.payment_status, p.created_at, " +
               "       c.full_name AS customer_name, c.phone AS customer_phone, " +
               "       (d.brand || ' ' || d.model) AS device_summary, " +
               "       t.name AS technician_name, r.labour_charges " +
               "FROM payments p " +
               "JOIN repair_jobs r ON p.job_id = r.job_id " +
               "JOIN customers c ON r.customer_id = c.customer_id " +
               "JOIN devices d ON r.device_id = d.device_id " +
               "LEFT JOIN technicians t ON r.technician_id = t.technician_id";
    }

    private static Payment mapResultSetToPayment(ResultSet rs) throws SQLException {
        Payment p = new Payment(
                rs.getInt("payment_id"),
                rs.getInt("job_id"),
                rs.getString("payment_date"),
                rs.getDouble("total_repair_cost"),
                rs.getDouble("amount_paid"),
                rs.getString("payment_method"),
                rs.getString("payment_status"),
                rs.getString("created_at")
        );
        p.setCustomerName(rs.getString("customer_name"));
        p.setCustomerPhone(rs.getString("customer_phone"));
        p.setDeviceSummary(rs.getString("device_summary"));
        p.setTechnicianName(rs.getString("technician_name"));
        double labour = rs.getDouble("labour_charges");
        p.setLabourCharges(labour);
        p.setPartsCost(Math.max(0.0, p.getTotalRepairCost() - labour));
        return p;
    }
}
