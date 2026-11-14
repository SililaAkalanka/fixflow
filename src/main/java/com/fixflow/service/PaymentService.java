package com.fixflow.service;

import com.fixflow.database.InventoryDB;
import com.fixflow.database.PaymentDB;
import com.fixflow.database.RepairDB;
import com.fixflow.enums.PaymentStatus;
import com.fixflow.model.Payment;
import com.fixflow.model.RepairJob;

import java.sql.SQLException;
import java.util.List;

/**
 * Service class for payment calculations, partial payment management, and balance tracking.
 */
public class PaymentService {

    private PaymentService() {
        // Business service
    }

    /**
     * Calculates the total repair cost for a job.
     * Formula: Labour Charges + Spare Parts Used Cost.
     *
     * @param jobId repair job ID
     * @return total repair cost
     * @throws SQLException if a database error occurs
     */
    public static double calculateTotalCost(int jobId) throws SQLException {
        RepairJob job = RepairDB.getRepairJobById(jobId);
        if (job == null) {
            throw new SQLException("Repair Job #" + jobId + " not found.");
        }
        double labour = job.getLabourCharges();
        double parts = InventoryDB.getTotalPartsCostForJob(jobId);
        return labour + parts;
    }

    /**
     * Returns the total amount paid so far for a repair job.
     *
     * @param jobId repair job ID
     * @return sum of all payments
     * @throws SQLException if a database error occurs
     */
    public static double getTotalPaid(int jobId) throws SQLException {
        return PaymentDB.getTotalPaidForJob(jobId);
    }

    /**
     * Calculates the outstanding balance for a repair job.
     * Formula: Total Cost - Total Paid.
     *
     * @param jobId repair job ID
     * @return remaining balance (0.0 if fully paid or negative due to float rounding)
     * @throws SQLException if a database error occurs
     */
    public static double getRemainingBalance(int jobId) throws SQLException {
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
     * @throws SQLException if a database error occurs
     */
    public static PaymentStatus getPaymentStatus(int jobId) throws SQLException {
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
     * @throws SQLException if a database error occurs
     */
    public static boolean isJobFullyPaid(int jobId) throws SQLException {
        return getPaymentStatus(jobId) == PaymentStatus.PAID;
    }

    /**
     * Validates and records a cash payment (partial or full) for a repair job.
     * Prevents overpayment and zero/negative amounts.
     *
     * @param jobId repair job ID
     * @param amount amount to pay
     * @param paymentDate payment timestamp (or null for current time)
     * @param notes optional payment notes
     * @return generated payment ID
     * @throws SQLException if a database error occurs
     * @throws IllegalArgumentException if validation fails
     */
    public static int recordPayment(int jobId, double amount, String paymentDate, String notes) throws SQLException {
        RepairJob job = RepairDB.getRepairJobById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Repair Job #" + jobId + " does not exist.");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
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

        Payment payment = new Payment(jobId, paymentDate, amount, notes);
        return PaymentDB.insertPayment(payment);
    }

    /**
     * Retrieves all payments for a given repair job.
     */
    public static List<Payment> getPaymentsForJob(int jobId) throws SQLException {
        return PaymentDB.getPaymentsByJobId(jobId);
    }

    /**
     * Retrieves all payments across all jobs.
     */
    public static List<Payment> getAllPayments() throws SQLException {
        return PaymentDB.getAllPayments();
    }
}
