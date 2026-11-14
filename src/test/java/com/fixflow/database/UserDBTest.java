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
        DatabaseConnection.setCustomJdbcUrl(testUrl);

        sharedMemoryConnection = DatabaseConnection.getConnection();
        DatabaseInitializer.initializeDatabase();
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (sharedMemoryConnection != null && !sharedMemoryConnection.isClosed()) {
            sharedMemoryConnection.close();
        }
        DatabaseConnection.setCustomJdbcUrl(null);
    }

    @Test
    @DisplayName("Verify isSetupRequired is true on empty database and false after setup")
    void testSetupRequiredCycle() throws SQLException {
        assertTrue(UserDB.isSetupRequired(), "Setup should be required on empty database");

        Map<UserRole, UserDB.RoleCredential> creds = buildTestCredentials();
        UserDB.setupAllRoles(creds);

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

        boolean updated = UserDB.updatePassword(UserRole.TECHNICIAN, "newTechPass456");
        assertTrue(updated);

        // Old password must fail
        assertNull(UserDB.authenticate("technician", "techPass123"));

        // New password must succeed
        User tech = UserDB.authenticate("technician", "newTechPass456");
        assertNotNull(tech);
        assertEquals(UserRole.TECHNICIAN, tech.getRole());
    }

    @Test
    @DisplayName("Verify getAllUsers returns configured roles")
    void testGetAllUsers() throws SQLException {
        UserDB.setupAllRoles(buildTestCredentials());

        List<User> users = UserDB.getAllUsers();
        assertEquals(2, users.size(), "Should have exactly 2 configured roles");

        User admin = UserDB.getUserByRole(UserRole.ADMINISTRATOR);
        assertNotNull(admin);
        assertEquals("admin", admin.getUsername());
    }

    private Map<UserRole, UserDB.RoleCredential> buildTestCredentials() {
        Map<UserRole, UserDB.RoleCredential> creds = new EnumMap<>(UserRole.class);
        creds.put(UserRole.ADMINISTRATOR, new UserDB.RoleCredential("admin", "adminPass123"));
        creds.put(UserRole.TECHNICIAN, new UserDB.RoleCredential("technician", "techPass123"));
        return creds;
    }
}
