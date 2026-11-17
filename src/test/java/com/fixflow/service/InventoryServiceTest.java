package com.fixflow.service;

import com.fixflow.exception.InsufficientStockException;
import com.fixflow.model.SparePart;
import com.fixflow.repository.InventoryRepository;
import com.fixflow.repository.PartUsageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InventoryServiceTest {

    private File tempDir;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = File.createTempFile("fixflow_invtest_", "_dir");
        tempDir.delete();
        tempDir.mkdirs();

        InventoryRepository ir = new InventoryRepository(new File(tempDir, "spare_parts.json").getAbsolutePath());
        PartUsageRepository ur = new PartUsageRepository(new File(tempDir, "part_usage.json").getAbsolutePath());
        InventoryService.setRepositories(ir, ur);
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
    @DisplayName("Verify adding spare part and retrieving low stock items")
    void testAddPartAndLowStock() {
        SparePart p1 = new SparePart("iPhone Battery", "Battery", "iPhone 13", 3000.0, 6000.0, 2, 5);
        int id1 = InventoryService.addPart(p1);
        assertTrue(id1 > 0);

        SparePart p2 = new SparePart("Samsung Screen", "Display", "Galaxy S22", 10000.0, 18000.0, 10, 3);
        int id2 = InventoryService.addPart(p2);
        assertTrue(id2 > 0);

        List<SparePart> lowStock = InventoryService.getLowStockParts();
        assertEquals(1, lowStock.size());
        assertEquals("iPhone Battery", lowStock.get(0).getPartName());
    }

    @Test
    @DisplayName("Verify InsufficientStockException is thrown when issuing more than available")
    void testInsufficientStockExceptionThrown() {
        SparePart part = new SparePart("Thermal Paste", "Consumable", "Universal", 500.0, 1000.0, 3, 1);
        int partId = InventoryService.addPart(part);

        // Attempting to issue 5 when only 3 are available
        InsufficientStockException ex = assertThrows(InsufficientStockException.class, () ->
                InventoryService.issuePartToJob(101, partId, 5, "Technician")
        );

        assertEquals(partId, ex.getPartId());
        assertEquals(3, ex.getAvailableQuantity());
        assertEquals(5, ex.getRequestedQuantity());

        // Verify stock was not deducted on exception
        SparePart unchanged = InventoryService.getPartById(partId);
        assertEquals(3, unchanged.getAvailableQuantity());
    }

    @Test
    @DisplayName("Verify successful part issuance deducts stock")
    void testSuccessfulPartIssuance() throws InsufficientStockException {
        SparePart part = new SparePart("USB-C Port", "Charging", "Universal", 400.0, 1200.0, 5, 2);
        int partId = InventoryService.addPart(part);

        InventoryService.issuePartToJob(202, partId, 2, "Technician");

        SparePart updated = InventoryService.getPartById(partId);
        assertEquals(3, updated.getAvailableQuantity());
        assertEquals(1, InventoryService.getPartsUsedForJob(202).size());
    }

    @Test
    @DisplayName("Verify adding stock replenishes inventory quantity")
    void testAddStock() {
        SparePart part = new SparePart("Screws Pack", "Hardware", "Universal", 100.0, 300.0, 5, 2);
        int partId = InventoryService.addPart(part);

        InventoryService.addStock(partId, 15);
        SparePart updated = InventoryService.getPartById(partId);
        assertEquals(20, updated.getAvailableQuantity());
    }
}
