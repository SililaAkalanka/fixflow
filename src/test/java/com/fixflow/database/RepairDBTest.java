package com.fixflow.database;

import com.fixflow.enums.RepairStatus;
import com.fixflow.exception.InvalidStatusTransitionException;
import com.fixflow.model.Customer;
import com.fixflow.model.Device;
import com.fixflow.model.RepairJob;
import com.fixflow.model.StatusHistory;
import com.fixflow.model.Technician;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RepairDBTest {

    private Connection sharedMemoryConnection;
    private int customerId;
    private int deviceId;
    private int activeTechId;
    private int inactiveTechId;

    @BeforeEach
    void setUp() throws SQLException {
        String testUrl = "jdbc:sqlite:file:repairdb_test?mode=memory&cache=shared";
        DatabaseManager.setCustomJdbcUrl(testUrl);

        sharedMemoryConnection = DatabaseManager.getConnection();
        DatabaseManager.initializeDatabase();

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
    void tearDown() throws SQLException {
        if (sharedMemoryConnection != null && !sharedMemoryConnection.isClosed()) {
            sharedMemoryConnection.close();
        }
        DatabaseManager.setCustomJdbcUrl(null);
    }

    @Test
    @DisplayName("Verify repair job creation sets status to RECEIVED and logs initial audit history")
    void testCreateRepairJob() throws SQLException {
        RepairJob job = new RepairJob(customerId, deviceId, "Screen shattered upon dropping.");
        int jobId = RepairDB.createRepairJob(job, "Receptionist");
        assertTrue(jobId > 0);

        RepairJob retrieved = RepairDB.getRepairJobById(jobId);
        assertNotNull(retrieved);
        assertEquals(RepairStatus.RECEIVED, retrieved.getCurrentStatus());
        assertEquals("PENDING", retrieved.getApprovalStatus());
        assertEquals("Emily Watson", retrieved.getCustomerName());
        assertEquals("Apple iPhone 13 (Smartphone)", retrieved.getDeviceSummary());

        // Verify status history audit
        List<StatusHistory> history = RepairDB.getStatusHistory(jobId);
        assertEquals(1, history.size());
        assertNull(history.get(0).getFromStatus());
        assertEquals(RepairStatus.RECEIVED, history.get(0).getToStatus());
        assertEquals("Receptionist", history.get(0).getChangedByRole());
    }

    @Test
    @DisplayName("Verify technician assignment rejects inactive technician")
    void testTechnicianAssignmentInactiveRejection() throws SQLException {
        RepairJob job = new RepairJob(customerId, deviceId, "Battery swelling");
        int jobId = RepairDB.createRepairJob(job, "Receptionist");

        // Attempt to assign inactive technician must throw IllegalArgumentException
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> RepairDB.assignTechnician(jobId, inactiveTechId, "Manager")
        );
        assertTrue(ex.getMessage().contains("Inactive"));

        // Assign active technician must succeed
        boolean assigned = RepairDB.assignTechnician(jobId, activeTechId, "Manager");
        assertTrue(assigned);

        RepairJob updatedJob = RepairDB.getRepairJobById(jobId);
        assertEquals(Integer.valueOf(activeTechId), updatedJob.getTechnicianId());
        assertEquals("David Miller", updatedJob.getTechnicianName());
    }

    @Test
    @DisplayName("Verify diagnosis recording and advancing to WAITING_FOR_APPROVAL")
    void testRecordDiagnosis() throws Exception {
        RepairJob job = new RepairJob(customerId, deviceId, "Phone won't charge");
        int jobId = RepairDB.createRepairJob(job, "Receptionist");
        RepairDB.assignTechnician(jobId, activeTechId, "Receptionist");

        RepairDB.recordDiagnosis(jobId, "Charging port full of debris and pins bent. Replacement needed.",
                65.0, 30.0, "2026-11-15", true, "Technician");

        RepairJob diagnosed = RepairDB.getRepairJobById(jobId);
        assertEquals(RepairStatus.WAITING_FOR_APPROVAL, diagnosed.getCurrentStatus());
        assertEquals(Double.valueOf(65.0), diagnosed.getEstimatedCost());
        assertEquals(30.0, diagnosed.getLabourCharges());
        assertEquals("2026-11-15", diagnosed.getEstimatedCompletionDate());
        assertTrue(diagnosed.getDiagnosisFindings().contains("Charging port"));
    }

    @Test
    @DisplayName("Verify customer approval lifecycle: Approval advances to REPAIRING, Rejection moves to CANCELLED")
    void testCustomerApprovalWorkflows() throws Exception {
        // Workflow A: Rejection
        RepairJob jobA = new RepairJob(customerId, deviceId, "Broken camera lens");
        int jobAId = RepairDB.createRepairJob(jobA, "Receptionist");
        RepairDB.recordDiagnosis(jobAId, "Replace camera assembly", 120.0, 40.0, "2026-11-18", true, "Technician");

        RepairDB.recordCustomerApproval(jobAId, false, "Customer feels cost exceeds device value.", "Receptionist");
        RepairJob rejectedJob = RepairDB.getRepairJobById(jobAId);
        assertEquals(RepairStatus.CANCELLED, rejectedJob.getCurrentStatus());
        assertEquals("REJECTED", rejectedJob.getApprovalStatus());
        assertEquals("Customer feels cost exceeds device value.", rejectedJob.getCancellationReason());

        // Workflow B: Approval
        RepairJob jobB = new RepairJob(customerId, deviceId, "Cracked display");
        int jobBId = RepairDB.createRepairJob(jobB, "Receptionist");
        RepairDB.recordDiagnosis(jobBId, "OLED panel replacement", 140.0, 50.0, "2026-11-14", true, "Technician");

        RepairDB.recordCustomerApproval(jobBId, true, null, "Receptionist");
        RepairJob approvedJob = RepairDB.getRepairJobById(jobBId);
        assertEquals(RepairStatus.REPAIRING, approvedJob.getCurrentStatus());
        assertEquals("APPROVED", approvedJob.getApprovalStatus());
        assertNotNull(approvedJob.getApprovalDate());
    }

    @Test
    @DisplayName("Verify waiting for parts loop and transition to READY_FOR_COLLECTION")
    void testWaitingForPartsAndCompletion() throws Exception {
        RepairJob job = new RepairJob(customerId, deviceId, "Touchscreen not responding");
        int jobId = RepairDB.createRepairJob(job, "Receptionist");
        RepairDB.recordDiagnosis(jobId, "Screen digitizer replacement", 95.0, 35.0, "2026-11-14", true, "Technician");
        RepairDB.recordCustomerApproval(jobId, true, null, "Receptionist");

        // REPAIRING -> WAITING_FOR_PARTS
        RepairDB.updateStatus(jobId, RepairStatus.WAITING_FOR_PARTS, "OEM display currently out of stock. Supplier shipment ordered.", "Technician");
        assertEquals(RepairStatus.WAITING_FOR_PARTS, RepairDB.getRepairJobById(jobId).getCurrentStatus());

        // WAITING_FOR_PARTS -> REPAIRING
        RepairDB.updateStatus(jobId, RepairStatus.REPAIRING, "Parts arrived. Resuming assembly.", "Technician");
        assertEquals(RepairStatus.REPAIRING, RepairDB.getRepairJobById(jobId).getCurrentStatus());

        // REPAIRING -> READY_FOR_COLLECTION
        RepairDB.updateStatus(jobId, RepairStatus.READY_FOR_COLLECTION, "Screen replaced and tested. Ready for customer pickup.", "Technician");
        RepairJob completedJob = RepairDB.getRepairJobById(jobId);
        assertEquals(RepairStatus.READY_FOR_COLLECTION, completedJob.getCurrentStatus());
        assertNotNull(completedJob.getActualCompletionDate());

        // Verify complete audit history
        List<StatusHistory> history = RepairDB.getStatusHistory(jobId);
        assertTrue(history.size() >= 5);
    }

    @Test
    @DisplayName("Verify repair job deletion blocked if financial payment records exist")
    void testDeleteRepairJobBlockedByPayment() throws Exception {
        RepairJob job = new RepairJob(customerId, deviceId, "Mic repair");
        int jobId = RepairDB.createRepairJob(job, "Receptionist");

        // Insert payment record referencing this job
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO payments (job_id, payment_date, total_repair_cost, amount_paid, payment_method, payment_status, created_at) " +
                             "VALUES (?, datetime('now'), 50.0, 50.0, 'CASH', 'PAID', datetime('now'))")) {
            ps.setInt(1, jobId);
            ps.executeUpdate();
        }

        SQLException ex = assertThrows(SQLException.class, () -> RepairDB.deleteRepairJob(jobId));
        assertTrue(ex.getMessage().contains("Financial payment records are associated"));
    }
}

