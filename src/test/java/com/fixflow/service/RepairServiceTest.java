package com.fixflow.service;

import com.fixflow.enums.RepairStatus;
import com.fixflow.exception.InvalidStatusTransitionException;
import com.fixflow.model.Customer;
import com.fixflow.model.Device;
import com.fixflow.model.RepairJob;
import com.fixflow.model.Technician;
import com.fixflow.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class RepairServiceTest {

    private File tempDir;
    private int customerId;
    private int deviceId;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = File.createTempFile("fixflow_repairsvc_test_", "_dir");
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

        Customer c = new Customer("Bob Vance", "0771234567", "bob@vance.com", "Scranton");
        customerId = CustomerService.createCustomer(c);
        Device d = new Device(customerId, "Laptop", "Lenovo", "ThinkPad", "SN-TP-1", "Black", "Charger");
        deviceId = DeviceService.createDevice(d);
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

    @Test
    @DisplayName("Verify valid full repair lifecycle from RECEIVED to COLLECTED")
    void testValidLifecycle() throws Exception {
        RepairJob job = new RepairJob(customerId, deviceId, "Fan making noise");
        job.setLabourCharges(2500.0);
        int jobId = RepairService.createRepairJob(job);
        assertEquals(RepairStatus.RECEIVED, RepairService.getRepairJobById(jobId).getCurrentStatus());

        RepairService.updateStatus(jobId, RepairStatus.DIAGNOSING, "Inspecting fan bearings");
        assertEquals(RepairStatus.DIAGNOSING, RepairService.getRepairJobById(jobId).getCurrentStatus());

        RepairService.updateStatus(jobId, RepairStatus.IN_PROGRESS, "Replacing fan assembly");
        assertEquals(RepairStatus.IN_PROGRESS, RepairService.getRepairJobById(jobId).getCurrentStatus());

        RepairService.updateStatus(jobId, RepairStatus.COMPLETED, "Repair complete and tested");
        assertEquals(RepairStatus.COMPLETED, RepairService.getRepairJobById(jobId).getCurrentStatus());

        // Paying in full before collection
        PaymentService.recordPayment(jobId, 2500.0, null, "Cash payment");

        RepairService.updateStatus(jobId, RepairStatus.COLLECTED, "Collected by customer");
        assertEquals(RepairStatus.COLLECTED, RepairService.getRepairJobById(jobId).getCurrentStatus());
    }

    @Test
    @DisplayName("Verify collection is strictly prevented if outstanding balance exists")
    void testCollectionBlockedWhenUnpaid() throws Exception {
        RepairJob job = new RepairJob(customerId, deviceId, "Keyboard replacement");
        job.setLabourCharges(3000.0);
        int jobId = RepairService.createRepairJob(job);

        RepairService.updateStatus(jobId, RepairStatus.DIAGNOSING, "Diagnosis");
        RepairService.updateStatus(jobId, RepairStatus.IN_PROGRESS, "Repairing");
        RepairService.updateStatus(jobId, RepairStatus.COMPLETED, "Done");

        // Attempt to collect without payment must throw IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                RepairService.updateStatus(jobId, RepairStatus.COLLECTED, "Attempt collection")
        );
        assertTrue(ex.getMessage().contains("outstanding balance"));

        // Partial payment (e.g. 1000 out of 3000) should still prevent collection
        PaymentService.recordPayment(jobId, 1000.0, null, "Partial deposit");
        assertThrows(IllegalStateException.class, () ->
                RepairService.updateStatus(jobId, RepairStatus.COLLECTED, "Attempt collection")
        );

        // Pay remaining 2000
        PaymentService.recordPayment(jobId, 2000.0, null, "Final payment");

        // Now collection must succeed
        RepairService.updateStatus(jobId, RepairStatus.COLLECTED, "Collected");
        assertEquals(RepairStatus.COLLECTED, RepairService.getRepairJobById(jobId).getCurrentStatus());
    }

    @Test
    @DisplayName("Verify cancellation requires non-empty reason")
    void testCancellationReasonRequired() {
        RepairJob job = new RepairJob(customerId, deviceId, "Screen issue");
        int jobId = RepairService.createRepairJob(job);

        assertThrows(IllegalArgumentException.class, () ->
                RepairService.updateStatus(jobId, RepairStatus.CANCELLED, null)
        );
        assertThrows(IllegalArgumentException.class, () ->
                RepairService.updateStatus(jobId, RepairStatus.CANCELLED, "   ")
        );

        // Valid cancellation with reason
        assertDoesNotThrow(() ->
                RepairService.updateStatus(jobId, RepairStatus.CANCELLED, "Customer declined repair estimate")
        );
        assertEquals(RepairStatus.CANCELLED, RepairService.getRepairJobById(jobId).getCurrentStatus());
    }

    @Test
    @DisplayName("Verify technician assignment requires active technician")
    void testTechnicianAssignmentRules() {
        Technician activeTech = new Technician("Active Tech", "0711111111", "Hardware", true);
        int activeId = TechnicianService.addTechnician(activeTech);

        Technician inactiveTech = new Technician("Inactive Tech", "0722222222", "Software", false);
        int inactiveId = TechnicianService.addTechnician(inactiveTech);

        RepairJob job = new RepairJob(customerId, deviceId, "Power button stuck");
        int jobId = RepairService.createRepairJob(job);

        // Inactive technician cannot be assigned
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                RepairService.assignTechnician(jobId, inactiveId)
        );
        assertTrue(ex.getMessage().contains("Inactive"));

        // Active technician assignment succeeds
        assertTrue(RepairService.assignTechnician(jobId, activeId));
        assertEquals(activeId, RepairService.getRepairJobById(jobId).getTechnicianId());
    }
}
