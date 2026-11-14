package com.fixflow.service;

import com.fixflow.database.CustomerDB;
import com.fixflow.database.DatabaseConnection;
import com.fixflow.database.DatabaseInitializer;
import com.fixflow.database.DeviceDB;
import com.fixflow.database.RepairDB;
import com.fixflow.enums.RepairStatus;
import com.fixflow.exception.InvalidStatusTransitionException;
import com.fixflow.model.Customer;
import com.fixflow.model.Device;
import com.fixflow.model.RepairJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class RepairServiceTest {

    private File tempDbFile;
    private int customerId;
    private int deviceId;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = File.createTempFile("fixflow_repairsvc_test_", ".db");
        DatabaseConnection.setCustomJdbcUrl("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
        DatabaseInitializer.initializeDatabase();

        Customer c = new Customer("Bob Vance", "0771234567", "bob@vance.com", "Scranton");
        customerId = CustomerDB.createCustomer(c);
        Device d = new Device(customerId, "Laptop", "Lenovo", "ThinkPad", "SN-TP-1", "Black", "Charger");
        deviceId = DeviceDB.createDevice(d);
    }

    @AfterEach
    void tearDown() {
        DatabaseConnection.setCustomJdbcUrl(null);
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
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
        assertDoesNotThrow(() ->
                RepairService.updateStatus(jobId, RepairStatus.COLLECTED, "Picked up")
        );
        assertEquals(RepairStatus.COLLECTED, RepairService.getRepairJobById(jobId).getCurrentStatus());
    }

    @Test
    @DisplayName("Verify invalid lifecycle skip throws InvalidStatusTransitionException")
    void testInvalidStatusTransition() throws Exception {
        RepairJob job = new RepairJob(customerId, deviceId, "Broken screen");
        int jobId = RepairService.createRepairJob(job);

        // Cannot skip directly from RECEIVED to COMPLETED
        assertThrows(InvalidStatusTransitionException.class, () ->
                RepairService.updateStatus(jobId, RepairStatus.COMPLETED, "Skip")
        );
    }

    @Test
    @DisplayName("Verify cancellation requires a reason")
    void testCancellationRequiresReason() throws Exception {
        RepairJob job = new RepairJob(customerId, deviceId, "Water damage");
        int jobId = RepairService.createRepairJob(job);

        assertThrows(IllegalArgumentException.class, () ->
                RepairService.updateStatus(jobId, RepairStatus.CANCELLED, "")
        );
        assertThrows(IllegalArgumentException.class, () ->
                RepairService.updateStatus(jobId, RepairStatus.CANCELLED, null)
        );

        assertDoesNotThrow(() ->
                RepairService.updateStatus(jobId, RepairStatus.CANCELLED, "Customer decided not to repair")
        );
        assertEquals(RepairStatus.CANCELLED, RepairService.getRepairJobById(jobId).getCurrentStatus());
    }
}
