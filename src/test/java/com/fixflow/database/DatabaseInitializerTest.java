package com.fixflow.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseInitializerTest {

    private File tempDbFile;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = File.createTempFile("fixflow_init_test_", ".db");
        DatabaseConnection.setCustomJdbcUrl("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
    }

    @AfterEach
    void tearDown() {
        DatabaseConnection.setCustomJdbcUrl(null);
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    @Test
    void testFreshSchemaInitialization() throws Exception {
        DatabaseInitializer.initializeDatabase();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Check PRAGMA foreign_keys
            try (ResultSet rs = stmt.executeQuery("PRAGMA foreign_keys;")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "PRAGMA foreign_keys must be enabled (1)");
            }

            // Check that all core tables exist
            Set<String> tables = new HashSet<>();
            try (ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table';")) {
                while (rs.next()) {
                    tables.add(rs.getString(1).toLowerCase());
                }
            }

            assertTrue(tables.contains("customers"), "customers table must exist");
            assertTrue(tables.contains("devices"), "devices table must exist");
            assertTrue(tables.contains("technicians"), "technicians table must exist");
            assertTrue(tables.contains("repair_jobs"), "repair_jobs table must exist");
            assertTrue(tables.contains("spare_parts"), "spare_parts table must exist");
            assertTrue(tables.contains("repair_part_usage"), "repair_part_usage table must exist");
            assertTrue(tables.contains("payments"), "payments table must exist");
            assertTrue(tables.contains("users"), "users table must exist");

            // Obsolete tables must NOT exist
            assertFalse(tables.contains("warranties"), "warranties table must not exist");
            assertFalse(tables.contains("repair_status_history"), "repair_status_history table must not exist");
        }
    }

    @Test
    void testMigrationRemovesUniqueJobIdOnPayments() throws Exception {
        // 1. Initialize full database
        DatabaseInitializer.initializeDatabase();

        // 2. Simulate legacy payments table with UNIQUE(job_id)
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE payments;");
            stmt.execute("CREATE TABLE payments (" +
                    "payment_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "job_id INTEGER NOT NULL UNIQUE, " +
                    "payment_date TEXT NOT NULL, " +
                    "amount_paid REAL NOT NULL CHECK (amount_paid >= 0), " +
                    "payment_method TEXT NOT NULL DEFAULT 'CASH', " +
                    "payment_status TEXT NOT NULL DEFAULT 'PAID', " +
                    "created_at TEXT NOT NULL, " +
                    "FOREIGN KEY (job_id) REFERENCES repair_jobs(job_id) ON DELETE RESTRICT" +
                    ");");

            stmt.execute("INSERT INTO customers (customer_id, full_name, phone, created_at) VALUES (1, 'Test', '077', datetime('now'));");
            stmt.execute("INSERT INTO devices (device_id, customer_id, category, brand, model, created_at) VALUES (1, 1, 'Phone', 'Brand', 'Model', datetime('now'));");
            stmt.execute("INSERT INTO repair_jobs (job_id, customer_id, device_id, date_received, initial_problem, current_status, created_at, updated_at) " +
                    "VALUES (1, 1, 1, datetime('now'), 'Broken', 'RECEIVED', datetime('now'), datetime('now'));");

            stmt.execute("INSERT INTO payments (job_id, payment_date, amount_paid, payment_method, payment_status, created_at) " +
                    "VALUES (1, '2026-01-01 10:00:00', 500.0, 'CASH', 'PAID', '2026-01-01 10:00:00');");
        }

        // 3. Run initializeDatabase to trigger migration
        DatabaseInitializer.initializeDatabase();

        // 4. Verify multiple payments for job 1 now succeed
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO payments (job_id, payment_date, amount_paid, payment_method, notes, created_at) " +
                    "VALUES (1, '2026-01-02 10:00:00', 250.0, 'CASH', 'Partial payment 2', '2026-01-02 10:00:00');");

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM payments WHERE job_id = 1;")) {
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1), "Should allow multiple payments for the same job_id");
            }
        }
    }
}
