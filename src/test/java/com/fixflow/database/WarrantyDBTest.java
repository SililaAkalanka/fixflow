package com.fixflow.database;

import com.fixflow.enums.RepairStatus;
import com.fixflow.enums.WarrantyStatus;
import com.fixflow.model.Customer;
import com.fixflow.model.Device;
import com.fixflow.model.RepairJob;
import com.fixflow.model.Warranty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WarrantyDBTest {

    private File tempDbFile;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = File.createTempFile("fixflow_war_test_", ".db");
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

    private int createCollectedRepairJob() throws Exception {
        Customer c = new Customer("Michael Scott", "0773344556", "michael@dundermifflin.com", "Scranton Branch");
        int custId = CustomerDB.createCustomer(c);
        Device d = new Device(custId, "Phone", "Samsung", "Galaxy S22", "SN-SAM-7766", "Phantom White", "Cable");
        int devId = DeviceDB.createDevice(d);

        RepairJob job = new RepairJob(custId, devId, "Charging port loose");
        int jobId = RepairDB.createRepairJob(job, "Receptionist");

        RepairDB.recordDiagnosis(jobId, "Port solder joints cracked", 40.0, 30.0, "2026-11-20", true, "Technician");
        RepairDB.recordCustomerApproval(jobId, true, null, "Receptionist");
        RepairDB.updateStatus(jobId, RepairStatus.READY_FOR_COLLECTION, "Repaired", "Technician");

        // Pay full cash
        PaymentDB.recordCashPayment(jobId, 30.0, null, "Receptionist");

        // Collect
        RepairDB.updateStatus(jobId, RepairStatus.COLLECTED, "Collected by customer", "Receptionist");
        return jobId;
    }

    @Test
    void testAutoCreationOf30DayWarrantyOnCollection() throws Exception {
        int jobId = createCollectedRepairJob();

        Warranty warranty = WarrantyDB.getWarrantyByJobId(jobId);
        assertNotNull(warranty, "Warranty should be automatically created upon transition to COLLECTED (Business Rule 14)");

        LocalDate today = LocalDate.now();
        LocalDate expectedExpiry = today.plusDays(30);

        assertEquals(today.toString(), warranty.getStartDate());
        assertEquals(expectedExpiry.toString(), warranty.getExpiryDate());
        assertEquals(WarrantyStatus.ACTIVE, warranty.getStatus());
        assertTrue(warranty.isCurrentlyValid());
        assertTrue(warranty.getDaysRemaining() >= 29 && warranty.getDaysRemaining() <= 30);
    }

    @Test
    void testClaimWarrantyCreatesLinkedReRepair() throws Exception {
        int origJobId = createCollectedRepairJob();
        Warranty warranty = WarrantyDB.getWarrantyByJobId(origJobId);
        assertNotNull(warranty);

        // File warranty claim
        String defect = "Charging stopped working again after 3 days";
        int newJobId = WarrantyDB.claimWarranty(warranty.getWarrantyId(), defect, "Receptionist");
        assertTrue(newJobId > 0, "New re-repair job should be created");

        // Original warranty status should now be CLAIMED
        Warranty updatedWarranty = WarrantyDB.getWarrantyById(warranty.getWarrantyId());
        assertEquals(WarrantyStatus.CLAIMED, updatedWarranty.getStatus());
        assertFalse(updatedWarranty.isCurrentlyValid());

        // Verify the newly created repair job
        RepairJob newJob = RepairDB.getRepairJobById(newJobId);
        assertNotNull(newJob);
        assertEquals(origJobId, newJob.getOriginalJobId(), "New job must be linked to original job ID (§7.7, §8.15)");
        assertEquals(RepairStatus.RECEIVED, newJob.getCurrentStatus());
        assertEquals("APPROVED", newJob.getApprovalStatus(), "Warranty re-repairs are pre-approved covered work");
        assertTrue(newJob.getInitialProblem().contains("Warranty Claim for Job #" + origJobId));
        assertTrue(newJob.getInitialProblem().contains(defect));
    }

    @Test
    void testRejectClaimOnAlreadyClaimedWarranty() throws Exception {
        int jobId = createCollectedRepairJob();
        Warranty warranty = WarrantyDB.getWarrantyByJobId(jobId);

        WarrantyDB.claimWarranty(warranty.getWarrantyId(), "First claim", "Receptionist");

        // Second claim on the same warranty must be rejected
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                WarrantyDB.claimWarranty(warranty.getWarrantyId(), "Second claim", "Receptionist")
        );
        assertTrue(ex.getMessage().contains("ineligible for claim"));
    }

    @Test
    void testRejectClaimOnExpiredWarranty() throws Exception {
        int jobId = createCollectedRepairJob();
        Warranty warranty = WarrantyDB.getWarrantyByJobId(jobId);

        // Manually set expiry date in past
        String pastDate = LocalDate.now().minusDays(5).toString();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE warranties SET expiry_date = ? WHERE warranty_id = ?")) {
            ps.setString(1, pastDate);
            ps.setInt(2, warranty.getWarrantyId());
            ps.executeUpdate();
        }

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                WarrantyDB.claimWarranty(warranty.getWarrantyId(), "Expired claim", "Receptionist")
        );
        assertTrue(ex.getMessage().contains("ineligible for claim"));
    }

    @Test
    void testSearchWarranties() throws Exception {
        int jobId = createCollectedRepairJob();

        List<Warranty> activeList = WarrantyDB.searchWarranties("Michael Scott", "ACTIVE");
        assertEquals(1, activeList.size());
        assertEquals(jobId, activeList.get(0).getJobId());

        List<Warranty> claimedList = WarrantyDB.searchWarranties(null, "CLAIMED");
        assertTrue(claimedList.isEmpty());
    }
}

