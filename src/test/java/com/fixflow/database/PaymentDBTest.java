package com.fixflow.database;

import com.fixflow.enums.PaymentStatus;
import com.fixflow.enums.RepairStatus;
import com.fixflow.model.Customer;
import com.fixflow.model.Device;
import com.fixflow.model.Payment;
import com.fixflow.model.RepairJob;
import com.fixflow.model.SparePart;
import com.fixflow.service.PaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentDBTest {

    private File tempDbFile;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = File.createTempFile("fixflow_pay_test_", ".db");
        DatabaseConnection.setCustomJdbcUrl("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
        DatabaseInitializer.initializeDatabase();
    }

    @AfterEach
    void tearDown() {
        DatabaseConnection.setCustomJdbcUrl(null);
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    private int createCompletedRepairJob(double labourCharges) throws Exception {
        Customer c = new Customer("Jane Doe", "0771122334", "jane@example.com", "10 Colombo Rd");
        int custId = CustomerDB.createCustomer(c);
        Device d = new Device(custId, "Laptop", "HP", "Spectre x360", "SN-HP-9988", "Nightfall Black", "Charger");
        int devId = DeviceDB.createDevice(d);

        RepairJob job = new RepairJob(custId, devId, "Keyboard keys unresponsive");
        job.setLabourCharges(labourCharges);
        int jobId = RepairDB.createRepairJob(job);

        RepairDB.updateStatus(jobId, RepairStatus.DIAGNOSING, "Diagnosis underway");
        RepairDB.updateStatus(jobId, RepairStatus.IN_PROGRESS, "Working on keyboard");
        RepairDB.updateStatus(jobId, RepairStatus.COMPLETED, "Repair complete");
        return jobId;
    }

    @Test
    void testTotalRepairCostCalculation() throws Exception {
        double labour = 5000.0;
        int jobId = createCompletedRepairJob(labour);

        // Issue a spare part: 1x Keyboard @ LKR 6500.00
        SparePart part = new SparePart("HP Spectre Keyboard", "Keyboards", "Spectre x360", 3500.0, 6500.0, 5, 1);
        int partId = InventoryDB.createPart(part);
        InventoryDB.issuePartToJob(jobId, partId, 1, "Technician");

        // Total should be labour (5000) + parts (6500) = 11500.00
        double totalCost = PaymentService.calculateTotalCost(jobId);
        assertEquals(11500.0, totalCost, 0.001, "Calculated cost must be labour + spare parts");
    }

    @Test
    void testMultiplePartialPaymentsAndBalanceProgression() throws Exception {
        int jobId = createCompletedRepairJob(10000.0);

        // Initial state: UNPAID, balance = 10000.00
        assertEquals(10000.0, PaymentService.getRemainingBalance(jobId), 0.001);
        assertEquals(PaymentStatus.UNPAID, PaymentService.getPaymentStatus(jobId));
        assertFalse(PaymentService.isJobFullyPaid(jobId));

        // 1st partial payment: LKR 4000.00
        int p1 = PaymentService.recordPayment(jobId, 4000.0, null, "Initial deposit");
        assertTrue(p1 > 0);
        assertEquals(4000.0, PaymentService.getTotalPaid(jobId), 0.001);
        assertEquals(6000.0, PaymentService.getRemainingBalance(jobId), 0.001);
        assertEquals(PaymentStatus.PARTIALLY_PAID, PaymentService.getPaymentStatus(jobId));
        assertFalse(PaymentService.isJobFullyPaid(jobId));

        // 2nd partial payment: LKR 3000.00
        int p2 = PaymentService.recordPayment(jobId, 3000.0, null, "Second installment");
        assertTrue(p2 > 0);
        assertEquals(7000.0, PaymentService.getTotalPaid(jobId), 0.001);
        assertEquals(3000.0, PaymentService.getRemainingBalance(jobId), 0.001);
        assertEquals(PaymentStatus.PARTIALLY_PAID, PaymentService.getPaymentStatus(jobId));

        // Final payment: LKR 3000.00
        int p3 = PaymentService.recordPayment(jobId, 3000.0, null, "Final settlement");
        assertTrue(p3 > 0);
        assertEquals(10000.0, PaymentService.getTotalPaid(jobId), 0.001);
        assertEquals(0.0, PaymentService.getRemainingBalance(jobId), 0.001);
        assertEquals(PaymentStatus.PAID, PaymentService.getPaymentStatus(jobId));
        assertTrue(PaymentService.isJobFullyPaid(jobId));

        // Verify payments in DB
        List<Payment> payments = PaymentDB.getPaymentsByJobId(jobId);
        assertEquals(3, payments.size());
    }

    @Test
    void testRejectOverpayment() throws Exception {
        int jobId = createCompletedRepairJob(5000.0);

        // Attempting to pay 6000 when remaining balance is 5000
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                PaymentService.recordPayment(jobId, 6000.0, null, "Overpay attempt")
        );
        assertTrue(ex.getMessage().contains("exceeds remaining balance"));
    }

    @Test
    void testRejectZeroOrNegativePayment() throws Exception {
        int jobId = createCompletedRepairJob(5000.0);

        assertThrows(IllegalArgumentException.class, () ->
                PaymentService.recordPayment(jobId, 0.0, null, "Zero payment")
        );
        assertThrows(IllegalArgumentException.class, () ->
                PaymentService.recordPayment(jobId, -100.0, null, "Negative payment")
        );
    }
}
