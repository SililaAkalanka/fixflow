package com.fixflow.database;

import com.fixflow.model.Payment;

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
 * Pure Data Access Object (DAO) for payment records.
 * All business validations and balance calculations are delegated to PaymentService.
 */
public class PaymentDB {

    private static final Logger LOGGER = Logger.getLogger(PaymentDB.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private PaymentDB() {
        // Data access utility
    }

    /**
     * Inserts a payment record into the payments table.
     *
     * @param payment payment domain model
     * @return generated payment ID
     * @throws SQLException if a database error occurs
     */
    public static int insertPayment(Payment payment) throws SQLException {
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        String pDate = (payment.getPaymentDate() != null && !payment.getPaymentDate().trim().isEmpty())
                ? payment.getPaymentDate().trim() : now;

        String sql = "INSERT INTO payments (job_id, payment_date, amount_paid, payment_method, notes, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, payment.getJobId());
            ps.setString(2, pDate);
            ps.setDouble(3, payment.getAmountPaid());
            ps.setString(4, payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "CASH");
            ps.setString(5, payment.getNotes());
            ps.setString(6, now);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    payment.setPaymentId(id);
                    payment.setPaymentDate(pDate);
                    payment.setCreatedAt(now);
                    return id;
                }
            }
        }
        throw new SQLException("Failed to retrieve generated payment ID.");
    }

    /**
     * Retrieves the sum of all payments recorded for a given repair job.
     *
     * @param jobId repair job ID
     * @return total amount paid so far
     * @throws SQLException if a database error occurs
     */
    public static double getTotalPaidForJob(int jobId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount_paid), 0.0) FROM payments WHERE job_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    /**
     * Retrieves all payment transactions for a given repair job.
     *
     * @param jobId repair job ID
     * @return list of payments ordered by payment_id ASC
     * @throws SQLException if a database error occurs
     */
    public static List<Payment> getPaymentsByJobId(int jobId) throws SQLException {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT p.payment_id, p.job_id, p.payment_date, p.amount_paid, p.payment_method, p.notes, p.created_at, " +
                     "c.full_name AS customer_name, c.phone AS customer_phone, " +
                     "d.brand || ' ' || d.model AS device_summary, " +
                     "t.name AS technician_name, j.labour_charges " +
                     "FROM payments p " +
                     "JOIN repair_jobs j ON p.job_id = j.job_id " +
                     "JOIN customers c ON j.customer_id = c.customer_id " +
                     "JOIN devices d ON j.device_id = d.device_id " +
                     "LEFT JOIN technicians t ON j.technician_id = t.technician_id " +
                     "WHERE p.job_id = ? " +
                     "ORDER BY p.payment_id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPayment(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves all payment transactions across the system with joined customer and device info.
     *
     * @return list of payments ordered by newest first
     * @throws SQLException if a database error occurs
     */
    public static List<Payment> getAllPayments() throws SQLException {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT p.payment_id, p.job_id, p.payment_date, p.amount_paid, p.payment_method, p.notes, p.created_at, " +
                     "c.full_name AS customer_name, c.phone AS customer_phone, " +
                     "d.brand || ' ' || d.model AS device_summary, " +
                     "t.name AS technician_name, j.labour_charges " +
                     "FROM payments p " +
                     "JOIN repair_jobs j ON p.job_id = j.job_id " +
                     "JOIN customers c ON j.customer_id = c.customer_id " +
                     "JOIN devices d ON j.device_id = d.device_id " +
                     "LEFT JOIN technicians t ON j.technician_id = t.technician_id " +
                     "ORDER BY p.payment_id DESC";

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
     * Retrieves a payment by its ID.
     *
     * @param paymentId payment ID
     * @return Payment or null if not found
     * @throws SQLException if a database error occurs
     */
    public static Payment getPaymentById(int paymentId) throws SQLException {
        String sql = "SELECT p.payment_id, p.job_id, p.payment_date, p.amount_paid, p.payment_method, p.notes, p.created_at, " +
                     "c.full_name AS customer_name, c.phone AS customer_phone, " +
                     "d.brand || ' ' || d.model AS device_summary, " +
                     "t.name AS technician_name, j.labour_charges " +
                     "FROM payments p " +
                     "JOIN repair_jobs j ON p.job_id = j.job_id " +
                     "JOIN customers c ON j.customer_id = c.customer_id " +
                     "JOIN devices d ON j.device_id = d.device_id " +
                     "LEFT JOIN technicians t ON j.technician_id = t.technician_id " +
                     "WHERE p.payment_id = ?";

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

    private static Payment mapResultSetToPayment(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getInt("payment_id"));
        p.setJobId(rs.getInt("job_id"));
        p.setPaymentDate(rs.getString("payment_date"));
        p.setAmountPaid(rs.getDouble("amount_paid"));
        p.setPaymentMethod(rs.getString("payment_method"));
        p.setNotes(rs.getString("notes"));
        p.setCreatedAt(rs.getString("created_at"));

        p.setCustomerName(rs.getString("customer_name"));
        p.setCustomerPhone(rs.getString("customer_phone"));
        p.setDeviceSummary(rs.getString("device_summary"));
        p.setTechnicianName(rs.getString("technician_name"));
        p.setLabourCharges(rs.getDouble("labour_charges"));

        return p;
    }
}
