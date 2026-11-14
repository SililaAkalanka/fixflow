package com.fixflow.util;

import com.fixflow.enums.UserRole;
import com.fixflow.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    @BeforeEach
    @AfterEach
    void cleanSession() {
        SessionManager.logout();
    }

    @Test
    @DisplayName("Verify default state when unauthenticated")
    void testInitialState() {
        assertFalse(SessionManager.isLoggedIn());
        assertNull(SessionManager.getCurrentUser());
        assertNull(SessionManager.getCurrentRole());
        assertFalse(SessionManager.isAdmin());
        assertFalse(SessionManager.hasRole(UserRole.ADMINISTRATOR));
    }

    @Test
    @DisplayName("Verify successful login sets user and role")
    void testLogin() {
        User adminUser = new User(1, UserRole.ADMINISTRATOR, "admin", "dummyHash", "2026-11-10 10:00:00", null);
        SessionManager.login(adminUser);

        assertTrue(SessionManager.isLoggedIn());
        assertEquals(adminUser, SessionManager.getCurrentUser());
        assertEquals(UserRole.ADMINISTRATOR, SessionManager.getCurrentRole());
        assertTrue(SessionManager.isAdmin());
        assertTrue(SessionManager.hasRole(UserRole.ADMINISTRATOR));
        assertFalse(SessionManager.hasRole(UserRole.TECHNICIAN));
    }

    @Test
    @DisplayName("Verify non-admin role checks")
    void testTechnicianLogin() {
        User techUser = new User(2, UserRole.TECHNICIAN, "tech01", "dummyHash", "2026-11-10 10:00:00", null);
        SessionManager.login(techUser);

        assertTrue(SessionManager.isLoggedIn());
        assertEquals(UserRole.TECHNICIAN, SessionManager.getCurrentRole());
        assertFalse(SessionManager.isAdmin());
        assertTrue(SessionManager.hasRole(UserRole.TECHNICIAN));
        assertFalse(SessionManager.hasRole(UserRole.ADMINISTRATOR));
    }

    @Test
    @DisplayName("Verify logout clears session state")
    void testLogout() {
        User user = new User(3, UserRole.TECHNICIAN, "tech02", "dummyHash", "2026-11-10 10:00:00", null);
        SessionManager.login(user);
        assertTrue(SessionManager.isLoggedIn());

        SessionManager.logout();
        assertFalse(SessionManager.isLoggedIn());
        assertNull(SessionManager.getCurrentUser());
        assertNull(SessionManager.getCurrentRole());
    }
}

