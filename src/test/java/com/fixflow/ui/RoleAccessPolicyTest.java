package com.fixflow.ui;

import com.fixflow.enums.NavigationItem;
import com.fixflow.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleAccessPolicyTest {

    @Test
    @DisplayName("Verify Administrator has permission to access all screens")
    void testAdministratorPermissions() {
        for (NavigationItem item : NavigationItem.values()) {
            assertTrue(item.isPermitted(UserRole.ADMINISTRATOR),
                    "Administrator must have access to: " + item.name());
            assertFalse(item.isReadOnly(UserRole.ADMINISTRATOR),
                    "Administrator must have write access to: " + item.name());
        }
    }

    @Test
    @DisplayName("Verify Technician permissions (Operational access & view-only rules)")
    void testTechnicianPermissions() {
        assertTrue(NavigationItem.DASHBOARD.isPermitted(UserRole.TECHNICIAN));
        assertTrue(NavigationItem.CUSTOMERS.isPermitted(UserRole.TECHNICIAN));
        assertTrue(NavigationItem.DEVICES.isPermitted(UserRole.TECHNICIAN));
        assertTrue(NavigationItem.REPAIRS.isPermitted(UserRole.TECHNICIAN));
        assertTrue(NavigationItem.INVENTORY.isPermitted(UserRole.TECHNICIAN));
        assertTrue(NavigationItem.PAYMENTS.isPermitted(UserRole.TECHNICIAN));

        // Admin-only management modules
        assertFalse(NavigationItem.TECHNICIANS.isPermitted(UserRole.TECHNICIAN));
        assertFalse(NavigationItem.REPORTS.isPermitted(UserRole.TECHNICIAN));
        assertFalse(NavigationItem.SETTINGS.isPermitted(UserRole.TECHNICIAN));

        // View-only restrictions for Technician on customer/device directories
        assertTrue(NavigationItem.CUSTOMERS.isReadOnly(UserRole.TECHNICIAN));
        assertTrue(NavigationItem.DEVICES.isReadOnly(UserRole.TECHNICIAN));
    }
}
