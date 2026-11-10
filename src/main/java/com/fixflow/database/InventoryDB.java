package com.fixflow.database;

import com.fixflow.exception.InsufficientStockException;
import com.fixflow.model.RepairPartUsage;
import com.fixflow.model.SparePart;

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
 * Data access operations for spare-part inventory, stock adjustments, and transactional part usage.
 * Implements Section 7.5, Section 7.6, and Section 8.17 of the FixFlow Project Specification.
 */
public class InventoryDB {

    private static final Logger LOGGER = Logger.getLogger(InventoryDB.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Registers a new spare part in the inventory catalog.
     *
     * @param part spare part model
     * @return generated part_id
     * @throws SQLException if a database error occurs
     */
    public static int createPart(SparePart part) throws SQLException {
        String sql = "INSERT INTO spare_parts (part_name, category, compatible_device_model, purchase_cost, " +
                     "selling_price, available_quantity, min_stock_level, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String now = LocalDateTime.now().format(DATE_FORMATTER);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, part.getPartName().trim());
            ps.setString(2, part.getCategory().trim());
            ps.setString(3, part.getCompatibleDeviceModel().trim());
            ps.setDouble(4, part.getPurchaseCost());
            ps.setDouble(5, part.getSellingPrice());
            ps.setInt(6, part.getAvailableQuantity());
            ps.setInt(7, part.getMinStockLevel());
            ps.setString(8, now);
            ps.setString(9, now);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    part.setPartId(id);
                    part.setCreatedAt(now);
                    part.setUpdatedAt(now);
                    return id;
                }
            }
        }
        throw new SQLException("Failed to retrieve generated part ID.");
    }

    /**
     * Updates an existing spare part's catalog details.
     *
     * @param part spare part with updated values
     * @return true if updated, false if not found
     * @throws SQLException if a database error occurs
     */
    public static boolean updatePart(SparePart part) throws SQLException {
        String sql = "UPDATE spare_parts SET part_name = ?, category = ?, compatible_device_model = ?, " +
                     "purchase_cost = ?, selling_price = ?, min_stock_level = ?, updated_at = ? " +
                     "WHERE part_id = ?";
        String now = LocalDateTime.now().format(DATE_FORMATTER);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, part.getPartName().trim());
            ps.setString(2, part.getCategory().trim());
            ps.setString(3, part.getCompatibleDeviceModel().trim());
            ps.setDouble(4, part.getPurchaseCost());
            ps.setDouble(5, part.getSellingPrice());
            ps.setInt(6, part.getMinStockLevel());
            ps.setString(7, now);
            ps.setInt(8, part.getPartId());

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Records a manual stock addition.
     *
     * @param partId spare part ID
     * @param additionalQuantity quantity to add (must be > 0)
     * @return true if updated
     * @throws SQLException if a database error occurs
     */
    public static boolean addStock(int partId, int additionalQuantity) throws SQLException {
        if (additionalQuantity <= 0) {
            throw new IllegalArgumentException("Stock quantity addition must be greater than zero.");
        }

        String sql = "UPDATE spare_parts SET available_quantity = available_quantity + ?, updated_at = ? WHERE part_id = ?";
        String now = LocalDateTime.now().format(DATE_FORMATTER);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, additionalQuantity);
            ps.setString(2, now);
            ps.setInt(3, partId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Issues spare parts to a repair job and deducts inventory stock in a database transaction.
     * Enforces:
     * 1. Available quantity check (Section 7.5 & 8.17: stock cannot become negative).
     * 2. Transactional deduction and usage insert (Section 7.5).
     * 3. Price snapshotting (Section 7.6: selling_price_at_time is frozen with usage record).
     *
     * @param jobId repair job ID
     * @param partId spare part ID
     * @param quantity quantity to issue
     * @param issuedByRole role issuing the parts (Technician)
     * @throws SQLException if a database error occurs
     * @throws InsufficientStockException if requested quantity exceeds available stock
     */
    public static void issuePartToJob(int jobId, int partId, int quantity, String issuedByRole)
            throws SQLException, InsufficientStockException {

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity issued must be at least 1.");
        }

        String now = LocalDateTime.now().format(DATE_FORMATTER);

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Check current stock and price
                String selectSql = "SELECT part_name, available_quantity, selling_price FROM spare_parts WHERE part_id = ?";
                String partName;
                int availableQty;
                double currentSellingPrice;

                try (PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
                    psSelect.setInt(1, partId);
                    try (ResultSet rs = psSelect.executeQuery()) {
                        if (rs.next()) {
                            partName = rs.getString("part_name");
                            availableQty = rs.getInt("available_quantity");
                            currentSellingPrice = rs.getDouble("selling_price");
                        } else {
                            throw new SQLException("Part #" + partId + " not found in inventory.");
                        }
                    }
                }

                // Verify stock availability
                if (availableQty < quantity) {
                    throw new InsufficientStockException(partId, partName, availableQty, quantity);
                }

                // 2. Deduct inventory stock
                String deductSql = "UPDATE spare_parts SET available_quantity = available_quantity - ?, updated_at = ? WHERE part_id = ?";
                try (PreparedStatement psDeduct = conn.prepareStatement(deductSql)) {
                    psDeduct.setInt(1, quantity);
                    psDeduct.setString(2, now);
                    psDeduct.setInt(3, partId);
                    psDeduct.executeUpdate();
                }

                // 3. Insert usage record with frozen selling_price_at_time snapshot
                String usageSql = "INSERT INTO repair_part_usage (job_id, part_id, quantity, selling_price_at_time, date_used) " +
                                  "VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement psUsage = conn.prepareStatement(usageSql)) {
                    psUsage.setInt(1, jobId);
                    psUsage.setInt(2, partId);
                    psUsage.setInt(3, quantity);
                    psUsage.setDouble(4, currentSellingPrice); // Frozen price snapshot (§7.6)
                    psUsage.setString(5, now);
                    psUsage.executeUpdate();
                }

                // 4. Log part usage in repair status history
                String histSql = "INSERT INTO repair_status_history (job_id, from_status, to_status, changed_by_role, change_date, notes) " +
                                 "SELECT job_id, current_status, current_status, ?, ?, ? FROM repair_jobs WHERE job_id = ?";
                try (PreparedStatement psHist = conn.prepareStatement(histSql)) {
                    psHist.setString(1, issuedByRole != null ? issuedByRole : "Technician");
                    psHist.setString(2, now);
                    psHist.setString(3, "Issued part: " + quantity + "x " + partName + " @ LKR " + String.format("%.2f", currentSellingPrice));
                    psHist.setInt(4, jobId);
                    psHist.executeUpdate();
                }

                conn.commit();
                LOGGER.info("Issued " + quantity + "x " + partName + " to Job #" + jobId + " (Deducted from stock).");
            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to issue parts to job #" + jobId + ", rolled back.", e);
                if (e instanceof InsufficientStockException) {
                    throw (InsufficientStockException) e;
                }
                throw new SQLException("Failed to issue part to job: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Removes an issued part from a job and restores the inventory quantity in a transaction.
     *
     * @param usageId usage record ID
     * @return true if removed and stock restored
     * @throws SQLException if a database error occurs
     */
    public static boolean removePartUsage(int usageId) throws SQLException {
        String selectSql = "SELECT job_id, part_id, quantity FROM repair_part_usage WHERE usage_id = ?";
        String now = LocalDateTime.now().format(DATE_FORMATTER);

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int partId;
                int qty;
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, usageId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            partId = rs.getInt("part_id");
                            qty = rs.getInt("quantity");
                        } else {
                            return false;
                        }
                    }
                }

                // Restore stock
                try (PreparedStatement psRest = conn.prepareStatement(
                        "UPDATE spare_parts SET available_quantity = available_quantity + ?, updated_at = ? WHERE part_id = ?")) {
                    psRest.setInt(1, qty);
                    psRest.setString(2, now);
                    psRest.setInt(3, partId);
                    psRest.executeUpdate();
                }

                // Delete usage row
                try (PreparedStatement psDel = conn.prepareStatement("DELETE FROM repair_part_usage WHERE usage_id = ?")) {
                    psDel.setInt(1, usageId);
                    psDel.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Retrieves a spare part by its ID.
     *
     * @param partId part ID
     * @return {@link SparePart} object, or null if not found
     * @throws SQLException if a database error occurs
     */
    public static SparePart getPartById(int partId) throws SQLException {
        String sql = "SELECT part_id, part_name, category, compatible_device_model, purchase_cost, selling_price, " +
                     "available_quantity, min_stock_level, created_at, updated_at FROM spare_parts WHERE part_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, partId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPart(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all spare parts ordered by name.
     *
     * @return list of spare parts
     * @throws SQLException if a database error occurs
     */
    public static List<SparePart> getAllParts() throws SQLException {
        List<SparePart> list = new ArrayList<>();
        String sql = "SELECT part_id, part_name, category, compatible_device_model, purchase_cost, selling_price, " +
                     "available_quantity, min_stock_level, created_at, updated_at FROM spare_parts ORDER BY part_name ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToPart(rs));
            }
        }
        return list;
    }

    /**
     * Retrieves all parts currently at or below their configured minimum stock level.
     * Implements Section 7.5 and Section 7.8 (Low-stock alerts).
     *
     * @return list of low-stock parts
     * @throws SQLException if a database error occurs
     */
    public static List<SparePart> getLowStockParts() throws SQLException {
        List<SparePart> list = new ArrayList<>();
        String sql = "SELECT part_id, part_name, category, compatible_device_model, purchase_cost, selling_price, " +
                     "available_quantity, min_stock_level, created_at, updated_at " +
                     "FROM spare_parts WHERE available_quantity <= min_stock_level ORDER BY available_quantity ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToPart(rs));
            }
        }
        return list;
    }

    /**
     * Retrieves all spare parts issued to a specific repair job.
     *
     * @param jobId repair job ID
     * @return list of part usages with joined part names and frozen prices
     * @throws SQLException if a database error occurs
     */
    public static List<RepairPartUsage> getPartsUsedForJob(int jobId) throws SQLException {
        List<RepairPartUsage> list = new ArrayList<>();
        String sql = "SELECT u.usage_id, u.job_id, u.part_id, u.quantity, u.selling_price_at_time, u.date_used, " +
                     "p.part_name, p.category AS part_category, p.compatible_device_model " +
                     "FROM repair_part_usage u " +
                     "JOIN spare_parts p ON u.part_id = p.part_id " +
                     "WHERE u.job_id = ? ORDER BY u.usage_id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jobId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RepairPartUsage usage = new RepairPartUsage(
                            rs.getInt("usage_id"),
                            rs.getInt("job_id"),
                            rs.getInt("part_id"),
                            rs.getInt("quantity"),
                            rs.getDouble("selling_price_at_time"),
                            rs.getString("date_used")
                    );
                    usage.setPartName(rs.getString("part_name"));
                    usage.setPartCategory(rs.getString("part_category"));
                    usage.setCompatibleDeviceModel(rs.getString("compatible_device_model"));
                    list.add(usage);
                }
            }
        }
        return list;
    }

    /**
     * Calculates the total spare-part selling cost for a repair job.
     * Formula: sum(quantity * selling_price_at_time)
     *
     * @param jobId repair job ID
     * @return total parts cost
     * @throws SQLException if a database error occurs
     */
    public static double getTotalPartsCostForJob(int jobId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantity * selling_price_at_time), 0.0) FROM repair_part_usage WHERE job_id = ?";
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
     * Searches spare parts with optional category and low-stock filters.
     *
     * @param query search query
     * @param category category filter or "All Categories"
     * @param lowStockOnly if true, only includes parts where quantity <= min_stock_level
     * @return matching spare parts
     * @throws SQLException if a database error occurs
     */
    public static List<SparePart> searchParts(String query, String category, boolean lowStockOnly) throws SQLException {
        List<SparePart> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT part_id, part_name, category, compatible_device_model, purchase_cost, selling_price, " +
                "available_quantity, min_stock_level, created_at, updated_at FROM spare_parts WHERE 1=1 "
        );

        boolean hasQuery = query != null && !query.trim().isEmpty();
        boolean hasCategory = category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("All Categories") && !category.equalsIgnoreCase("All");

        if (hasQuery) {
            sql.append("AND (LOWER(part_name) LIKE ? OR LOWER(compatible_device_model) LIKE ? OR CAST(part_id AS TEXT) = ?) ");
        }

        if (hasCategory) {
            sql.append("AND LOWER(category) = LOWER(?) ");
        }

        if (lowStockOnly) {
            sql.append("AND available_quantity <= min_stock_level ");
        }

        sql.append("ORDER BY part_name ASC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int pIdx = 1;

            if (hasQuery) {
                String wildcard = "%" + query.trim().toLowerCase() + "%";
                ps.setString(pIdx++, wildcard);
                ps.setString(pIdx++, wildcard);
                ps.setString(pIdx++, query.trim());
            }

            if (hasCategory) {
                ps.setString(pIdx++, category.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPart(rs));
                }
            }
        }
        return list;
    }

    /**
     * Deletes a spare part. Blocked if the part has historical usage records in repair jobs.
     *
     * @param partId part ID
     * @return true if deleted
     * @throws SQLException if deletion is blocked
     */
    public static boolean deletePart(int partId) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM repair_part_usage WHERE part_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, partId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new SQLException("Cannot delete Part #" + partId + ": It has been recorded as used in past repair jobs. Historical records must be preserved.");
                }
            }
        }

        String sql = "DELETE FROM spare_parts WHERE part_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, partId);
            return ps.executeUpdate() > 0;
        }
    }

    private static SparePart mapResultSetToPart(ResultSet rs) throws SQLException {
        return new SparePart(
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
        );
    }
}

