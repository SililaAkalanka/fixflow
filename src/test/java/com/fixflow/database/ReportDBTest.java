package com.fixflow.database;

import com.fixflow.enums.RepairStatus;
import com.fixflow.model.Customer;
import com.fixflow.model.DashboardMetrics;
import com.fixflow.model.Device;
import com.fixflow.model.Payment;
import com.fixflow.model.RepairJob;
import com.fixflow.model.SparePart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReportDBTest {

    private File tempDbFile;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = File.createTempFile("fixflow_rep_test_", ".db");
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

    private int setupTestScenario() throws Exception {
        // Customer & Devices
        Customer c1 = new Customer("Peter Parker", "0770011223", "peter@dailybugle.com", "Queens, NY");
        int c1Id = CustomerDB.createCustomer(c1);
        Device d1 = new Device(c1Id, "Phone", "Sony", "Xperia 1", "SN-SONY-01", "Black", "Charger");
        int d1Id = DeviceDB.createDevice(d1);

        // Job 1: Ready for collection, paid
        RepairJob job1 = new RepairJob(c1Id, d1Id, "Broken camera lens");
        int j1Id = RepairDB.createRepairJob(job1, "Receptionist");
        RepairDB.recordDiagnosis(j1Id, "Camera module replaced", 90.0, 40.0, "2026-11-20", true, "Technician");
        RepairDB.recordCustomerApproval(j1Id, true, null, "Receptionist");

        SparePart p1 = new SparePart("Sony Camera Module", "Cameras", "Xperia 1", 25.0, 50.0, 4, 2); // 4 in stock
        int p1Id = InventoryDB.createPart(p1);
        InventoryDB.issuePartToJob(j1Id, p1Id, 1, "Technician"); // Stock becomes 3

        RepairDB.updateStatus(j1Id, RepairStatus.READY_FOR_COLLECTION, "Ready", "Technician");
        PaymentDB.recordCashPayment(j1Id, 90.0, "2026-11-10 12:00:00", "Receptionist"); // Paid $90 ($40 labour + $50 parts)
        RepairDB.updateStatus(j1Id, RepairStatus.COLLECTED, "Collected by customer", "Receptionist");

        // Job 2: Waiting for parts (Unpaid, In-progress)
        Device d2 = new Device(c1Id, "Laptop", "Dell", "Latitude", "SN-DELL-02", "Silver", "Charger");
        int d2Id = DeviceDB.createDevice(d2);
        RepairJob job2 = new RepairJob(c1Id, d2Id, "Trackpad unresponsive");
        int j2Id = RepairDB.createRepairJob(job2, "Receptionist");
        RepairDB.recordDiagnosis(j2Id, "Awaiting trackpad part", 60.0, 30.0, "2026-11-25", true, "Technician");
        RepairDB.recordCustomerApproval(j2Id, true, null, "Receptionist");
        RepairDB.updateStatus(j2Id, RepairStatus.WAITING_FOR_PARTS, "Awaiting supplier", "Technician");

        // Spare part 2: Low stock critical (1 in stock, min 5)
        SparePart p2 = new SparePart("Dell Trackpad", "Trackpads", "Latitude", 15.0, 30.0, 1, 5);
        InventoryDB.createPart(p2);

        // Spare part 3: Out of stock (0 in stock)
        SparePart p3 = new SparePart("Universal USB-C Cable", "Cables", "Universal", 2.0, 8.0, 0, 3);
        InventoryDB.createPart(p3);

        return j1Id;
    }

    @Test
    void testDashboardMetricsAggregation() throws Exception {
        setupTestScenario();

        DashboardMetrics metrics = ReportDB.getDashboardMetrics(null);
        assertNotNull(metrics);

        // 2 total jobs created
        assertEquals(2, metrics.getTotalJobs());
        // Job 2 is WAITING_FOR_PARTS (active in shop)
        assertEquals(1, metrics.getActiveJobs());
        assertEquals(1, metrics.getWaitingPartsJobs());
        // Job 1 is COLLECTED
        assertEquals(1, metrics.getCollectedJobs());
        assertEquals(1, metrics.getStatusCount(RepairStatus.COLLECTED));
        assertEquals(1, metrics.getStatusCount(RepairStatus.WAITING_FOR_PARTS));

        // Revenue: Job 1 paid $90.00
        assertEquals(90.0, metrics.getTotalRevenue(), 0.001);

        // Inventory: p2 has 1 (min 5) -> low stock; p3 has 0 -> out of stock
        assertEquals(2, metrics.getLowStockCount(), "p2 (qty 1) and p3 (qty 0) are <= min stock level");
        assertEquals(1, metrics.getOutOfStockCount());

        // Valuation: p1 (3 * $25 = $75) + p2 (1 * $15 = $15) + p3 (0) = $90.00
        assertEquals(90.0, metrics.getTotalInventoryValuation(), 0.001);
    }

    @Test
    void testRepairJobsReportFiltering() throws Exception {
        setupTestScenario();

        // 1. All jobs
        List<RepairJob> all = ReportDB.getRepairJobsReport(null, null, null, null);
        assertEquals(2, all.size());

        // 2. Filter by status WAITING_FOR_PARTS
        List<RepairJob> waiting = ReportDB.getRepairJobsReport(null, null, RepairStatus.WAITING_FOR_PARTS, null);
        assertEquals(1, waiting.size());
        assertEquals(RepairStatus.WAITING_FOR_PARTS, waiting.get(0).getCurrentStatus());

        // 3. Filter by date range
        String today = LocalDate.now().toString();
        List<RepairJob> todayJobs = ReportDB.getRepairJobsReport(today, today, null, null);
        assertEquals(2, todayJobs.size());
    }

    @Test
    void testRevenueReport() throws Exception {
        setupTestScenario();

        List<Payment> payments = ReportDB.getRevenueReport(null, null);
        assertEquals(1, payments.size());

        Payment p = payments.get(0);
        assertEquals(90.0, p.getAmountPaid(), 0.001);
        assertEquals(40.0, p.getLabourCharges(), 0.001);
        assertEquals(50.0, p.getPartsCost(), 0.001);
        assertEquals("Peter Parker", p.getCustomerName());
    }

    @Test
    void testInventoryReportFiltering() throws Exception {
        setupTestScenario();

        // All parts (3 catalog items)
        List<SparePart> allParts = ReportDB.getInventoryReport(null, "ALL");
        assertEquals(3, allParts.size());

        // Out of stock only (p3)
        List<SparePart> outOfStock = ReportDB.getInventoryReport(null, "OUT_OF_STOCK");
        assertEquals(1, outOfStock.size());
        assertEquals("Universal USB-C Cable", outOfStock.get(0).getPartName());

        // In stock only (p1 with 3 in stock, min 2)
        List<SparePart> inStock = ReportDB.getInventoryReport(null, "IN_STOCK");
        assertEquals(1, inStock.size());
        assertEquals("Sony Camera Module", inStock.get(0).getPartName());
    }
}

