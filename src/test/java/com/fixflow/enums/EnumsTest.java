package com.fixflow.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnumsTest {

    @Test
    @DisplayName("Verify UserRole enum contains all five predefined roles")
    void testUserRoles() {
        assertEquals(5, UserRole.values().length);
        assertNotNull(UserRole.valueOf("ADMINISTRATOR"));
        assertNotNull(UserRole.valueOf("MANAGER"));
        assertNotNull(UserRole.valueOf("RECEPTIONIST"));
        assertNotNull(UserRole.valueOf("TECHNICIAN"));
        assertNotNull(UserRole.valueOf("INVENTORY_STAFF"));
    }

    @Test
    @DisplayName("Verify RepairStatus enum contains all eight specification statuses")
    void testRepairStatuses() {
        assertEquals(8, RepairStatus.values().length);
        assertTrue(RepairStatus.COLLECTED.isTerminal());
        assertTrue(RepairStatus.CANCELLED.isTerminal());
        assertFalse(RepairStatus.RECEIVED.isTerminal());
        assertFalse(RepairStatus.REPAIRING.isTerminal());
    }

    @Test
    @DisplayName("Verify PaymentStatus enum contains required statuses")
    void testPaymentStatuses() {
        assertEquals(2, PaymentStatus.values().length);
        assertNotNull(PaymentStatus.valueOf("PENDING"));
        assertNotNull(PaymentStatus.valueOf("PAID"));
    }
}

