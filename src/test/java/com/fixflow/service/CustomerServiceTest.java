package com.fixflow.service;

import com.fixflow.model.Customer;
import com.fixflow.model.Device;
import com.fixflow.repository.CustomerRepository;
import com.fixflow.repository.DeviceRepository;
import com.fixflow.repository.RepairRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerServiceTest {

    private File tempDir;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = File.createTempFile("fixflow_custtest_", "_dir");
        tempDir.delete();
        tempDir.mkdirs();

        CustomerRepository cr = new CustomerRepository(new File(tempDir, "customers.json").getAbsolutePath());
        DeviceRepository dr = new DeviceRepository(new File(tempDir, "devices.json").getAbsolutePath());
        RepairRepository rr = new RepairRepository(new File(tempDir, "repair_jobs.json").getAbsolutePath());

        CustomerService.setRepositories(cr, dr, rr);
        DeviceService.setRepositories(dr, cr, rr);
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
    @DisplayName("Verify customer registration and search")
    void testCustomerRegistrationAndSearch() {
        Customer c1 = new Customer("Kasun Jayawardena", "0771122334", "kasun@test.com", "Colombo");
        int id1 = CustomerService.createCustomer(c1);
        assertTrue(id1 > 0);

        Customer c2 = new Customer("Dilani Perera", "0785566778", "dilani@test.com", "Kandy");
        int id2 = CustomerService.createCustomer(c2);
        assertTrue(id2 > 0);

        List<Customer> searchResults = CustomerService.searchCustomers("Kasun");
        assertEquals(1, searchResults.size());
        assertEquals("Kasun Jayawardena", searchResults.get(0).getFullName());
    }

    @Test
    @DisplayName("Verify customer validation rejects invalid phone numbers")
    void testInvalidCustomerPhone() {
        Customer c = new Customer("Invalid Phone", "abc", "test@test.com", "Galle");
        assertThrows(IllegalArgumentException.class, () -> CustomerService.createCustomer(c));
    }

    @Test
    @DisplayName("Verify customer cannot be deleted if associated with devices")
    void testDeleteCustomerBlockedWithDevices() {
        Customer c = new Customer("Nuwan Silva", "0712345678", "nuwan@test.com", "Matara");
        int custId = CustomerService.createCustomer(c);

        Device d = new Device(custId, "Smartphone", "Apple", "iPhone 12", "SN-9988", "Blue", "None");
        DeviceService.createDevice(d);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                CustomerService.deleteCustomer(custId)
        );
        assertTrue(ex.getMessage().contains("device"));
    }
}
