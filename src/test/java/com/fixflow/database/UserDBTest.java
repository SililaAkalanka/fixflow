package com.fixflow.database;

import com.fixflow.enums.UserRole;
import com.fixflow.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserDBTest {

    private Connection sharedMemoryConnection;

    @BeforeEach
    void setUp() throws SQLException {
        String testUrl = "jdbc:sqlite:file:userdb_test?mode=memory&cache=shared";
        DatabaseManager.setCustomJdbcUrl(testUrl);

        // Keep shared root connection open for in-memory SQLite lifespan
        sharedMemoryConnection = DatabaseManager.getConnection();
        DatabaseManager.initializeDatabase();
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (sharedMemoryConnection != null && !sharedMemoryConnection.isClosed()) {
            sharedMemoryConnection.close();
        }
        DatabaseManager.setCustomJdbcUrl(null);
    }

    @Test
    @DisplayName("Verify isSetupRequired is true on empty database and false after setup")
    void testSetupRequiredCycle() throws SQLException {
        // Initially empty
        assertTrue(UserDB.isSetupRequired(), "Setup should be required on empty database");

        // Populate credentials for all 5 roles
        Map<UserRole, UserDB.RoleCredential> creds = buildTestCredentials();
        UserDB.setupAllRoles(creds);

        // Now setup should not be required
        assertFalse(UserDB.isSetupRequired(), "Setup should not be required after all roles are configured");
    }

    @Test
    @DisplayName("Verify authentication succeeds with valid credentials")
    void testAuthenticateSuccess() throws SQLException {
        UserDB.setupAllRoles(buildTestCredentials());

        User admin = UserDB.authenticate("admin", "adminPass123");
        assertNotNull(admin, "Authentication should succeed for valid credentials");
        assertEquals(UserRole.ADMINISTRATOR, admin.getRole());
        assertEquals("admin", admin.getUsername());

        User tech = UserDB.authenticate("technician", "techPass123");
        assertNotNull(tech);
        assertEquals(UserRole.TECHNICIAN, tech.getRole());
    }

    @Test
    @DisplayName("Verify authentication fails with invalid username or password")
    void testAuthenticateFailure() throws SQLException {
        UserDB.setupAllRoles(buildTestCredentials());

        assertNull(UserDB.authenticate("admin", "wrongPassword"));
        assertNull(UserDB.authenticate("unknownUser", "adminPass123"));
        assertNull(UserDB.authenticate("", "adminPass123"));
        assertNull(UserDB.authenticate(null, "adminPass123"));
    }

    @Test
    @DisplayName("Verify password update allows authentication with new password")
    void testUpdatePassword() throws SQLException {
        UserDB.setupAllRoles(buildTestCredentials());

        boolean updated = UserDB.updatePassword(UserRole.MANAGER, "newManagerPass456");
        assertTrue(updated);

        // Old password must fail
        assertNull(UserDB.authenticate("manager", "managerPass123"));

        // New password must succeed
        User manager = UserDB.authenticate("manager", "newManagerPass456");
        assertNotNull(manager);
        assertEquals(UserRole.MANAGER, manager.getRole());
    }

    @Test
    @DisplayName("Verify getAllUsers returns all 5 configured roles")
    void testGetAllUsers() throws SQLException {
        UserDB.setupAllRoles(buildTestCredentials());

        List<User> users = UserDB.getAllUsers();
        assertEquals(5, users.size(), "Should have exactly 5 configured roles");

        User admin = UserDB.getUserByRole(UserRole.ADMINISTRATOR);
        assertNotNull(admin);
        assertEquals("admin", admin.getUsername());
    }

    private Map<UserRole, UserDB.RoleCredential> buildTestCredentials() {
        Map<UserRole, UserDB.RoleCredential> creds = new EnumMap<>(UserRole.class);
        creds.put(UserRole.ADMINISTRATOR, new UserDB.RoleCredential("admin", "adminPass123"));
        creds.put(UserRole.MANAGER, new UserDB.RoleCredential("manager", "managerPass123"));
        creds.put(UserRole.RECEPTIONIST, new UserDB.RoleCredential("receptionist", "recepPass123"));
        creds.put(UserRole.TECHNICIAN, new UserDB.RoleCredential("technician", "techPass123"));
        creds.put(UserRole.INVENTORY_STAFF, new UserDB.RoleCredential("inventory", "invPass123"));
        return creds;
    }
}

