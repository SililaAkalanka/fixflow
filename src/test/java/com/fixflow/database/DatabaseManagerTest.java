package com.fixflow.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {

    private Connection sharedMemoryConnection;

    @BeforeEach
    void setUp() throws SQLException {
        // Using SQLite shared in-memory database URI so multiple connections can access the same in-memory DB during test
        String testUrl = "jdbc:sqlite:file:memdb_test?mode=memory&cache=shared";
        DatabaseManager.setCustomJdbcUrl(testUrl);

        // Keep a root connection open so in-memory DB doesn't vanish between getConnection() calls
        sharedMemoryConnection = DatabaseManager.getConnection();
        DatabaseManager.initializeDatabase();
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (sharedMemoryConnection != null && !sharedMemoryConnection.isClosed()) {
            sharedMemoryConnection.close();
        }
        DatabaseManager.setCustomJdbcUrl(null);
    }

    @Test
    @DisplayName("Verify that all 10 core tables exist after schema initialization")
    void testAllCoreTablesExist() throws SQLException {
        for (String table : DatabaseManager.CORE_TABLES) {
            assertTrue(DatabaseManager.tableExists(table), "Expected table '" + table + "' to exist.");
        }
        assertTrue(DatabaseManager.verifySchema());
    }

    @Test
    @DisplayName("Verify foreign key constraints are enforced on devices table")
    void testForeignKeyEnforcement() {
        // Inserting a device referencing non-existent customer_id 9999 must fail
        assertThrows(SQLException.class, () -> {
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO devices (customer_id, category, brand, model, created_at) " +
                                 "VALUES (?, ?, ?, ?, datetime('now'))")) {
                ps.setInt(1, 9999); // Invalid customer_id
                ps.setString(2, "Smartphone");
                ps.setString(3, "Apple");
                ps.setString(4, "iPhone 13");
                ps.executeUpdate();
            }
        }, "Inserting a device with non-existent customer must throw SQLException due to foreign key violation.");
    }

    @Test
    @DisplayName("Verify check constraint prevents negative stock in spare_parts")
    void testSparePartsNegativeStockConstraint() {
        assertThrows(SQLException.class, () -> {
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO spare_parts (part_name, category, compatible_device_model, purchase_cost, selling_price, available_quantity, created_at, updated_at) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))")) {
                ps.setString(1, "Battery");
                ps.setString(2, "Battery");
                ps.setString(3, "iPhone 13");
                ps.setDouble(4, 20.0);
                ps.setDouble(5, 45.0);
                ps.setInt(6, -5); // Negative quantity violates CHECK constraint
                ps.executeUpdate();
            }
        }, "Inserting negative available_quantity must fail due to CHECK constraint.");
    }

    @Test
    @DisplayName("Verify successful customer and device creation with foreign key")
    void testValidCustomerAndDeviceInsert() throws SQLException {
        int customerId;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO customers (full_name, phone, email, created_at) VALUES (?, ?, ?, datetime('now'))",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "Alice Smith");
            ps.setString(2, "0771234567");
            ps.setString(3, "alice@example.com");
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                assertTrue(rs.next());
                customerId = rs.getInt(1);
            }
        }

        // Insert device referencing the newly created customer
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO devices (customer_id, category, brand, model, serial_number, created_at) " +
                             "VALUES (?, ?, ?, ?, ?, datetime('now'))")) {
            ps.setInt(1, customerId);
            ps.setString(2, "Laptop");
            ps.setString(3, "Dell");
            ps.setString(4, "XPS 15");
            ps.setString(5, "DL-987654");
            int rows = ps.executeUpdate();
            assertEquals(1, rows);
        }
    }
}

