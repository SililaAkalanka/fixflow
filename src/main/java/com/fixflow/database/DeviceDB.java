package com.fixflow.database;

import com.fixflow.model.Device;

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

/**
 * Data access operations for electronic device records and basic warranty support.
 */
public class DeviceDB {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Registers a new electronic device associated with a customer.
     *
     * @param device device model
     * @return generated device_id
     * @throws SQLException if a database error occurs
     */
    public static int createDevice(Device device) throws SQLException {
        String sql = "INSERT INTO devices (customer_id, category, brand, model, serial_number, colour, accessories, " +
                     "warranty_months, warranty_start_date, warranty_expiry_date, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String now = LocalDateTime.now().format(DATE_FORMATTER);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, device.getCustomerId());
            ps.setString(2, device.getCategory().trim());
            ps.setString(3, device.getBrand().trim());
            ps.setString(4, device.getModel().trim());
            ps.setString(5, device.getSerialNumber() != null && !device.getSerialNumber().trim().isEmpty() ? device.getSerialNumber().trim() : null);
            ps.setString(6, device.getColour() != null && !device.getColour().trim().isEmpty() ? device.getColour().trim() : null);
            ps.setString(7, device.getAccessories() != null && !device.getAccessories().trim().isEmpty() ? device.getAccessories().trim() : null);
            ps.setInt(8, device.getWarrantyMonths() != null ? device.getWarrantyMonths() : 0);
            ps.setString(9, device.getWarrantyStartDate());
            ps.setString(10, device.getWarrantyExpiryDate());
            ps.setString(11, now);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    device.setDeviceId(id);
                    device.setCreatedAt(now);
                    return id;
                }
            }
        }
        throw new SQLException("Failed to retrieve generated device ID.");
    }

    /**
     * Updates an existing device's details and warranty info.
     *
     * @param device device with updated fields
     * @return true if updated, false if not found
     * @throws SQLException if a database error occurs
     */
    public static boolean updateDevice(Device device) throws SQLException {
        String sql = "UPDATE devices SET category = ?, brand = ?, model = ?, serial_number = ?, colour = ?, accessories = ?, " +
                     "warranty_months = ?, warranty_start_date = ?, warranty_expiry_date = ? " +
                     "WHERE device_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, device.getCategory().trim());
            ps.setString(2, device.getBrand().trim());
            ps.setString(3, device.getModel().trim());
            ps.setString(4, device.getSerialNumber() != null && !device.getSerialNumber().trim().isEmpty() ? device.getSerialNumber().trim() : null);
            ps.setString(5, device.getColour() != null && !device.getColour().trim().isEmpty() ? device.getColour().trim() : null);
            ps.setString(6, device.getAccessories() != null && !device.getAccessories().trim().isEmpty() ? device.getAccessories().trim() : null);
            ps.setInt(7, device.getWarrantyMonths() != null ? device.getWarrantyMonths() : 0);
            ps.setString(8, device.getWarrantyStartDate());
            ps.setString(9, device.getWarrantyExpiryDate());
            ps.setInt(10, device.getDeviceId());

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Updates the warranty information for a device.
     */
    public static boolean updateDeviceWarranty(int deviceId, int warrantyMonths, String startDate, String expiryDate) throws SQLException {
        String sql = "UPDATE devices SET warranty_months = ?, warranty_start_date = ?, warranty_expiry_date = ? WHERE device_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, warrantyMonths);
            ps.setString(2, startDate);
            ps.setString(3, expiryDate);
            ps.setInt(4, deviceId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Checks if a device is currently under active warranty.
     *
     * @param deviceId device ID
     * @return true if valid warranty exists, false otherwise
     * @throws SQLException if database error occurs
     */
    public static boolean isDeviceUnderWarranty(int deviceId) throws SQLException {
        Device d = getDeviceById(deviceId);
        return d != null && d.isUnderWarranty();
    }

    /**
     * Deletes a device record.
     * Blocked if repair records exist for this device.
     *
     * @param deviceId ID of the device to delete
     * @return true if deleted
     * @throws SQLException if deletion is blocked due to active repairs or foreign keys
     */
    public static boolean deleteDevice(int deviceId) throws SQLException {
        if (hasRepairHistory(deviceId)) {
            throw new SQLException("Cannot delete Device #" + deviceId + ": Historical repair records are associated with this device. " +
                    "Operational history must be preserved.");
        }

        String sql = "DELETE FROM devices WHERE device_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deviceId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves a device by its ID.
     */
    public static Device getDeviceById(int deviceId) throws SQLException {
        String sql = "SELECT d.device_id, d.customer_id, d.category, d.brand, d.model, d.serial_number, " +
                     "d.colour, d.accessories, d.warranty_months, d.warranty_start_date, d.warranty_expiry_date, d.created_at, " +
                     "c.full_name AS customer_name, c.phone AS customer_phone " +
                     "FROM devices d " +
                     "LEFT JOIN customers c ON d.customer_id = c.customer_id " +
                     "WHERE d.device_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deviceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDevice(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all devices registered in the system.
     */
    public static List<Device> getAllDevices() throws SQLException {
        List<Device> list = new ArrayList<>();
        String sql = "SELECT d.device_id, d.customer_id, d.category, d.brand, d.model, d.serial_number, " +
                     "d.colour, d.accessories, d.warranty_months, d.warranty_start_date, d.warranty_expiry_date, d.created_at, " +
                     "c.full_name AS customer_name, c.phone AS customer_phone " +
                     "FROM devices d " +
                     "LEFT JOIN customers c ON d.customer_id = c.customer_id " +
                     "ORDER BY d.device_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToDevice(rs));
            }
        }
        return list;
    }

    /**
     * Retrieves all devices belonging to a specific customer.
     */
    public static List<Device> getDevicesByCustomerId(int customerId) throws SQLException {
        List<Device> list = new ArrayList<>();
        String sql = "SELECT d.device_id, d.customer_id, d.category, d.brand, d.model, d.serial_number, " +
                     "d.colour, d.accessories, d.warranty_months, d.warranty_start_date, d.warranty_expiry_date, d.created_at, " +
                     "c.full_name AS customer_name, c.phone AS customer_phone " +
                     "FROM devices d " +
                     "LEFT JOIN customers c ON d.customer_id = c.customer_id " +
                     "WHERE d.customer_id = ? " +
                     "ORDER BY d.device_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDevice(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves the primary or first device registered for a customer.
     */
    public static Device getDeviceByCustomerId(int customerId) throws SQLException {
        List<Device> devices = getDevicesByCustomerId(customerId);
        return devices.isEmpty() ? null : devices.get(0);
    }

    /**
     * Searches devices by keyword across brand, model, serial number, and customer name.
     */
    public static List<Device> searchDevices(String query, String categoryFilter) throws SQLException {
        List<Device> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT d.device_id, d.customer_id, d.category, d.brand, d.model, d.serial_number, " +
                "d.colour, d.accessories, d.warranty_months, d.warranty_start_date, d.warranty_expiry_date, d.created_at, " +
                "c.full_name AS customer_name, c.phone AS customer_phone " +
                "FROM devices d " +
                "LEFT JOIN customers c ON d.customer_id = c.customer_id WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (d.brand LIKE ? OR d.model LIKE ? OR d.serial_number LIKE ? OR c.full_name LIKE ?) ");
            String term = "%" + query.trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (categoryFilter != null && !categoryFilter.trim().isEmpty() && !"All Categories".equalsIgnoreCase(categoryFilter)) {
            sql.append("AND LOWER(d.category) = LOWER(?) ");
            params.add(categoryFilter.trim());
        }

        sql.append("ORDER BY d.device_id DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDevice(rs));
                }
            }
        }
        return list;
    }

    /**
     * Checks if any repair jobs are associated with this device.
     */
    public static boolean hasRepairHistory(int deviceId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM repair_jobs WHERE device_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deviceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private static Device mapResultSetToDevice(ResultSet rs) throws SQLException {
        Device d = new Device(
                rs.getInt("device_id"),
                rs.getInt("customer_id"),
                rs.getString("category"),
                rs.getString("brand"),
                rs.getString("model"),
                rs.getString("serial_number"),
                rs.getString("colour"),
                rs.getString("accessories"),
                rs.getInt("warranty_months"),
                rs.getString("warranty_start_date"),
                rs.getString("warranty_expiry_date"),
                rs.getString("created_at")
        );
        d.setCustomerName(rs.getString("customer_name"));
        d.setCustomerPhone(rs.getString("customer_phone"));
        return d;
    }
}
