package com.fixflow.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnumsTest {

    @Test
    @DisplayName("Verify UserRole enum contains two predefined roles")
    void testUserRoles() {
        assertEquals(2, UserRole.values().length);
        assertNotNull(UserRole.valueOf("ADMINISTRATOR"));
        assertNotNull(UserRole.valueOf("TECHNICIAN"));
    }

    @Test
    @DisplayName("Verify RepairStatus enum contains all 6 simplified statuses")
    void testRepairStatuses() {
        assertEquals(6, RepairStatus.values().length);
        assertTrue(RepairStatus.COLLECTED.isTerminal());
        assertTrue(RepairStatus.CANCELLED.isTerminal());
        assertFalse(RepairStatus.RECEIVED.isTerminal());
        assertFalse(RepairStatus.DIAGNOSING.isTerminal());
        assertFalse(RepairStatus.IN_PROGRESS.isTerminal());
        assertFalse(RepairStatus.COMPLETED.isTerminal());
    }

    @Test
    @DisplayName("Verify PaymentStatus enum contains 3 partial-payment statuses")
    void testPaymentStatuses() {
        assertEquals(3, PaymentStatus.values().length);
        assertNotNull(PaymentStatus.valueOf("UNPAID"));
        assertNotNull(PaymentStatus.valueOf("PARTIALLY_PAID"));
        assertNotNull(PaymentStatus.valueOf("PAID"));
    }
}
