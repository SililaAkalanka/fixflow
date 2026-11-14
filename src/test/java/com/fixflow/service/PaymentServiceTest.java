package com.fixflow.service;

import com.fixflow.database.CustomerDB;
import com.fixflow.database.DatabaseConnection;
import com.fixflow.database.DatabaseInitializer;
import com.fixflow.database.DeviceDB;
import com.fixflow.database.InventoryDB;
import com.fixflow.database.PaymentDB;
import com.fixflow.database.RepairDB;
import com.fixflow.enums.PaymentStatus;
import com.fixflow.model.Customer;
import com.fixflow.model.Device;
import com.fixflow.model.Payment;
import com.fixflow.model.RepairJob;
import com.fixflow.model.SparePart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentServiceTest {

    private File tempDbFile;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = File.createTempFile("fixflow_paysvc_test_", ".db");
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

    private int createTestJob(double labourCharges, double partsPrice) throws Exception {
        Customer c = new Customer("Alice Walker", "0712345678", "alice@example.com", "Colombo");
        int custId = CustomerDB.createCustomer(c);
        Device d = new Device(custId, "Laptop", "Dell", "XPS", "SN-999", "Black", "Charger");
        int devId = DeviceDB.createDevice(d);

        RepairJob job = new RepairJob(custId, devId, "Motherboard issue");
        job.setLabourCharges(labourCharges);
        int jobId = RepairDB.createRepairJob(job);

        if (partsPrice > 0) {
            SparePart part = new SparePart("Capacitor Kit", "Replacement ICs & Components", "Dell XPS", partsPrice / 2, partsPrice, 10, 2);
            int partId = InventoryDB.createPart(part);
            InventoryDB.issuePartToJob(jobId, partId, 1, "Technician");
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

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                PaymentService.recordPayment(jobId, 5001.0, null, "Too much")
        );
        assertTrue(ex.getMessage().contains("exceeds remaining balance"));
    }

    @Test
    @DisplayName("Verify invalid amounts (zero, negative, NaN, infinity) rejected")
    void testInvalidAmounts() throws Exception {
        int jobId = createTestJob(5000.0, 0.0);

        assertThrows(IllegalArgumentException.class, () ->
                PaymentService.recordPayment(jobId, 0.0, null, "Zero")
        );
        assertThrows(IllegalArgumentException.class, () ->
                PaymentService.recordPayment(jobId, -500.0, null, "Negative")
        );
        assertThrows(IllegalArgumentException.class, () ->
                PaymentService.recordPayment(jobId, Double.NaN, null, "NaN")
        );
        assertThrows(IllegalArgumentException.class, () ->
                PaymentService.recordPayment(jobId, Double.POSITIVE_INFINITY, null, "Infinity")
        );
        assertThrows(IllegalArgumentException.class, () ->
                PaymentService.recordPayment(jobId, Double.NEGATIVE_INFINITY, null, "-Infinity")
        );
    }
}
