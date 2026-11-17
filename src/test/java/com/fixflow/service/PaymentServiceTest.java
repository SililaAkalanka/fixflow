package com.fixflow.service;

import com.fixflow.enums.PaymentStatus;
import com.fixflow.model.Customer;
import com.fixflow.model.Device;
import com.fixflow.model.Payment;
import com.fixflow.model.RepairJob;
import com.fixflow.model.SparePart;
import com.fixflow.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentServiceTest {

    private File tempDir;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = File.createTempFile("fixflow_paysvc_test_", "_dir");
        tempDir.delete();
        tempDir.mkdirs();

        CustomerRepository cr = new CustomerRepository(new File(tempDir, "customers.json").getAbsolutePath());
        DeviceRepository dr = new DeviceRepository(new File(tempDir, "devices.json").getAbsolutePath());
        TechnicianRepository tr = new TechnicianRepository(new File(tempDir, "technicians.json").getAbsolutePath());
        InventoryRepository ir = new InventoryRepository(new File(tempDir, "spare_parts.json").getAbsolutePath());
        PartUsageRepository ur = new PartUsageRepository(new File(tempDir, "part_usage.json").getAbsolutePath());
        RepairRepository rr = new RepairRepository(new File(tempDir, "repair_jobs.json").getAbsolutePath());
        PaymentRepository pr = new PaymentRepository(new File(tempDir, "payments.json").getAbsolutePath());

        CustomerService.setRepositories(cr, dr, rr);
        DeviceService.setRepositories(dr, cr, rr);
        TechnicianService.setRepositories(tr, rr);
        InventoryService.setRepositories(ir, ur);
        RepairService.setRepositories(rr, cr, dr, tr);
        PaymentService.setRepositories(pr, rr, cr, dr);
    }

    @AfterEach
    void tearDown() {
        if (tempDir != null && tempDir.exists()) {
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
            tempDir.delete();
        }
    }

    private int createTestJob(double labourCharges, double partsPrice) throws Exception {
        Customer c = new Customer("Alice Walker", "0712345678", "alice@example.com", "Colombo");
        int custId = CustomerService.createCustomer(c);
        Device d = new Device(custId, "Laptop", "Dell", "XPS", "SN-999", "Black", "Charger");
        int devId = DeviceService.createDevice(d);

        RepairJob job = new RepairJob(custId, devId, "Motherboard issue");
        job.setLabourCharges(labourCharges);
        int jobId = RepairService.createRepairJob(job);

        if (partsPrice > 0) {
            SparePart part = new SparePart("Capacitor Kit", "Replacement ICs & Components", "Dell XPS", partsPrice / 2, partsPrice, 10, 2);
            int partId = InventoryService.addPart(part);
            InventoryService.issuePartToJob(jobId, partId, 1, "Technician");
        }
        return jobId;
    }

    @Test
    @DisplayName("Verify total cost calculation with labour and spare parts")
    void testTotalCostCalculation() throws Exception {
        int jobId = createTestJob(3500.0, 4500.0);
        double total = PaymentService.calculateTotalCost(jobId);
        assertEquals(8000.0, total, 0.001);
    }

    @Test
    @DisplayName("Verify partial payments and balance calculation")
    void testPartialPaymentsAndBalance() throws Exception {
        int jobId = createTestJob(5000.0, 5000.0); // Total 10,000

        assertEquals(10000.0, PaymentService.getRemainingBalance(jobId), 0.001);
        assertEquals(PaymentStatus.UNPAID, PaymentService.getPaymentStatus(jobId));

        // 1st partial payment
        PaymentService.recordPayment(jobId, 4000.0, null, "1st installment");
        assertEquals(4000.0, PaymentService.getTotalPaid(jobId), 0.001);
        assertEquals(6000.0, PaymentService.getRemainingBalance(jobId), 0.001);
        assertEquals(PaymentStatus.PARTIALLY_PAID, PaymentService.getPaymentStatus(jobId));

        // 2nd partial payment
        PaymentService.recordPayment(jobId, 6000.0, null, "Final settlement");
        assertEquals(10000.0, PaymentService.getTotalPaid(jobId), 0.001);
        assertEquals(0.0, PaymentService.getRemainingBalance(jobId), 0.001);
        assertEquals(PaymentStatus.PAID, PaymentService.getPaymentStatus(jobId));
        assertTrue(PaymentService.isJobFullyPaid(jobId));

        List<Payment> payments = PaymentService.getPaymentsForJob(jobId);
        assertEquals(2, payments.size());
    }

    @Test
    @DisplayName("Verify overpayment is rejected")
    void testOverpaymentRejection() throws Exception {
        int jobId = createTestJob(5000.0, 0.0);

        // Attempting to pay 6000 when only 5000 is due
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                PaymentService.recordPayment(jobId, 6000.0, null, "Overpay attempt")
        );
        assertTrue(ex.getMessage().contains("exceeds remaining balance"));
    }

    @Test
    @DisplayName("Verify zero and negative payment amounts are rejected")
    void testInvalidPaymentAmounts() throws Exception {
        int jobId = createTestJob(5000.0, 0.0);

        assertThrows(IllegalArgumentException.class, () ->
                PaymentService.recordPayment(jobId, 0.0, null, "Zero payment")
        );
        assertThrows(IllegalArgumentException.class, () ->
                PaymentService.recordPayment(jobId, -500.0, null, "Negative payment")
        );
    }

    @Test
    @DisplayName("Verify paying on fully settled job throws IllegalStateException")
    void testPaymentOnFullySettledJob() throws Exception {
        int jobId = createTestJob(3000.0, 0.0);
        PaymentService.recordPayment(jobId, 3000.0, null, "Full payment");

        assertThrows(IllegalStateException.class, () ->
                PaymentService.recordPayment(jobId, 500.0, null, "Extra payment")
        );
    }
}
