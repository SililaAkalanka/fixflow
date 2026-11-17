package com.fixflow.repository;

import com.fixflow.enums.RepairStatus;
import com.fixflow.model.Customer;
import com.fixflow.model.Device;
import com.fixflow.model.RepairJob;
import com.fixflow.model.SparePart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JsonPersistenceTest {

    private File tempDir;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = File.createTempFile("fixflow_perstest_", "_dir");
        tempDir.delete();
        tempDir.mkdirs();
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
    @DisplayName("Verify Customer persistence survives repository re-instantiation (restart simulation)")
    void testCustomerPersistenceSurvivesRestart() {
        String jsonPath = new File(tempDir, "customers.json").getAbsolutePath();

        // 1. Initial repository instance adds customer
        CustomerRepository repo1 = new CustomerRepository(jsonPath);
        Customer c1 = new Customer(1, "Kamal Gunaratne", "0771234567", "kamal@test.lk", "Colombo 07", "2026-11-17 12:00:00");
        repo1.add(c1);

        assertTrue(new File(jsonPath).exists(), "JSON data file must be created on disk");

        // 2. Simulate application restart: instantiate a brand new repository pointing to the same file
        CustomerRepository repo2 = new CustomerRepository(jsonPath);
        List<Customer> loaded = repo2.findAll();

        assertEquals(1, loaded.size());
        assertEquals("Kamal Gunaratne", loaded.get(0).getFullName());
        assertEquals("0771234567", loaded.get(0).getPhone());
    }

    @Test
    @DisplayName("Verify RepairJob and SparePart persistence survives restart")
    void testRepairAndInventoryPersistenceSurvivesRestart() {
        String partPath = new File(tempDir, "spare_parts.json").getAbsolutePath();
        String jobPath = new File(tempDir, "repair_jobs.json").getAbsolutePath();

        InventoryRepository inv1 = new InventoryRepository(partPath);
        SparePart part = new SparePart(1, "OLED Display", "Screen", "Galaxy S22", 10000.0, 16000.0, 5, 2, "2026-11-17 10:00:00", "2026-11-17 10:00:00");
        inv1.add(part);

        RepairRepository rep1 = new RepairRepository(jobPath);
        RepairJob job = new RepairJob(1, 1, "Touch screen dead");
        job.setJobId(1);
        job.setCurrentStatus(RepairStatus.IN_PROGRESS);
        rep1.add(job);

        // Restart simulation
        InventoryRepository inv2 = new InventoryRepository(partPath);
        assertEquals(1, inv2.findAll().size());
        assertEquals("OLED Display", inv2.findById(1).getPartName());
        assertEquals(5, inv2.findById(1).getAvailableQuantity());

        RepairRepository rep2 = new RepairRepository(jobPath);
        assertEquals(1, rep2.findAll().size());
        assertEquals(RepairStatus.IN_PROGRESS, rep2.findById(1).getCurrentStatus());
    }
}
