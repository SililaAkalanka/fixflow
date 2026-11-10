package com.fixflow.ui;

import com.fixflow.enums.NavigationItem;
import com.fixflow.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleAccessPolicyTest {

    @Test
    @DisplayName("Verify Administrator has permission to access all 10 panels including Settings")
    void testAdministratorPermissions() {
        for (NavigationItem item : NavigationItem.values()) {
            assertTrue(item.isPermitted(UserRole.ADMINISTRATOR),
                    "Administrator must have access to: " + item.name());
        }
    }

    @Test
    @DisplayName("Verify Manager permissions (All operational panels except Settings)")
    void testManagerPermissions() {
        assertTrue(NavigationItem.DASHBOARD.isPermitted(UserRole.MANAGER));
        assertTrue(NavigationItem.CUSTOMERS.isPermitted(UserRole.MANAGER));
        assertTrue(NavigationItem.DEVICES.isPermitted(UserRole.MANAGER));
        assertTrue(NavigationItem.REPAIRS.isPermitted(UserRole.MANAGER));
        assertTrue(NavigationItem.TECHNICIANS.isPermitted(UserRole.MANAGER));
        assertTrue(NavigationItem.INVENTORY.isPermitted(UserRole.MANAGER));
        assertTrue(NavigationItem.PAYMENTS.isPermitted(UserRole.MANAGER));
        assertTrue(NavigationItem.WARRANTIES.isPermitted(UserRole.MANAGER));
        assertTrue(NavigationItem.REPORTS.isPermitted(UserRole.MANAGER));

        // Settings must be restricted to Administrator only
        assertFalse(NavigationItem.SETTINGS.isPermitted(UserRole.MANAGER),
                "Manager must NOT have access to Settings");
    }

    @Test
    @DisplayName("Verify Receptionist permissions (Front-desk customer/job flow)")
    void testReceptionistPermissions() {
        assertTrue(NavigationItem.DASHBOARD.isPermitted(UserRole.RECEPTIONIST));
        assertTrue(NavigationItem.CUSTOMERS.isPermitted(UserRole.RECEPTIONIST));
        assertTrue(NavigationItem.DEVICES.isPermitted(UserRole.RECEPTIONIST));
        assertTrue(NavigationItem.REPAIRS.isPermitted(UserRole.RECEPTIONIST));
        assertTrue(NavigationItem.TECHNICIANS.isPermitted(UserRole.RECEPTIONIST));
        assertTrue(NavigationItem.PAYMENTS.isPermitted(UserRole.RECEPTIONIST));
        assertTrue(NavigationItem.WARRANTIES.isPermitted(UserRole.RECEPTIONIST));

        // Disallowed modules
        assertFalse(NavigationItem.INVENTORY.isPermitted(UserRole.RECEPTIONIST));
        assertFalse(NavigationItem.REPORTS.isPermitted(UserRole.RECEPTIONIST));
        assertFalse(NavigationItem.SETTINGS.isPermitted(UserRole.RECEPTIONIST));

        // View-only checks
        assertTrue(NavigationItem.TECHNICIANS.isReadOnly(UserRole.RECEPTIONIST),
                "Receptionist must have view-only access to technicians");
    }

    @Test
    @DisplayName("Verify Technician permissions (Repair jobs and diagnostics)")
    void testTechnicianPermissions() {
        assertTrue(NavigationItem.DASHBOARD.isPermitted(UserRole.TECHNICIAN));
        assertTrue(NavigationItem.CUSTOMERS.isPermitted(UserRole.TECHNICIAN));
        assertTrue(NavigationItem.DEVICES.isPermitted(UserRole.TECHNICIAN));
        assertTrue(NavigationItem.REPAIRS.isPermitted(UserRole.TECHNICIAN));
        assertTrue(NavigationItem.INVENTORY.isPermitted(UserRole.TECHNICIAN));

        // Disallowed modules
        assertFalse(NavigationItem.TECHNICIANS.isPermitted(UserRole.TECHNICIAN));
        assertFalse(NavigationItem.PAYMENTS.isPermitted(UserRole.TECHNICIAN));
        assertFalse(NavigationItem.WARRANTIES.isPermitted(UserRole.TECHNICIAN));
        assertFalse(NavigationItem.REPORTS.isPermitted(UserRole.TECHNICIAN));
        assertFalse(NavigationItem.SETTINGS.isPermitted(UserRole.TECHNICIAN));

        // View-only checks
        assertTrue(NavigationItem.CUSTOMERS.isReadOnly(UserRole.TECHNICIAN));
        assertTrue(NavigationItem.DEVICES.isReadOnly(UserRole.TECHNICIAN));
        assertTrue(NavigationItem.INVENTORY.isReadOnly(UserRole.TECHNICIAN));
    }

    @Test
    @DisplayName("Verify Inventory Staff permissions (Parts and stock catalog only)")
    void testInventoryStaffPermissions() {
        assertTrue(NavigationItem.DASHBOARD.isPermitted(UserRole.INVENTORY_STAFF));
        assertTrue(NavigationItem.INVENTORY.isPermitted(UserRole.INVENTORY_STAFF));
        assertFalse(NavigationItem.INVENTORY.isReadOnly(UserRole.INVENTORY_STAFF),
                "Inventory staff must have full write access to Inventory");

        // Disallowed modules
        assertFalse(NavigationItem.CUSTOMERS.isPermitted(UserRole.INVENTORY_STAFF));
        assertFalse(NavigationItem.DEVICES.isPermitted(UserRole.INVENTORY_STAFF));
        assertFalse(NavigationItem.REPAIRS.isPermitted(UserRole.INVENTORY_STAFF));
        assertFalse(NavigationItem.TECHNICIANS.isPermitted(UserRole.INVENTORY_STAFF));
        assertFalse(NavigationItem.PAYMENTS.isPermitted(UserRole.INVENTORY_STAFF));
        assertFalse(NavigationItem.WARRANTIES.isPermitted(UserRole.INVENTORY_STAFF));
        assertFalse(NavigationItem.REPORTS.isPermitted(UserRole.INVENTORY_STAFF));
        assertFalse(NavigationItem.SETTINGS.isPermitted(UserRole.INVENTORY_STAFF));
    }
}

