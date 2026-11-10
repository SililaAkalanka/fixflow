package com.fixflow.database;

import com.fixflow.enums.RepairStatus;
import com.fixflow.model.Customer;
import com.fixflow.model.Device;
import com.fixflow.model.Payment;
import com.fixflow.model.RepairJob;
import com.fixflow.model.SparePart;
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
        DatabaseManager.setCustomJdbcUrl("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
        DatabaseManager.initializeDatabase();
    }

    @AfterEach
    void tearDown() {
        DatabaseManager.setCustomJdbcUrl(null);
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
        int jobId = RepairDB.createRepairJob(job, "Receptionist");

        // Diagnose & approve
        RepairDB.recordDiagnosis(jobId, "Keyboard membrane damaged", 120.0, labourCharges, "2026-11-20", true, "Technician");
        RepairDB.recordCustomerApproval(jobId, true, null, "Receptionist");

        // Advance REPAIRING -> READY_FOR_COLLECTION
        RepairDB.updateStatus(jobId, RepairStatus.READY_FOR_COLLECTION, "Repairs completed. Tested OK.", "Technician");
        return jobId;
    }

    @Test
    void testCalculateJobTotalCostAndPaymentSettlement() throws Exception {
        double labour = 45.0;
        int jobId = createCompletedRepairJob(labour);

        // Issue a spare part: 1x Keyboard @ $65.00
        SparePart part = new SparePart("HP Spectre Keyboard", "Keyboards", "Spectre x360", 35.0, 65.0, 5, 1);
        int partId = InventoryDB.createPart(part);
        InventoryDB.issuePartToJob(jobId, partId, 1, "Technician");

        // Total should be labour ($45) + parts ($65) = $110.00
        double totalCost = PaymentDB.calculateJobTotalCost(jobId);
        assertEquals(110.0, totalCost, 0.001, "Calculated cost must be labour + spare parts");

        // Before payment: job is not fully paid
        assertFalse(PaymentDB.isJobFullyPaid(jobId));

        // Record full cash payment
        int paymentId = PaymentDB.recordCashPayment(jobId, 110.0, "2026-11-10 10:00:00", "Receptionist");
        assertTrue(paymentId > 0, "Payment ID should be generated");

        // After payment: job is fully paid
        assertTrue(PaymentDB.isJobFullyPaid(jobId));

        Payment payment = PaymentDB.getPaymentById(paymentId);
        assertNotNull(payment);
        assertEquals(jobId, payment.getJobId());
        assertEquals(110.0, payment.getTotalRepairCost(), 0.001);
        assertEquals(110.0, payment.getAmountPaid(), 0.001);
        assertEquals("CASH", payment.getPaymentMethod());
        assertEquals("PAID", payment.getPaymentStatus());
        assertEquals("Jane Doe", payment.getCustomerName());
        assertEquals(45.0, payment.getLabourCharges(), 0.001);
        assertEquals(65.0, payment.getPartsCost(), 0.001);
    }

    @Test
    void testRejectPartialPayment() throws Exception {
        int jobId = createCompletedRepairJob(50.0);

        // Attempting to pay $40 when total is $50
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                PaymentDB.recordCashPayment(jobId, 40.0, "2026-11-10 10:00:00", "Receptionist")
        );
        assertTrue(ex.getMessage().contains("Partial payments are not supported"), "Must enforce Section 7.4 no partial payments");
        assertFalse(PaymentDB.isJobFullyPaid(jobId));
    }

    @Test
    void testRejectPaymentOnIncompleteJob() throws Exception {
        Customer c = new Customer("Bob Smith", "0775566778", "bob@example.com", "45 Galle Rd");
        int custId = CustomerDB.createCustomer(c);
        Device d = new Device(custId, "Tablet", "Apple", "iPad 9", "SN-IPAD-1", "Space Gray", "Case");
        int devId = DeviceDB.createDevice(d);

        RepairJob job = new RepairJob(custId, devId, "Battery draining quickly");
        int jobId = RepairDB.createRepairJob(job, "Receptionist");

        // Job is in RECEIVED stage
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                PaymentDB.recordCashPayment(jobId, 50.0, null, "Receptionist")
        );
        assertTrue(ex.getMessage().contains("Payment can only be recorded after repair completion"));
    }

    @Test
    void testRejectDuplicatePayment() throws Exception {
        int jobId = createCompletedRepairJob(30.0);

        PaymentDB.recordCashPayment(jobId, 30.0, null, "Receptionist");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                PaymentDB.recordCashPayment(jobId, 30.0, null, "Receptionist")
        );
        assertTrue(ex.getMessage().contains("Payment has already been recorded"));
    }

    @Test
    void testCollectionRestrictionEnforced() throws Exception {
        int jobId = createCompletedRepairJob(40.0);

        // Attempting to update status to COLLECTED without paying must fail (Business Rule 13)
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                RepairDB.updateStatus(jobId, RepairStatus.COLLECTED, "Customer collected", "Receptionist")
        );
        assertTrue(ex.getMessage().contains("Business Rule 13"));

        // Now pay in full
        PaymentDB.recordCashPayment(jobId, 40.0, null, "Receptionist");

        // Now update status to COLLECTED must succeed
        assertDoesNotThrow(() ->
                RepairDB.updateStatus(jobId, RepairStatus.COLLECTED, "Customer collected and paid in full", "Receptionist")
        );
        assertEquals(RepairStatus.COLLECTED, RepairDB.getRepairJobById(jobId).getCurrentStatus());
    }

    @Test
    void testSearchPaymentsAndUnpaidCompletedJobs() throws Exception {
        int jobId = createCompletedRepairJob(75.0);

        List<RepairJob> unpaidList = PaymentDB.getUnpaidCompletedJobs();
        assertFalse(unpaidList.isEmpty());
        assertTrue(unpaidList.stream().anyMatch(j -> j.getJobId() == jobId));

        // Settle payment
        PaymentDB.recordCashPayment(jobId, 75.0, null, "Receptionist");

        // Unpaid list should no longer include this job
        List<RepairJob> unpaidAfter = PaymentDB.getUnpaidCompletedJobs();
        assertTrue(unpaidAfter.stream().noneMatch(j -> j.getJobId() == jobId));

        // Search payments
        List<Payment> found = PaymentDB.searchPayments("Jane Doe");
        assertEquals(1, found.size());
        assertEquals(jobId, found.get(0).getJobId());
    }
}

