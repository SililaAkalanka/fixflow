package com.fixflow.service;

import com.fixflow.model.Customer;
import com.fixflow.model.Device;
import com.fixflow.model.RepairJob;
import com.fixflow.repository.CustomerRepository;
import com.fixflow.repository.DeviceRepository;
import com.fixflow.repository.RepairRepository;
import com.fixflow.util.ValidationUtil;

import java.util.List;

/**
 * Service class for customer business operations and validation.
 */
public class CustomerService {

    private static CustomerRepository customerRepo = new CustomerRepository();
    private static DeviceRepository deviceRepo = new DeviceRepository();
    private static RepairRepository repairRepo = new RepairRepository();

    private CustomerService() {
    }

    public static void setRepositories(CustomerRepository cr, DeviceRepository dr, RepairRepository rr) {
        customerRepo = cr;
        deviceRepo = dr;
        repairRepo = rr;
    }

    public static CustomerRepository getCustomerRepository() {
        return customerRepo;
    }

    public static int createCustomer(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null.");
        }
        if (!ValidationUtil.isNotEmpty(customer.getFullName())) {
            throw new IllegalArgumentException("Customer full name is required.");
        }
        if (!ValidationUtil.isValidPhone(customer.getPhone())) {
            throw new IllegalArgumentException("Invalid phone number format: " + customer.getPhone());
        }
        if (!ValidationUtil.isValidOptionalEmail(customer.getEmail())) {
            throw new IllegalArgumentException("Invalid email address format: " + customer.getEmail());
        }

        customerRepo.add(customer);
        return customer.getCustomerId();
    }

    public static Customer getCustomerById(int customerId) {
        return customerRepo.findById(customerId);
    }

    public static List<Customer> getAllCustomers() {
        return customerRepo.findAll();
    }

    public static List<Customer> searchCustomers(String query) {
        return customerRepo.search(query);
    }

    public static boolean updateCustomer(Customer customer) {
        if (customer == null || customer.getCustomerId() <= 0) {
            throw new IllegalArgumentException("Valid customer ID is required.");
        }
        if (!ValidationUtil.isNotEmpty(customer.getFullName())) {
            throw new IllegalArgumentException("Customer full name is required.");
        }
        if (!ValidationUtil.isValidPhone(customer.getPhone())) {
            throw new IllegalArgumentException("Invalid phone number format: " + customer.getPhone());
        }
        if (!ValidationUtil.isValidOptionalEmail(customer.getEmail())) {
            throw new IllegalArgumentException("Invalid email address format: " + customer.getEmail());
        }

        Customer existing = customerRepo.findById(customer.getCustomerId());
        if (existing == null) {
            return false;
        }

        existing.setFullName(customer.getFullName().trim());
        existing.setPhone(customer.getPhone().trim());
        existing.setEmail(customer.getEmail() != null ? customer.getEmail().trim() : "");
        existing.setAddress(customer.getAddress() != null ? customer.getAddress().trim() : "");

        customerRepo.update(existing);
        return true;
    }

    public static boolean deleteCustomer(int customerId) {
        Customer existing = customerRepo.findById(customerId);
        if (existing == null) {
            return false;
        }

        // Check referential integrity: reject if customer has devices
        List<Device> devices = deviceRepo.findByCustomerId(customerId);
        if (!devices.isEmpty()) {
            throw new IllegalStateException("Cannot delete customer #" + customerId + ": Customer has " +
                    devices.size() + " associated device(s). Remove devices first.");
        }

        // Check referential integrity: reject if customer has repair jobs
        List<RepairJob> jobs = repairRepo.findByCustomerId(customerId);
        if (!jobs.isEmpty()) {
            throw new IllegalStateException("Cannot delete customer #" + customerId + ": Customer has " +
                    jobs.size() + " associated repair job(s).");
        }

        return customerRepo.delete(customerId);
    }
}
