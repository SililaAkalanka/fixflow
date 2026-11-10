package com.fixflow.database;

import com.fixflow.model.Device;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access operations for electronic device records.
 * Implements Section 7.2 of the FixFlow Project Specification.
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
        String sql = "INSERT INTO devices (customer_id, category, brand, model, serial_number, colour, accessories, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
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
            ps.setString(8, now);

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
     * Updates an existing device's details.
     *
     * @param device device with updated fields
     * @return true if updated, false if not found
     * @throws SQLException if a database error occurs
     */
    public static boolean updateDevice(Device device) throws SQLException {
        String sql = "UPDATE devices SET category = ?, brand = ?, model = ?, serial_number = ?, colour = ?, accessories = ? " +
                     "WHERE device_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, device.getCategory().trim());
            ps.setString(2, device.getBrand().trim());
            ps.setString(3, device.getModel().trim());
            ps.setString(4, device.getSerialNumber() != null && !device.getSerialNumber().trim().isEmpty() ? device.getSerialNumber().trim() : null);
            ps.setString(5, device.getColour() != null && !device.getColour().trim().isEmpty() ? device.getColour().trim() : null);
            ps.setString(6, device.getAccessories() != null && !device.getAccessories().trim().isEmpty() ? device.getAccessories().trim() : null);
            ps.setInt(7, device.getDeviceId());

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a device record.
     * Blocked if repair records exist for this device (Section 7.10).
     *
     * @param deviceId ID of the device to delete
     * @return true if deleted
     * @throws SQLException if deletion is blocked due to active repairs or foreign keys
     */
    public static boolean deleteDevice(int deviceId) throws SQLException {
        if (hasRepairHistory(deviceId)) {
            throw new SQLException("Cannot delete Device #" + deviceId + ": Historical repair records are associated with this device. " +
                    "Operational and financial history must be preserved.");
        }

        String sql = "DELETE FROM devices WHERE device_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deviceId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Checks if a device has any associated repair records.
     *
     * @param deviceId device ID
     * @return true if repair records exist, false otherwise
     * @throws SQLException if a database error occurs
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

    /**
     * Retrieves a device by its ID, with customer information joined.
     *
     * @param deviceId device ID
     * @return {@link Device} object, or null if not found
     * @throws SQLException if a database error occurs
     */
    public static Device getDeviceById(int deviceId) throws SQLException {
        String sql = "SELECT d.device_id, d.customer_id, d.category, d.brand, d.model, d.serial_number, d.colour, d.accessories, d.created_at, " +
                     "c.full_name, c.phone " +
                     "FROM devices d " +
                     "JOIN customers c ON d.customer_id = c.customer_id " +
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
     * Retrieves the device associated with a customer ID.
     *
     * @param customerId customer ID
     * @return {@link Device} object, or null if not found
     * @throws SQLException if a database error occurs
     */
    public static Device getDeviceByCustomerId(int customerId) throws SQLException {
        String sql = "SELECT d.device_id, d.customer_id, d.category, d.brand, d.model, d.serial_number, d.colour, d.accessories, d.created_at, " +
                     "c.full_name, c.phone " +
                     "FROM devices d " +
                     "JOIN customers c ON d.customer_id = c.customer_id " +
                     "WHERE d.customer_id = ? " +
                     "ORDER BY d.device_id DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDevice(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all devices with joined customer details ordered by newest to oldest.
     *
     * @return list of devices
     * @throws SQLException if a database error occurs
     */
    public static List<Device> getAllDevices() throws SQLException {
        List<Device> list = new ArrayList<>();
        String sql = "SELECT d.device_id, d.customer_id, d.category, d.brand, d.model, d.serial_number, d.colour, d.accessories, d.created_at, " +
                     "c.full_name, c.phone " +
                     "FROM devices d " +
                     "JOIN customers c ON d.customer_id = c.customer_id " +
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
     * Searches devices with an optional category filter.
     *
     * @param query search text for brand, model, serial, or customer
     * @param category category filter, or null/"All" for no filter
     * @return matching devices
     * @throws SQLException if a database error occurs
     */
    public static List<Device> searchDevices(String query, String category) throws SQLException {
        List<Device> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT d.device_id, d.customer_id, d.category, d.brand, d.model, d.serial_number, d.colour, d.accessories, d.created_at, " +
                "c.full_name, c.phone " +
                "FROM devices d " +
                "JOIN customers c ON d.customer_id = c.customer_id WHERE 1=1 "
        );

        boolean hasQuery = query != null && !query.trim().isEmpty();
        boolean hasCategory = category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("All Categories") && !category.equalsIgnoreCase("All");

        if (hasQuery) {
            sql.append("AND (LOWER(d.brand) LIKE ? OR LOWER(d.model) LIKE ? OR LOWER(d.serial_number) LIKE ? " +
                       "OR LOWER(c.full_name) LIKE ? OR c.phone LIKE ? OR CAST(d.device_id AS TEXT) = ?) ");
        }

        if (hasCategory) {
            sql.append("AND LOWER(d.category) = LOWER(?) ");
        }

        sql.append("ORDER BY d.device_id DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;

            if (hasQuery) {
                String wildcard = "%" + query.trim().toLowerCase() + "%";
                ps.setString(paramIndex++, wildcard);
                ps.setString(paramIndex++, wildcard);
                ps.setString(paramIndex++, wildcard);
                ps.setString(paramIndex++, wildcard);
                ps.setString(paramIndex++, wildcard);
                ps.setString(paramIndex++, query.trim());
            }

            if (hasCategory) {
                ps.setString(paramIndex++, category.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDevice(rs));
                }
            }
        }
        return list;
    }

    private static Device mapResultSetToDevice(ResultSet rs) throws SQLException {
        int id = rs.getInt("device_id");
        int customerId = rs.getInt("customer_id");
        String category = rs.getString("category");
        String brand = rs.getString("brand");
        String model = rs.getString("model");
        String serialNumber = rs.getString("serial_number");
        String colour = rs.getString("colour");
        String accessories = rs.getString("accessories");
        String createdAt = rs.getString("created_at");

        Device dev = new Device(id, customerId, category, brand, model, serialNumber, colour, accessories, createdAt);
        dev.setCustomerName(rs.getString("full_name"));
        dev.setCustomerPhone(rs.getString("phone"));
        return dev;
    }
}

