package com.fixflow.repository;

import com.fixflow.model.Customer;
import com.fixflow.util.JsonFileHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing Customer persistence in JSON format.
 * Implements Repository<Customer> using an ArrayList.
 */
public class CustomerRepository implements Repository<Customer> {

    private static final String DEFAULT_FILE_PATH = "data/customers.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String filePath;
    private List<Customer> customers;

    public CustomerRepository() {
        this(DEFAULT_FILE_PATH);
    }

    public CustomerRepository(String filePath) {
        this.filePath = filePath;
        this.customers = new ArrayList<>();
        load();
    }

    @Override
    public void add(Customer customer) {
        if (customer.getCustomerId() <= 0) {
            customer.setCustomerId(generateNextId());
        }
        if (customer.getCreatedAt() == null || customer.getCreatedAt().isEmpty()) {
            customer.setCreatedAt(LocalDateTime.now().format(DATE_FORMATTER));
        }
        customers.add(customer);
        save();
    }

    @Override
    public Customer findById(int id) {
        for (Customer c : customers) {
            if (c.getCustomerId() == id) {
                return c;
            }
        }
        return null;
    }

    @Override
    public List<Customer> findAll() {
        return new ArrayList<>(customers);
    }

    @Override
    public void update(Customer customer) {
        for (int i = 0; i < customers.size(); i++) {
            if (customers.get(i).getCustomerId() == customer.getCustomerId()) {
                customers.set(i, customer);
                save();
                return;
            }
        }
    }

    @Override
    public boolean delete(int id) {
        for (int i = 0; i < customers.size(); i++) {
            if (customers.get(i).getCustomerId() == id) {
                customers.remove(i);
                save();
                return true;
            }
        }
        return false;
    }

    @Override
    public void save() {
        JsonFileHandler.saveList(filePath, customers);
    }

    @Override
    public void load() {
        customers = JsonFileHandler.loadList(filePath, Customer.class);
    }

    /**
     * Searches for customers matching a search term in name, phone, or email.
     *
     * @param query search term
     * @return list of matching customers
     */
    public List<Customer> search(String query) {
        List<Customer> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }
        String lowerQuery = query.trim().toLowerCase();
        for (Customer c : customers) {
            boolean matchesName = c.getFullName() != null && c.getFullName().toLowerCase().contains(lowerQuery);
            boolean matchesPhone = c.getPhone() != null && c.getPhone().toLowerCase().contains(lowerQuery);
            boolean matchesEmail = c.getEmail() != null && c.getEmail().toLowerCase().contains(lowerQuery);
            if (matchesName || matchesPhone || matchesEmail) {
                results.add(c);
            }
        }
        return results;
    }

    private int generateNextId() {
        int maxId = 0;
        for (Customer c : customers) {
            if (c.getCustomerId() > maxId) {
                maxId = c.getCustomerId();
            }
        }
        return maxId + 1;
    }
}
