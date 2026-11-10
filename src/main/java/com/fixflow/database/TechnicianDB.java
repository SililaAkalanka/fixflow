package com.fixflow.database;

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

/**
 * Data access operations for repair technicians.
 * Implements Section 7.4 of the FixFlow Project Specification.
 */
public class TechnicianDB {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Registers a new technician in the database.
     *
     * @param technician technician model
     * @return generated technician_id
     * @throws SQLException if a database error occurs
     */
    public static int createTechnician(Technician technician) throws SQLException {
        String sql = "INSERT INTO technicians (name, phone, specialization, is_active, created_at) VALUES (?, ?, ?, ?, ?)";
        String now = LocalDateTime.now().format(DATE_FORMATTER);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, technician.getName().trim());
            ps.setString(2, technician.getPhone().trim());
            ps.setString(3, technician.getSpecialization() != null && !technician.getSpecialization().trim().isEmpty() ? technician.getSpecialization().trim() : null);
            ps.setInt(4, technician.isActive() ? 1 : 0);
            ps.setString(5, now);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    technician.setTechnicianId(id);
                    technician.setCreatedAt(now);
                    return id;
                }
            }
        }
        throw new SQLException("Failed to retrieve generated technician ID.");
    }

    /**
     * Updates an existing technician's information.
     *
     * @param technician technician model with updated fields
     * @return true if updated, false if not found
     * @throws SQLException if a database error occurs
     */
    public static boolean updateTechnician(Technician technician) throws SQLException {
        String sql = "UPDATE technicians SET name = ?, phone = ?, specialization = ?, is_active = ? WHERE technician_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, technician.getName().trim());
            ps.setString(2, technician.getPhone().trim());
            ps.setString(3, technician.getSpecialization() != null && !technician.getSpecialization().trim().isEmpty() ? technician.getSpecialization().trim() : null);
            ps.setInt(4, technician.isActive() ? 1 : 0);
            ps.setInt(5, technician.getTechnicianId());

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Activates or deactivates a technician.
     * In accordance with Section 7.4, deactivation prevents new assignments while preserving assignment history.
     *
     * @param technicianId technician ID
     * @param active true to activate, false to deactivate
     * @return true if updated
     * @throws SQLException if a database error occurs
     */
    public static boolean setTechnicianActive(int technicianId, boolean active) throws SQLException {
        String sql = "UPDATE technicians SET is_active = ? WHERE technician_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, technicianId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a technician record.
     * Blocked if the technician has any historical or active repair assignments (Section 7.4 & Section 7.10).
     *
     * @param technicianId ID of technician to delete
     * @return true if deleted
     * @throws SQLException if deletion is blocked
     */
    public static boolean deleteTechnician(int technicianId) throws SQLException {
        if (hasAssignedJobs(technicianId)) {
            throw new SQLException("Cannot delete Technician #" + technicianId + ": Associated repair assignment history exists. " +
                    "Please deactivate the technician instead to prevent new assignments while preserving historical records.");
        }

        String sql = "DELETE FROM technicians WHERE technician_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, technicianId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Checks if a technician has any assigned repair jobs.
     *
     * @param technicianId technician ID
     * @return true if assigned jobs exist, false otherwise
     * @throws SQLException if a database error occurs
     */
    public static boolean hasAssignedJobs(int technicianId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM repair_jobs WHERE technician_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, technicianId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Retrieves a technician by their ID.
     *
     * @param technicianId technician ID
     * @return {@link Technician} object, or null if not found
     * @throws SQLException if a database error occurs
     */
    public static Technician getTechnicianById(int technicianId) throws SQLException {
        String sql = "SELECT technician_id, name, phone, specialization, is_active, created_at FROM technicians WHERE technician_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, technicianId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTechnician(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all technicians.
     *
     * @return list of technicians
     * @throws SQLException if a database error occurs
     */
    public static List<Technician> getAllTechnicians() throws SQLException {
        List<Technician> list = new ArrayList<>();
        String sql = "SELECT technician_id, name, phone, specialization, is_active, created_at FROM technicians ORDER BY technician_id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToTechnician(rs));
            }
        }
        return list;
    }

    /**
     * Retrieves only currently active technicians available for job assignments.
     * Implements Section 7.4 business rule: Inactive technicians cannot receive new assignments.
     *
     * @return list of active technicians
     * @throws SQLException if a database error occurs
     */
    public static List<Technician> getActiveTechnicians() throws SQLException {
        List<Technician> list = new ArrayList<>();
        String sql = "SELECT technician_id, name, phone, specialization, is_active, created_at " +
                     "FROM technicians WHERE is_active = 1 ORDER BY name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToTechnician(rs));
            }
        }
        return list;
    }

    /**
     * Searches technicians by name, phone, or specialization with optional active filter.
     *
     * @param query search text
     * @param statusFilter "All", "Active Only", or "Inactive Only"
     * @return matching technicians
     * @throws SQLException if a database error occurs
     */
    public static List<Technician> searchTechnicians(String query, String statusFilter) throws SQLException {
        List<Technician> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT technician_id, name, phone, specialization, is_active, created_at FROM technicians WHERE 1=1 "
        );

        boolean hasQuery = query != null && !query.trim().isEmpty();
        if (hasQuery) {
            sql.append("AND (LOWER(name) LIKE ? OR phone LIKE ? OR LOWER(specialization) LIKE ? OR CAST(technician_id AS TEXT) = ?) ");
        }

        if ("Active Only".equalsIgnoreCase(statusFilter)) {
            sql.append("AND is_active = 1 ");
        } else if ("Inactive Only".equalsIgnoreCase(statusFilter)) {
            sql.append("AND is_active = 0 ");
        }

        sql.append("ORDER BY technician_id DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;

            if (hasQuery) {
                String wildcard = "%" + query.trim().toLowerCase() + "%";
                ps.setString(paramIndex++, wildcard);
                ps.setString(paramIndex++, wildcard);
                ps.setString(paramIndex++, wildcard);
                ps.setString(paramIndex++, query.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTechnician(rs));
                }
            }
        }
        return list;
    }

    private static Technician mapResultSetToTechnician(ResultSet rs) throws SQLException {
        int id = rs.getInt("technician_id");
        String name = rs.getString("name");
        String phone = rs.getString("phone");
        String specialization = rs.getString("specialization");
        boolean active = rs.getInt("is_active") == 1;
        String createdAt = rs.getString("created_at");
        return new Technician(id, name, phone, specialization, active, createdAt);
    }
}

