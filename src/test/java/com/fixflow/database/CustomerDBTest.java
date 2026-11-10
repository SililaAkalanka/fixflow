package com.fixflow.database;

import com.fixflow.model.Customer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CustomerDBTest {

    private Connection sharedMemoryConnection;

    @BeforeEach
    void setUp() throws SQLException {
        String testUrl = "jdbc:sqlite:file:custdb_test?mode=memory&cache=shared";
        DatabaseManager.setCustomJdbcUrl(testUrl);

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
    @DisplayName("Verify customer creation and retrieval")
    void testCreateAndGetCustomer() throws SQLException {
        Customer customer = new Customer("John Doe", "0771234567", "john@example.com", "123 Main St");
        int id = CustomerDB.createCustomer(customer);
        assertTrue(id > 0);

        Customer retrieved = CustomerDB.getCustomerById(id);
        assertNotNull(retrieved);
        assertEquals("John Doe", retrieved.getFullName());
        assertEquals("0771234567", retrieved.getPhone());
        assertEquals("john@example.com", retrieved.getEmail());
        assertEquals("123 Main St", retrieved.getAddress());
        assertNotNull(retrieved.getCreatedAt());
    }

    @Test
    @DisplayName("Verify customer update")
    void testUpdateCustomer() throws SQLException {
        Customer customer = new Customer("Jane Doe", "0779998888", "jane@example.com", "456 High St");
        int id = CustomerDB.createCustomer(customer);

        customer.setFullName("Jane Smith");
        customer.setPhone("0770001111");
        customer.setEmail("janesmith@example.com");
        customer.setAddress("789 Park Lane");

        boolean updated = CustomerDB.updateCustomer(customer);
        assertTrue(updated);

        Customer updatedCustomer = CustomerDB.getCustomerById(id);
        assertEquals("Jane Smith", updatedCustomer.getFullName());
        assertEquals("0770001111", updatedCustomer.getPhone());
    }

    @Test
    @DisplayName("Verify customer search by name, phone, and ID")
    void testSearchCustomers() throws SQLException {
        Customer c1 = new Customer("Alice Johnson", "0711112222", null, null);
        Customer c2 = new Customer("Bob Smith", "0723334444", null, null);
        Customer c3 = new Customer("Alice Cooper", "0755556666", null, null);

        int id1 = CustomerDB.createCustomer(c1);
        CustomerDB.createCustomer(c2);
        CustomerDB.createCustomer(c3);

        List<Customer> aliceResults = CustomerDB.searchCustomers("Alice");
        assertEquals(2, aliceResults.size());

        List<Customer> phoneResults = CustomerDB.searchCustomers("072333");
        assertEquals(1, phoneResults.size());
        assertEquals("Bob Smith", phoneResults.get(0).getFullName());

        List<Customer> idResults = CustomerDB.searchCustomers(String.valueOf(id1));
        assertEquals(1, idResults.size());
        assertEquals("Alice Johnson", idResults.get(0).getFullName());
    }

    @Test
    @DisplayName("Verify deletion succeeds when no repair records exist")
    void testDeleteCustomerWithoutRepairs() throws SQLException {
        Customer customer = new Customer("Ephemeral User", "0700000000", null, null);
        int id = CustomerDB.createCustomer(customer);

        boolean deleted = CustomerDB.deleteCustomer(id);
        assertTrue(deleted);
        assertNull(CustomerDB.getCustomerById(id));
    }

    @Test
    @DisplayName("Verify deletion is strictly blocked when repair records exist")
    void testDeleteCustomerWithRepairsThrows() throws SQLException {
        Customer customer = new Customer("Repair Client", "0775554444", null, null);
        int custId = CustomerDB.createCustomer(customer);

        // Insert dummy device & repair job referencing this customer
        int devId;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO devices (customer_id, category, brand, model, created_at) VALUES (?, 'Phone', 'Apple', 'iPhone', datetime('now'))",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, custId);
            ps.executeUpdate();
            var rs = ps.getGeneratedKeys();
            rs.next();
            devId = rs.getInt(1);
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO repair_jobs (customer_id, device_id, date_received, initial_problem, current_status, created_at, updated_at) " +
                             "VALUES (?, ?, datetime('now'), 'Screen broken', 'RECEIVED', datetime('now'), datetime('now'))")) {
            ps.setInt(1, custId);
            ps.setInt(2, devId);
            ps.executeUpdate();
        }

        // Attempting to delete customer must fail and throw SQLException
        SQLException ex = assertThrows(SQLException.class, () -> CustomerDB.deleteCustomer(custId));
        assertTrue(ex.getMessage().contains("Associated repair job history exists"));
    }
}

