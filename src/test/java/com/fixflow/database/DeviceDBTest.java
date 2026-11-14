package com.fixflow.database;

import com.fixflow.model.Customer;
import com.fixflow.model.Device;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeviceDBTest {

    private File tempDbFile;
    private int testCustomerId;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = File.createTempFile("fixflow_dev_test_", ".db");
        DatabaseConnection.setCustomJdbcUrl("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
        DatabaseInitializer.initializeDatabase();

        Customer customer = new Customer("Charlie Brown", "0788889999", "charlie@test.com", "77 Elm St");
        testCustomerId = CustomerDB.createCustomer(customer);
    }

    @AfterEach
    void tearDown() {
        DatabaseConnection.setCustomJdbcUrl(null);
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    @Test
    @DisplayName("Verify device registration and retrieval with customer details joined")
    void testCreateAndGetDevice() throws SQLException {
        Device device = new Device(testCustomerId, "Smartphone", "Samsung", "Galaxy S23", "SN-987654", "Phantom Black", "Original Charger");
        int devId = DeviceDB.createDevice(device);
        assertTrue(devId > 0);

        Device retrieved = DeviceDB.getDeviceById(devId);
        assertNotNull(retrieved);
        assertEquals("Samsung", retrieved.getBrand());
        assertEquals("Galaxy S23", retrieved.getModel());
        assertEquals("Smartphone", retrieved.getCategory());
        assertEquals("SN-987654", retrieved.getSerialNumber());
        assertEquals("Phantom Black", retrieved.getColour());
        assertEquals("Original Charger", retrieved.getAccessories());

        // Verify joined customer fields
        assertEquals("Charlie Brown", retrieved.getCustomerName());
        assertEquals("0788889999", retrieved.getCustomerPhone());
    }

    @Test
    @DisplayName("Verify embedded warranty persistence and validity check")
    void testDeviceWarrantyPersistenceAndCheck() throws SQLException {
        Device device = new Device(testCustomerId, "Laptop", "Apple", "MacBook Pro", "SN-MBP16", "Space Gray", "MagSafe");
        LocalDate now = LocalDate.now();
        device.setWarrantyMonths(12);
        device.setWarrantyStartDate(now.toString());
        device.setWarrantyExpiryDate(now.plusMonths(12).toString());

        int devId = DeviceDB.createDevice(device);
        assertTrue(devId > 0);

        Device retrieved = DeviceDB.getDeviceById(devId);
        assertNotNull(retrieved);
        assertEquals(12, retrieved.getWarrantyMonths());
        assertEquals(now.toString(), retrieved.getWarrantyStartDate());
        assertEquals(now.plusMonths(12).toString(), retrieved.getWarrantyExpiryDate());
        assertTrue(retrieved.isUnderWarranty(), "Device should be under active warranty");
        assertTrue(DeviceDB.isDeviceUnderWarranty(devId));

        // Test expired warranty
        retrieved.setWarrantyStartDate(now.minusMonths(24).toString());
        retrieved.setWarrantyExpiryDate(now.minusMonths(12).toString());
        DeviceDB.updateDevice(retrieved);

        assertFalse(DeviceDB.isDeviceUnderWarranty(devId), "Expired warranty must report false");
    }

    @Test
    @DisplayName("Verify device search with category filter")
    void testSearchDevicesWithCategory() throws SQLException {
        Device d1 = new Device(testCustomerId, "Smartphone", "Apple", "iPhone 14", "SN-1", null, null);
        Device d2 = new Device(testCustomerId, "Tablet", "Apple", "iPad Pro", "SN-2", null, null);
        Device d3 = new Device(testCustomerId, "Laptop", "Dell", "XPS 13", "SN-3", null, null);

        DeviceDB.createDevice(d1);
        DeviceDB.createDevice(d2);
        DeviceDB.createDevice(d3);

        List<Device> appleResults = DeviceDB.searchDevices("Apple", "All Categories");
        assertEquals(2, appleResults.size());

        List<Device> appleTablets = DeviceDB.searchDevices("Apple", "Tablet");
        assertEquals(1, appleTablets.size());
        assertEquals("iPad Pro", appleTablets.get(0).getModel());

        List<Device> laptops = DeviceDB.searchDevices("", "Laptop");
        assertEquals(1, laptops.size());
        assertEquals("Dell", laptops.get(0).getBrand());
    }

    @Test
    @DisplayName("Verify device deletion blocked when repair jobs exist")
    void testDeleteDeviceBlockedByRepairs() throws SQLException {
        Device device = new Device(testCustomerId, "Smartphone", "Google", "Pixel 7", "SN-PX7", null, null);
        int devId = DeviceDB.createDevice(device);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO repair_jobs (customer_id, device_id, date_received, initial_problem, current_status, created_at, updated_at) " +
                             "VALUES (?, ?, datetime('now'), 'Battery issue', 'RECEIVED', datetime('now'), datetime('now'))")) {
            ps.setInt(1, testCustomerId);
            ps.setInt(2, devId);
            ps.executeUpdate();
        }

        SQLException ex = assertThrows(SQLException.class, () -> DeviceDB.deleteDevice(devId));
        assertTrue(ex.getMessage().contains("Historical repair records are associated"));
    }
}
