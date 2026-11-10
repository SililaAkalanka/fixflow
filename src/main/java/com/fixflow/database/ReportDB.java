package com.fixflow.database;

import com.fixflow.enums.RepairStatus;
import com.fixflow.model.DashboardMetrics;
import com.fixflow.model.Payment;
import com.fixflow.model.RepairJob;
import com.fixflow.model.SparePart;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data access operations for the operational dashboard and in-application reporting views.
 * Implements Section 7.8 (Dashboard) and Section 7.9 (Reports) of the FixFlow Project Specification.
 */
public class ReportDB {

    private static final Logger LOGGER = Logger.getLogger(ReportDB.class.getName());

    /**
     * Loads aggregated real-time metrics for the operational dashboard.
     * Supports role-specific scoping (e.g. Technician assigned workload).
     *
     * @param technicianId optional technician ID filter (or null for overall shop metrics)
     * @return populated {@link DashboardMetrics}
     */
    public static DashboardMetrics getDashboardMetrics(Integer technicianId) {
        DashboardMetrics metrics = new DashboardMetrics();
        String today = LocalDate.now().toString();

        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Repair Job Counts by Status
            String statusSql = "SELECT current_status, COUNT(*) AS cnt FROM repair_jobs GROUP BY current_status";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(statusSql)) {
                int total = 0;
                int active = 0;
                while (rs.next()) {
                    String sStr = rs.getString("current_status");
                    int count = rs.getInt("cnt");
                    total += count;
                    try {
                        RepairStatus status = RepairStatus.valueOf(sStr);
                        metrics.setStatusCount(status, count);
                        if (status == RepairStatus.RECEIVED || status == RepairStatus.DIAGNOSING ||
                            status == RepairStatus.WAITING_FOR_APPROVAL || status == RepairStatus.WAITING_FOR_PARTS ||
                            status == RepairStatus.REPAIRING) {
                            active += count;
                        } else if (status == RepairStatus.READY_FOR_COLLECTION) {
                            metrics.setReadyForCollectionJobs(count);
                        } else if (status == RepairStatus.COLLECTED) {
                            metrics.setCollectedJobs(count);
                        } else if (status == RepairStatus.CANCELLED) {
                            metrics.setCancelledJobs(count);
                        }
                        if (status == RepairStatus.WAITING_FOR_PARTS) {
                            metrics.setWaitingPartsJobs(count);
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
                metrics.setTotalJobs(total);
                metrics.setActiveJobs(active);
            }

            // 2. Today's Intakes
            String todaySql = "SELECT COUNT(*) FROM repair_jobs WHERE date(date_received) = date(?) OR date(created_at) = date(?)";
            try (PreparedStatement ps = conn.prepareStatement(todaySql)) {
                ps.setString(1, today);
                ps.setString(2, today);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        metrics.setTodayJobs(rs.getInt(1));
                    }
                }
            }

            // 3. Technician-Specific Metrics (if assigned)
            if (technicianId != null) {
                String techActiveSql = "SELECT COUNT(*) FROM repair_jobs WHERE technician_id = ? " +
                                       "AND current_status IN ('DIAGNOSING', 'WAITING_FOR_APPROVAL', 'WAITING_FOR_PARTS', 'REPAIRING')";
                try (PreparedStatement ps = conn.prepareStatement(techActiveSql)) {
                    ps.setInt(1, technicianId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) metrics.setTechAssignedActive(rs.getInt(1));
                    }
                }

                String techDoneSql = "SELECT COUNT(*) FROM repair_jobs WHERE technician_id = ? " +
                                     "AND current_status IN ('READY_FOR_COLLECTION', 'COLLECTED') AND date(updated_at) = date(?)";
                try (PreparedStatement ps = conn.prepareStatement(techDoneSql)) {
                    ps.setInt(1, technicianId);
                    ps.setString(2, today);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) metrics.setTechAssignedCompletedToday(rs.getInt(1));
                    }
                }
            }

            // 4. Revenue Metrics (Settled Cash Payments)
            String revSql = "SELECT COALESCE(SUM(amount_paid), 0.0) FROM payments WHERE payment_status = 'PAID'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(revSql)) {
                if (rs.next()) metrics.setTotalRevenue(rs.getDouble(1));
            }

            String todayRevSql = "SELECT COALESCE(SUM(amount_paid), 0.0) FROM payments WHERE payment_status = 'PAID' AND date(payment_date) = date(?)";
            try (PreparedStatement ps = conn.prepareStatement(todayRevSql)) {
                ps.setString(1, today);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) metrics.setTodayRevenue(rs.getDouble(1));
                }
            }

            // 5. Pending Receivables (Ready for Collection jobs not yet settled)
            String pendingSql = "SELECT r.job_id, r.labour_charges, " +
                                "       COALESCE((SELECT SUM(u.quantity * u.selling_price_at_time) FROM repair_part_usage u WHERE u.job_id = r.job_id), 0.0) AS parts_cost " +
                                "FROM repair_jobs r " +
                                "WHERE r.current_status = 'READY_FOR_COLLECTION' " +
                                "  AND r.job_id NOT IN (SELECT job_id FROM payments WHERE payment_status = 'PAID')";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(pendingSql)) {
                double pending = 0.0;
                while (rs.next()) {
                    pending += rs.getDouble("labour_charges") + rs.getDouble("parts_cost");
                }
                metrics.setPendingRevenue(pending);
            }

            // 6. Inventory Stock & Alerts
            String invAlertSql = "SELECT COUNT(CASE WHEN available_quantity <= min_stock_level THEN 1 END) AS low_stock, " +
                                 "       COUNT(CASE WHEN available_quantity = 0 THEN 1 END) AS out_of_stock, " +
                                 "       COALESCE(SUM(available_quantity * purchase_cost), 0.0) AS val " +
                                 "FROM spare_parts";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(invAlertSql)) {
                if (rs.next()) {
                    metrics.setLowStockCount(rs.getInt("low_stock"));
                    metrics.setOutOfStockCount(rs.getInt("out_of_stock"));
                    metrics.setTotalInventoryValuation(rs.getDouble("val"));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load dashboard metrics", e);
        }

        return metrics;
    }

    /**
     * Generates a filtered repair jobs report.
     *
     * @param dateFrom optional start date (YYYY-MM-DD)
     * @param dateTo optional end date (YYYY-MM-DD)
     * @param status optional status filter
     * @param technicianId optional technician ID filter
     * @return list of matching repair jobs with cost calculations
     * @throws SQLException if a database error occurs
     */
    public static List<RepairJob> getRepairJobsReport(String dateFrom, String dateTo, RepairStatus status, Integer technicianId)
            throws SQLException {

        StringBuilder sql = new StringBuilder(
                "SELECT r.job_id, r.customer_id, r.device_id, r.technician_id, r.date_received, r.initial_problem, " +
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
                "WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (dateFrom != null && !dateFrom.trim().isEmpty()) {
            sql.append(" AND date(r.date_received) >= date(?) ");
            params.add(dateFrom.trim());
        }
        if (dateTo != null && !dateTo.trim().isEmpty()) {
            sql.append(" AND date(r.date_received) <= date(?) ");
            params.add(dateTo.trim());
        }
        if (status != null) {
            sql.append(" AND r.current_status = ? ");
            params.add(status.name());
        }
        if (technicianId != null) {
            sql.append(" AND r.technician_id = ? ");
            params.add(technicianId);
        }

        sql.append(" ORDER BY r.job_id DESC");

        List<RepairJob> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(RepairDB.mapResultSetToRepairJob(rs));
                }
            }
        }
        return list;
    }

    /**
     * Generates a filtered revenue & payments report.
     *
     * @param dateFrom optional start date (YYYY-MM-DD)
     * @param dateTo optional end date (YYYY-MM-DD)
     * @return list of matching payment records
     * @throws SQLException if a database error occurs
     */
    public static List<Payment> getRevenueReport(String dateFrom, String dateTo) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT p.payment_id, p.job_id, p.payment_date, p.total_repair_cost, p.amount_paid, " +
                "       p.payment_method, p.payment_status, p.created_at, " +
                "       c.full_name AS customer_name, c.phone AS customer_phone, " +
                "       (d.brand || ' ' || d.model) AS device_summary, " +
                "       t.name AS technician_name, r.labour_charges " +
                "FROM payments p " +
                "JOIN repair_jobs r ON p.job_id = r.job_id " +
                "JOIN customers c ON r.customer_id = c.customer_id " +
                "JOIN devices d ON r.device_id = d.device_id " +
                "LEFT JOIN technicians t ON r.technician_id = t.technician_id " +
                "WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (dateFrom != null && !dateFrom.trim().isEmpty()) {
            sql.append(" AND date(p.payment_date) >= date(?) ");
            params.add(dateFrom.trim());
        }
        if (dateTo != null && !dateTo.trim().isEmpty()) {
            sql.append(" AND date(p.payment_date) <= date(?) ");
            params.add(dateTo.trim());
        }

        sql.append(" ORDER BY p.payment_date DESC, p.payment_id DESC");

        List<Payment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
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
                    list.add(p);
                }
            }
        }
        return list;
    }

    /**
     * Generates a filtered inventory and valuation report.
     *
     * @param category optional category filter (or null for all)
     * @param stockFilter optional stock status filter ("ALL", "LOW_STOCK", "OUT_OF_STOCK", "IN_STOCK")
     * @return list of spare parts
     * @throws SQLException if a database error occurs
     */
    public static List<SparePart> getInventoryReport(String category, String stockFilter) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT part_id, part_name, category, compatible_device_model, purchase_cost, selling_price, " +
                "       available_quantity, min_stock_level, created_at, updated_at " +
                "FROM spare_parts WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category)) {
            sql.append(" AND LOWER(category) = LOWER(?) ");
            params.add(category.trim());
        }

        if ("LOW_STOCK".equalsIgnoreCase(stockFilter)) {
            sql.append(" AND available_quantity <= min_stock_level AND available_quantity > 0 ");
        } else if ("OUT_OF_STOCK".equalsIgnoreCase(stockFilter)) {
            sql.append(" AND available_quantity = 0 ");
        } else if ("IN_STOCK".equalsIgnoreCase(stockFilter)) {
            sql.append(" AND available_quantity > min_stock_level ");
        }

        sql.append(" ORDER BY category ASC, part_name ASC");

        List<SparePart> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SparePart(
                            rs.getInt("part_id"),
                            rs.getString("part_name"),
                            rs.getString("category"),
                            rs.getString("compatible_device_model"),
                            rs.getDouble("purchase_cost"),
                            rs.getDouble("selling_price"),
                            rs.getInt("available_quantity"),
                            rs.getInt("min_stock_level"),
                            rs.getString("created_at"),
                            rs.getString("updated_at")
                    ));
                }
            }
        }
        return list;
    }
}

