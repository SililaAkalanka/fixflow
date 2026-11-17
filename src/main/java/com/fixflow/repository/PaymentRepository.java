package com.fixflow.repository;

import com.fixflow.model.Payment;
import com.fixflow.util.JsonFileHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing Payment persistence in JSON format.
 * Implements Repository<Payment> using an ArrayList.
 */
public class PaymentRepository implements Repository<Payment> {

    private static final String DEFAULT_FILE_PATH = "data/payments.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String filePath;
    private List<Payment> payments;

    public PaymentRepository() {
        this(DEFAULT_FILE_PATH);
    }

    public PaymentRepository(String filePath) {
        this.filePath = filePath;
        this.payments = new ArrayList<>();
        load();
    }

    @Override
    public void add(Payment payment) {
        if (payment.getPaymentId() <= 0) {
            payment.setPaymentId(generateNextId());
        }
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        if (payment.getPaymentDate() == null || payment.getPaymentDate().isEmpty()) {
            payment.setPaymentDate(now);
        }
        if (payment.getCreatedAt() == null || payment.getCreatedAt().isEmpty()) {
            payment.setCreatedAt(now);
        }
        payments.add(payment);
        save();
    }

    @Override
    public Payment findById(int id) {
        for (Payment p : payments) {
            if (p.getPaymentId() == id) {
                return p;
            }
        }
        return null;
    }

    @Override
    public List<Payment> findAll() {
        return new ArrayList<>(payments);
    }

    @Override
    public void update(Payment payment) {
        for (int i = 0; i < payments.size(); i++) {
            if (payments.get(i).getPaymentId() == payment.getPaymentId()) {
                payments.set(i, payment);
                save();
                return;
            }
        }
    }

    @Override
    public boolean delete(int id) {
        for (int i = 0; i < payments.size(); i++) {
            if (payments.get(i).getPaymentId() == id) {
                payments.remove(i);
                save();
                return true;
            }
        }
        return false;
    }

    @Override
    public void save() {
        JsonFileHandler.saveList(filePath, payments);
    }

    @Override
    public void load() {
        payments = JsonFileHandler.loadList(filePath, Payment.class);
    }

    public List<Payment> findByJobId(int jobId) {
        List<Payment> results = new ArrayList<>();
        for (Payment p : payments) {
            if (p.getJobId() == jobId) {
                results.add(p);
            }
        }
        return results;
    }

    public double getTotalPaidForJob(int jobId) {
        double total = 0.0;
        for (Payment p : payments) {
            if (p.getJobId() == jobId) {
                total += p.getAmountPaid();
            }
        }
        return total;
    }

    private int generateNextId() {
        int maxId = 0;
        for (Payment p : payments) {
            if (p.getPaymentId() > maxId) {
                maxId = p.getPaymentId();
            }
        }
        return maxId + 1;
    }
}
