package com.fixflow.service;

import com.fixflow.enums.PaymentStatus;
import com.fixflow.model.Customer;
import com.fixflow.model.Device;
import com.fixflow.model.Payment;
import com.fixflow.model.RepairJob;
import com.fixflow.repository.CustomerRepository;
import com.fixflow.repository.DeviceRepository;
import com.fixflow.repository.PaymentRepository;
import com.fixflow.repository.RepairRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service class for payment calculations, partial payment handling, and balance tracking.
 */
public class PaymentService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static PaymentRepository paymentRepo = new PaymentRepository();
    private static RepairRepository repairRepo = new RepairRepository();
    private static CustomerRepository customerRepo = new CustomerRepository();
    private static DeviceRepository deviceRepo = new DeviceRepository();

    private PaymentService() {
    }

    public static void setRepositories(PaymentRepository pr, RepairRepository rr, CustomerRepository cr, DeviceRepository dr) {
        paymentRepo = pr;
        repairRepo = rr;
        customerRepo = cr;
        deviceRepo = dr;
    }

    public static PaymentRepository getPaymentRepository() {
        return paymentRepo;
    }

    /**
     * Calculates the total repair cost for a job.
     * Formula: Labour Charges + Spare Parts Used Cost.
     *
     * @param jobId repair job ID
     * @return total cost in LKR
     */
    public static double calculateTotalCost(int jobId) {
        RepairJob job = repairRepo.findById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Repair Job #" + jobId + " not found.");
        }
        double labour = job.getLabourCharges();
        double parts = InventoryService.getTotalPartsCostForJob(jobId);
        return labour + parts;
    }

    /**
     * Returns the total amount paid so far for a repair job.
     *
     * @param jobId repair job ID
     * @return sum of all recorded payments
     */
    public static double getTotalPaid(int jobId) {
        return paymentRepo.getTotalPaidForJob(jobId);
    }

    /**
     * Calculates the outstanding balance for a repair job.
     * Formula: Total Cost - Total Paid.
     *
     * @param jobId repair job ID
     * @return remaining balance (0.0 if fully paid)
     */
    public static double getRemainingBalance(int jobId) {
        double total = calculateTotalCost(jobId);
        double paid = getTotalPaid(jobId);
        double balance = total - paid;
        return balance > 0.001 ? balance : 0.0;
    }

    /**
     * Determines the current PaymentStatus for a repair job.
     *
     * @param jobId repair job ID
     * @return UNPAID, PARTIALLY_PAID, or PAID
     */
    public static PaymentStatus getPaymentStatus(int jobId) {
        double total = calculateTotalCost(jobId);
        double paid = getTotalPaid(jobId);

        if (paid <= 0.001) {
            return PaymentStatus.UNPAID;
        } else if (paid >= total - 0.001) {
            return PaymentStatus.PAID;
        } else {
            return PaymentStatus.PARTIALLY_PAID;
        }
    }

    /**
     * Checks if a repair job is completely paid.
     *
     * @param jobId repair job ID
     * @return true if paid in full
     */
    public static boolean isJobFullyPaid(int jobId) {
        return getPaymentStatus(jobId) == PaymentStatus.PAID;
    }

    /**
     * Validates and records a cash payment (partial or full) for a repair job.
     * Prevents overpayment and non-positive amounts.
     *
     * @param jobId repair job ID
     * @param amount payment amount
     * @param paymentDate payment timestamp (or null for current time)
     * @param notes optional payment note
     * @return generated payment ID
     */
    public static int recordPayment(int jobId, double amount, String paymentDate, String notes) {
        RepairJob job = repairRepo.findById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Repair Job #" + jobId + " does not exist.");
        }

        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be a valid positive number.");
        }

        double balance = getRemainingBalance(jobId);
        if (balance <= 0.001) {
            throw new IllegalStateException("Repair Job #" + jobId + " is already fully paid.");
        }

        if (amount > balance + 0.001) {
            throw new IllegalArgumentException(String.format(
                    "Payment amount (LKR %.2f) exceeds remaining balance (LKR %.2f). Overpayment is not allowed.",
                    amount, balance));
        }

        String dateStr = (paymentDate != null && !paymentDate.trim().isEmpty())
                ? paymentDate.trim()
                : LocalDateTime.now().format(DATE_FORMATTER);

        Payment payment = new Payment(jobId, dateStr, amount, notes);
        paymentRepo.add(payment);
        return payment.getPaymentId();
    }

    public static List<Payment> getPaymentsForJob(int jobId) {
        List<Payment> list = paymentRepo.findByJobId(jobId);
        for (Payment p : list) {
            populateTransientPaymentDetails(p);
        }
        return list;
    }

    public static List<Payment> getAllPayments() {
        List<Payment> list = paymentRepo.findAll();
        for (Payment p : list) {
            populateTransientPaymentDetails(p);
        }
        return list;
    }

    public static Payment getPaymentById(int paymentId) {
        Payment p = paymentRepo.findById(paymentId);
        if (p != null) {
            populateTransientPaymentDetails(p);
        }
        return p;
    }

    private static void populateTransientPaymentDetails(Payment payment) {
        RepairJob job = repairRepo.findById(payment.getJobId());
        if (job != null) {
            Customer customer = customerRepo.findById(job.getCustomerId());
            if (customer != null) {
                payment.setCustomerName(customer.getFullName());
                payment.setCustomerPhone(customer.getPhone());
            }
            Device device = deviceRepo.findById(job.getDeviceId());
            if (device != null) {
                payment.setDeviceSummary(device.getBrand() + " " + device.getModel());
            }
            payment.setLabourCharges(job.getLabourCharges());
            payment.setPartsCost(InventoryService.getTotalPartsCostForJob(job.getJobId()));
            payment.setTotalRepairCost(calculateTotalCost(job.getJobId()));
            payment.setPaymentStatus(getPaymentStatus(job.getJobId()).getDisplayName());
        }
    }
}
