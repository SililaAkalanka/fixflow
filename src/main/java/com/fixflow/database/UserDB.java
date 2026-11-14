package com.fixflow.database;

import com.fixflow.enums.UserRole;
import com.fixflow.model.User;
import com.fixflow.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data access operations for user credentials and role-based authentication.
 * Simplified for Administrator and Technician roles.
 */
public class UserDB {

    private static final Logger LOGGER = Logger.getLogger(UserDB.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * DTO for role initialization during first-launch setup.
     */
    public record RoleCredential(String username, String plainPassword) {}

    /**
     * Checks whether first-launch setup is required.
     * Setup is required if the users table does not contain credentials for Administrator and Technician.
     *
     * @return true if initial setup is needed, false if ready for normal login
     * @throws SQLException if a database access error occurs
     */
    public static boolean isSetupRequired() throws SQLException {
        String sql = "SELECT COUNT(DISTINCT role) FROM users WHERE role IN ('ADMINISTRATOR', 'TECHNICIAN')";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int count = rs.getInt(1);
                return count < UserRole.values().length;
            }
        }
        return true;
    }

    /**
     * Authenticates a user against stored hashed credentials.
     *
     * @param username entered username
     * @param plainPassword entered plain-text password
     * @return authenticated {@link User} object, or null if authentication fails
     * @throws SQLException if a database access error occurs
     */
    public static User authenticate(String username, String plainPassword) throws SQLException {
        if (username == null || username.trim().isEmpty() || plainPassword == null || plainPassword.isEmpty()) {
            return null;
        }

        String sql = "SELECT user_id, role, username, password_hash, created_at, updated_at " +
                     "FROM users WHERE LOWER(username) = LOWER(?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    if (PasswordUtil.verifyPassword(plainPassword, storedHash)) {
                        return mapResultSetToUser(rs);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Configures initial credentials for Administrator and Technician in a single transaction.
     *
     * @param credentials map containing credentials for each role
     * @throws SQLException if any credential fails to save
     */
    public static void setupAllRoles(Map<UserRole, RoleCredential> credentials) throws SQLException {
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        String sql = "INSERT INTO users (role, username, password_hash, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?) " +
                     "ON CONFLICT(role) DO UPDATE SET " +
                     "username = excluded.username, " +
                     "password_hash = excluded.password_hash, " +
                     "updated_at = excluded.updated_at";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (UserRole role : UserRole.values()) {
                    RoleCredential cred = credentials.get(role);
                    if (cred == null || cred.username() == null || cred.plainPassword() == null) {
                        throw new IllegalArgumentException("Missing credentials for role: " + role.name());
                    }

                    String passwordHash = PasswordUtil.hashPassword(cred.plainPassword());
                    ps.setString(1, role.name());
                    ps.setString(2, cred.username().trim());
                    ps.setString(3, passwordHash);
                    ps.setString(4, now);
                    ps.setString(5, now);
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
                LOGGER.info("Configured credentials for Administrator and Technician.");
            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to setup roles, rolled back.", e);
                if (e instanceof SQLException) throw (SQLException) e;
                throw new SQLException("Setup failed: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Updates the password for a specific user role.
     *
     * @param role target role
     * @param newPlainPassword new plain text password
     * @return true if updated successfully
     * @throws SQLException if a database error occurs
     */
    public static boolean updatePassword(UserRole role, String newPlainPassword) throws SQLException {
        if (role == null || newPlainPassword == null || newPlainPassword.trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }

        String passwordHash = PasswordUtil.hashPassword(newPlainPassword.trim());
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        String sql = "UPDATE users SET password_hash = ?, updated_at = ? WHERE role = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setString(2, now);
            ps.setString(3, role.name());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Updates username and password for a specific user role.
     *
     * @param role target role
     * @param newUsername new username
     * @param newPlainPassword new plain password (optional, can be null or empty to keep current password)
     * @return true if updated successfully
     * @throws SQLException if database update fails
     */
    public static boolean updateCredentials(UserRole role, String newUsername, String newPlainPassword) throws SQLException {
        if (role == null || newUsername == null || newUsername.trim().isEmpty()) {
            throw new IllegalArgumentException("Role and username cannot be empty.");
        }

        String now = LocalDateTime.now().format(DATE_FORMATTER);

        if (newPlainPassword != null && !newPlainPassword.trim().isEmpty()) {
            if (newPlainPassword.trim().length() < 4) {
                throw new IllegalArgumentException("Password must be at least 4 characters.");
            }
            String passwordHash = PasswordUtil.hashPassword(newPlainPassword.trim());
            String sql = "UPDATE users SET username = ?, password_hash = ?, updated_at = ? WHERE role = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newUsername.trim());
                ps.setString(2, passwordHash);
                ps.setString(3, now);
                ps.setString(4, role.name());
                return ps.executeUpdate() > 0;
            }
        } else {
            String sql = "UPDATE users SET username = ?, updated_at = ? WHERE role = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newUsername.trim());
                ps.setString(2, now);
                ps.setString(3, role.name());
                return ps.executeUpdate() > 0;
            }
        }
    }

    /**
     * Alias for updateCredentials.
     */
    public static boolean updateUserCredentials(UserRole role, String newUsername, String newPlainPassword) throws SQLException {
        return updateCredentials(role, newUsername, newPlainPassword);
    }

    /**
     * Retrieves user record by role.
     *
     * @param role user role
     * @return {@link User} object, or null if not found
     * @throws SQLException if a database error occurs
     */
    public static User getUserByRole(UserRole role) throws SQLException {
        if (role == null) return null;
        String sql = "SELECT user_id, role, username, password_hash, created_at, updated_at FROM users WHERE role = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all user accounts currently configured in the database.
     *
     * @return list of {@link User} records
     * @throws SQLException if a database error occurs
     */
    public static List<User> getAllUsers() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT user_id, role, username, password_hash, created_at, updated_at FROM users ORDER BY user_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                User u = mapResultSetToUser(rs);
                if (u != null) {
                    list.add(u);
                }
            }
        }
        return list;
    }

    private static User mapResultSetToUser(ResultSet rs) throws SQLException {
        int userId = rs.getInt("user_id");
        String roleStr = rs.getString("role");
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        String createdAt = rs.getString("created_at");
        String updatedAt = rs.getString("updated_at");

        try {
            UserRole role = UserRole.valueOf(roleStr);
            return new User(userId, role, username, passwordHash, createdAt, updatedAt);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Unknown role '" + roleStr + "' in users table, skipping.");
            return null;
        }
    }
}
