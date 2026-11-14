package com.fixflow.database;

import com.fixflow.enums.RepairStatus;
import com.fixflow.model.Customer;
import com.fixflow.model.Device;
import com.fixflow.model.RepairJob;
import com.fixflow.model.Technician;
import com.fixflow.service.RepairService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class RepairDBTest {

    private File tempDbFile;
    private int customerId;
    private int deviceId;
    private int activeTechId;
    private int inactiveTechId;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = File.createTempFile("fixflow_repair_test_", ".db");
        DatabaseConnection.setCustomJdbcUrl("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
        DatabaseInitializer.initializeDatabase();

        Customer customer = new Customer("Emily Watson", "0771239999", "emily@example.com", "22 Baker St");
        customerId = CustomerDB.createCustomer(customer);

        Device device = new Device(customerId, "Smartphone", "Apple", "iPhone 13", "SN-998877", "Midnight", "USB-C Cable");
        deviceId = DeviceDB.createDevice(device);

        Technician activeTech = new Technician("David Miller", "0771112233", "Screens & Batteries", true);
        activeTechId = TechnicianDB.createTechnician(activeTech);

        Technician inactiveTech = new Technician("Retired Tech", "0770009988", "Soldering", false);
        inactiveTechId = TechnicianDB.createTechnician(inactiveTech);
    }

    @AfterEach
    void tearDown() {
        DatabaseConnection.setCustomJdbcUrl(null);
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    @Test
    @DisplayName("Verify repair job creation sets status to RECEIVED")
    void testCreateRepairJob() throws SQLException {
        RepairJob job = new RepairJob(customerId, deviceId, "Screen shattered upon dropping.");
        int jobId = RepairDB.createRepairJob(job);
        assertTrue(jobId > 0);

        RepairJob retrieved = RepairDB.getRepairJobById(jobId);
        assertNotNull(retrieved);
        assertEquals(RepairStatus.RECEIVED, retrieved.getCurrentStatus());
        assertEquals("Emily Watson", retrieved.getCustomerName());
        assertEquals("Apple iPhone 13 (Smartphone)", retrieved.getDeviceSummary());
    }

    @Test
    @DisplayName("Verify technician assignment rejects inactive technician")
    void testTechnicianAssignmentInactiveRejection() throws SQLException {
        RepairJob job = new RepairJob(customerId, deviceId, "Battery swelling");
        int jobId = RepairDB.createRepairJob(job);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> RepairDB.assignTechnician(jobId, inactiveTechId)
        );
        assertTrue(ex.getMessage().contains("Inactive"));

        boolean assigned = RepairDB.assignTechnician(jobId, activeTechId);
        assertTrue(assigned);

        RepairJob updatedJob = RepairDB.getRepairJobById(jobId);
        assertEquals(Integer.valueOf(activeTechId), updatedJob.getTechnicianId());
        assertEquals("David Miller", updatedJob.getTechnicianName());
    }

    @Test
    @DisplayName("Verify diagnosis recording")
    void testRecordDiagnosis() throws Exception {
        RepairJob job = new RepairJob(customerId, deviceId, "Phone won't charge");
        int jobId = RepairDB.createRepairJob(job);
        RepairDB.assignTechnician(jobId, activeTechId);

        RepairDB.recordDiagnosis(jobId, "Charging port full of debris and pins bent. Replacement needed.",
                6500.0, 3000.0, "2026-11-15");

        RepairJob diagnosed = RepairDB.getRepairJobById(jobId);
        assertEquals(Double.valueOf(6500.0), diagnosed.getEstimatedCost());
        assertEquals(3000.0, diagnosed.getLabourCharges());
        assertEquals("2026-11-15", diagnosed.getEstimatedCompletionDate());
        assertTrue(diagnosed.getDiagnosisFindings().contains("Charging port"));
    }

    @Test
    @DisplayName("Verify 6-status lifecycle progression: RECEIVED -> DIAGNOSING -> IN_PROGRESS -> COMPLETED")
    void testLifecycleProgression() throws Exception {
        RepairJob job = new RepairJob(customerId, deviceId, "Touchscreen not responding");
        int jobId = RepairDB.createRepairJob(job);
        assertEquals(RepairStatus.RECEIVED, RepairDB.getRepairJobById(jobId).getCurrentStatus());

        RepairDB.updateStatus(jobId, RepairStatus.DIAGNOSING, "Diagnosis initiated");
        assertEquals(RepairStatus.DIAGNOSING, RepairDB.getRepairJobById(jobId).getCurrentStatus());

        RepairDB.updateStatus(jobId, RepairStatus.IN_PROGRESS, "Working on screen");
        assertEquals(RepairStatus.IN_PROGRESS, RepairDB.getRepairJobById(jobId).getCurrentStatus());

        RepairDB.updateStatus(jobId, RepairStatus.COMPLETED, "Repair completed and tested");
        RepairJob completed = RepairDB.getRepairJobById(jobId);
        assertEquals(RepairStatus.COMPLETED, completed.getCurrentStatus());
        assertNotNull(completed.getActualCompletionDate());
    }

    @Test
    @DisplayName("Verify repair job deletion blocked if financial payment records exist")
    void testDeleteRepairJobBlockedByPayment() throws Exception {
        RepairJob job = new RepairJob(customerId, deviceId, "Mic repair");
        int jobId = RepairDB.createRepairJob(job);

        // Insert payment record referencing this job
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO payments (job_id, payment_date, amount_paid, payment_method, created_at) " +
                             "VALUES (?, datetime('now'), 500.0, 'CASH', datetime('now'))")) {
            ps.setInt(1, jobId);
            ps.executeUpdate();
        }

        SQLException ex = assertThrows(SQLException.class, () -> RepairDB.deleteRepairJob(jobId));
        assertTrue(ex.getMessage().contains("Payment records exist"));
    }
}
