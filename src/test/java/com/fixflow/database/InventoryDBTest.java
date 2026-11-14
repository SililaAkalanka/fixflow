package com.fixflow.database;

import com.fixflow.enums.RepairStatus;
import com.fixflow.exception.InsufficientStockException;
import com.fixflow.model.Customer;
import com.fixflow.model.Device;
import com.fixflow.model.RepairJob;
import com.fixflow.model.RepairPartUsage;
import com.fixflow.model.SparePart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InventoryDBTest {

    private File tempDbFile;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = File.createTempFile("fixflow_inv_test_", ".db");
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

    private int createSampleRepairJob() throws SQLException {
        Customer c = new Customer("John Doe", "0771234567", "john@example.com", "123 Main St");
        int custId = CustomerDB.createCustomer(c);
        Device d = new Device(custId, "Laptop", "Dell", "XPS 15", "SN123456", "Silver", "Charger");
        int devId = DeviceDB.createDevice(d);
        RepairJob job = new RepairJob(custId, devId, "Screen flickering");
        return RepairDB.createRepairJob(job);
    }

    @Test
    void testCreateAndGetPart() throws SQLException {
        SparePart part = new SparePart("iPhone 13 OLED Screen", "Screens", "iPhone 13", 55.0, 95.0, 10, 3);
        int partId = InventoryDB.createPart(part);
        assertTrue(partId > 0, "Part ID should be positive");

        SparePart retrieved = InventoryDB.getPartById(partId);
        assertNotNull(retrieved);
        assertEquals("iPhone 13 OLED Screen", retrieved.getPartName());
        assertEquals("Screens", retrieved.getCategory());
        assertEquals("iPhone 13", retrieved.getCompatibleDeviceModel());
        assertEquals(55.0, retrieved.getPurchaseCost(), 0.001);
        assertEquals(95.0, retrieved.getSellingPrice(), 0.001);
        assertEquals(10, retrieved.getAvailableQuantity());
        assertEquals(3, retrieved.getMinStockLevel());
        assertFalse(retrieved.isLowStock());
    }

    @Test
    void testUpdatePart() throws SQLException {
        SparePart part = new SparePart("Original Battery", "Batteries", "Galaxy S21", 20.0, 45.0, 8, 2);
        int partId = InventoryDB.createPart(part);

        part.setCategory("Power Components");
        part.setSellingPrice(50.0);
        part.setMinStockLevel(4);

        boolean updated = InventoryDB.updatePart(part);
        assertTrue(updated);

        SparePart retrieved = InventoryDB.getPartById(partId);
        assertEquals("Power Components", retrieved.getCategory());
        assertEquals(50.0, retrieved.getSellingPrice(), 0.001);
        assertEquals(4, retrieved.getMinStockLevel());
    }

    @Test
    void testAddStock() throws SQLException {
        SparePart part = new SparePart("Thermal Paste", "Supplies", "Universal", 5.0, 12.0, 5, 2);
        int partId = InventoryDB.createPart(part);

        boolean ok = InventoryDB.addStock(partId, 15);
        assertTrue(ok);

        SparePart retrieved = InventoryDB.getPartById(partId);
        assertEquals(20, retrieved.getAvailableQuantity(), "Available quantity should be 5 + 15 = 20");

        assertThrows(IllegalArgumentException.class, () -> InventoryDB.addStock(partId, 0));
        assertThrows(IllegalArgumentException.class, () -> InventoryDB.addStock(partId, -5));
    }

    @Test
    void testLowStockAlerts() throws SQLException {
        SparePart p1 = new SparePart("Charging Port Type-C", "Ports", "Universal", 4.0, 15.0, 2, 5); // low stock
        SparePart p2 = new SparePart("RAM 16GB DDR4", "Memory", "Universal", 30.0, 55.0, 10, 2); // normal stock
        InventoryDB.createPart(p1);
        InventoryDB.createPart(p2);

        assertTrue(p1.isLowStock(), "Part with qty 2 and min 5 should be low stock");
        assertFalse(p2.isLowStock(), "Part with qty 10 and min 2 should not be low stock");

        List<SparePart> lowStock = InventoryDB.getLowStockParts();
        assertEquals(1, lowStock.size());
        assertEquals("Charging Port Type-C", lowStock.get(0).getPartName());
    }

    @Test
    void testSearchAndCategoryFilter() throws SQLException {
        InventoryDB.createPart(new SparePart("MacBook Trackpad", "Trackpads", "MacBook Air", 40.0, 80.0, 5, 1));
        InventoryDB.createPart(new SparePart("MacBook Keyboard", "Keyboards", "MacBook Pro", 45.0, 90.0, 4, 1));
        InventoryDB.createPart(new SparePart("Dell Inspiron Fan", "Cooling", "Dell Inspiron", 15.0, 30.0, 1, 2)); // low stock

        List<SparePart> macResults = InventoryDB.searchParts("MacBook", null, false);
        assertEquals(2, macResults.size());

        List<SparePart> coolingResults = InventoryDB.searchParts(null, "Cooling", false);
        assertEquals(1, coolingResults.size());
        assertEquals("Dell Inspiron Fan", coolingResults.get(0).getPartName());

        List<SparePart> lowStockOnly = InventoryDB.searchParts(null, null, true);
        assertEquals(1, lowStockOnly.size());
        assertEquals("Dell Inspiron Fan", lowStockOnly.get(0).getPartName());
    }

    @Test
    void testIssuePartToJobAndFrozenPriceSnapshot() throws SQLException, InsufficientStockException {
        int jobId = createSampleRepairJob();

        SparePart part = new SparePart("iPad Screen Assembly", "Screens", "iPad Air 4", 80.0, 140.0, 5, 1);
        int partId = InventoryDB.createPart(part);

        // Issue 2 units to the repair job
        InventoryDB.issuePartToJob(jobId, partId, 2, "Technician");

        // Stock in catalog should be reduced by 2
        SparePart updatedPart = InventoryDB.getPartById(partId);
        assertEquals(3, updatedPart.getAvailableQuantity(), "Stock should be reduced from 5 to 3");

        // Verify parts used for job
        List<RepairPartUsage> usages = InventoryDB.getPartsUsedForJob(jobId);
        assertEquals(1, usages.size());
        RepairPartUsage usage = usages.get(0);
        assertEquals(partId, usage.getPartId());
        assertEquals(2, usage.getQuantity());
        assertEquals(140.0, usage.getSellingPriceAtTime(), 0.001);
        assertEquals(280.0, usage.getSubtotal(), 0.001);

        // Verify total cost calculation
        assertEquals(280.0, InventoryDB.getTotalPartsCostForJob(jobId), 0.001);

        // Update catalog selling price to $160.0
        part.setSellingPrice(160.0);
        InventoryDB.updatePart(part);

        // Job usage frozen price snapshot must remain $140.0 (§7.6)
        List<RepairPartUsage> usagesAfterPriceChange = InventoryDB.getPartsUsedForJob(jobId);
        assertEquals(140.0, usagesAfterPriceChange.get(0).getSellingPriceAtTime(), 0.001);
        assertEquals(280.0, InventoryDB.getTotalPartsCostForJob(jobId), 0.001);
    }

    @Test
    void testInsufficientStockThrowsExceptionAndRollsBack() throws SQLException {
        int jobId = createSampleRepairJob();

        SparePart part = new SparePart("Rare CPU Chip", "Chips", "Specialized", 200.0, 350.0, 1, 1);
        int partId = InventoryDB.createPart(part);

        // Attempt to issue 2 units when only 1 is available
        InsufficientStockException ex = assertThrows(InsufficientStockException.class, () ->
                InventoryDB.issuePartToJob(jobId, partId, 2, "Technician")
        );
        assertTrue(ex.getMessage().contains("Insufficient stock"));

        // Verify stock is unchanged and no usage was inserted
        SparePart partAfter = InventoryDB.getPartById(partId);
        assertEquals(1, partAfter.getAvailableQuantity(), "Stock should still be 1");

        List<RepairPartUsage> usages = InventoryDB.getPartsUsedForJob(jobId);
        assertTrue(usages.isEmpty(), "No usage record should have been committed");
    }

    @Test
    void testRemovePartUsageRestoresStock() throws SQLException, InsufficientStockException {
        int jobId = createSampleRepairJob();

        SparePart part = new SparePart("Thermal Pad 1mm", "Supplies", "Universal", 2.0, 6.0, 10, 2);
        int partId = InventoryDB.createPart(part);

        InventoryDB.issuePartToJob(jobId, partId, 3, "Technician");
        assertEquals(7, InventoryDB.getPartById(partId).getAvailableQuantity());

        List<RepairPartUsage> usages = InventoryDB.getPartsUsedForJob(jobId);
        assertEquals(1, usages.size());
        int usageId = usages.get(0).getUsageId();

        boolean removed = InventoryDB.removePartUsage(usageId);
        assertTrue(removed);

        // Verify stock is restored to 10
        assertEquals(10, InventoryDB.getPartById(partId).getAvailableQuantity(), "Stock should be restored to 10");

        // Verify usage records are empty
        assertTrue(InventoryDB.getPartsUsedForJob(jobId).isEmpty());
    }

    @Test
    void testDeletePartSafeguard() throws SQLException, InsufficientStockException {
        int jobId = createSampleRepairJob();

        SparePart partUsed = new SparePart("Part A", "Cat A", "Model A", 10.0, 20.0, 5, 1);
        int partIdUsed = InventoryDB.createPart(partUsed);
        InventoryDB.issuePartToJob(jobId, partIdUsed, 1, "Technician");

        // Deleting part with active usage should fail
        assertThrows(SQLException.class, () -> InventoryDB.deletePart(partIdUsed));
        assertNotNull(InventoryDB.getPartById(partIdUsed), "Part with usage records must not be deleted");

        // Deleting unused part should succeed
        SparePart partUnused = new SparePart("Part B", "Cat B", "Model B", 10.0, 20.0, 5, 1);
        int partIdUnused = InventoryDB.createPart(partUnused);
        boolean deleted = InventoryDB.deletePart(partIdUnused);
        assertTrue(deleted);
        assertNull(InventoryDB.getPartById(partIdUnused));
    }
}
