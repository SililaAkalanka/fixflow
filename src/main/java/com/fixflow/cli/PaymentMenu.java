package com.fixflow.cli;

import com.fixflow.enums.PaymentStatus;
import com.fixflow.model.Payment;
import com.fixflow.model.RepairJob;
import com.fixflow.service.InventoryService;
import com.fixflow.service.PaymentService;
import com.fixflow.service.RepairService;
import com.fixflow.util.PdfInvoiceGenerator;

import java.io.File;
import java.util.List;

/**
 * CLI menu for Payment recording, balance tracking, and invoice generation.
 */
public class PaymentMenu {

    private final InputHandler input;

    public PaymentMenu(InputHandler input) {
        this.input = input;
    }

    public void display() {
        boolean back = false;
        while (!back) {
            System.out.println("\n========================================");
            System.out.println("           PAYMENT MANAGEMENT           ");
            System.out.println("========================================");
            System.out.println("1. Record Payment for Repair Job");
            System.out.println("2. View Payments for a Repair Job");
            System.out.println("3. Check Outstanding Balance & Status");
            System.out.println("4. List All Recorded Payments");
            System.out.println("5. Generate PDF Invoice Receipt");
            System.out.println("0. Back to Main Menu");
            System.out.println("========================================");

            int choice = input.readInt("Enter choice: ", 0, 5);
            switch (choice) {
                case 1 -> recordPayment();
                case 2 -> viewPaymentsForJob();
                case 3 -> checkOutstandingBalance();
                case 4 -> listAllPayments();
                case 5 -> generatePdfInvoice();
                case 0 -> back = true;
            }
        }
    }

    private void recordPayment() {
        System.out.println("\n--- Record Cash Payment ---");
        int jobId = input.readInt("Enter Repair Job ID: ");
        RepairJob job = RepairService.getRepairJobById(jobId);
        if (job == null) {
            System.out.println("[!] Repair Job #" + jobId + " not found.");
            input.pressEnterToContinue();
            return;
        }

        double totalCost = PaymentService.calculateTotalCost(jobId);
        double totalPaid = PaymentService.getTotalPaid(jobId);
        double balance = PaymentService.getRemainingBalance(jobId);
        PaymentStatus status = PaymentService.getPaymentStatus(jobId);

        System.out.println("----------------------------------------");
        System.out.printf("Repair Job #%d Overview:%n", jobId);
        System.out.println("Customer          : " + job.getCustomerName());
        System.out.println("Device            : " + job.getDeviceSummary());
        System.out.printf("Total Repair Cost : LKR %.2f%n", totalCost);
        System.out.printf("Already Paid      : LKR %.2f%n", totalPaid);
        System.out.printf("Remaining Balance : LKR %.2f%n", balance);
        System.out.printf("Payment Status    : %s%n", status.getDisplayName());
        System.out.println("----------------------------------------");

        if (status == PaymentStatus.PAID || balance <= 0.001) {
            System.out.println("[!] This repair job is already fully settled. No further payments accepted.");
            input.pressEnterToContinue();
            return;
        }

        System.out.printf("Enter payment amount (Max allowed: LKR %.2f):%n", balance);
        double amount = input.readDouble("Amount to pay (LKR): ", 0.01, 1000000);
        String notes = input.readOptionalString("Enter Payment Notes (e.g. Deposit, Final Settlement): ");

        try {
            int paymentId = PaymentService.recordPayment(jobId, amount, null, notes);
            double newBalance = PaymentService.getRemainingBalance(jobId);
            PaymentStatus newStatus = PaymentService.getPaymentStatus(jobId);

            System.out.println("\n[✓] Payment recorded successfully! Payment ID #" + paymentId);
            System.out.printf("    Amount Paid       : LKR %.2f%n", amount);
            System.out.printf("    New Balance       : LKR %.2f%n", newBalance);
            System.out.printf("    New Payment Status: %s%n", newStatus.getDisplayName());
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("\n[!] Payment Error: " + e.getMessage());
        }
        input.pressEnterToContinue();
    }

    private void viewPaymentsForJob() {
        System.out.println("\n--- Payments for Repair Job ---");
        int jobId = input.readInt("Enter Repair Job ID: ");
        RepairJob job = RepairService.getRepairJobById(jobId);
        if (job == null) {
            System.out.println("[!] Repair Job #" + jobId + " not found.");
            input.pressEnterToContinue();
            return;
        }

        List<Payment> list = PaymentService.getPaymentsForJob(jobId);
        if (list.isEmpty()) {
            System.out.println("No payments recorded yet for Repair Job #" + jobId);
        } else {
            System.out.printf("Payment History for Job #%d (%s):%n", jobId, job.getCustomerName());
            printPaymentTable(list);
            System.out.printf("%nTotal Paid: LKR %.2f | Balance: LKR %.2f | Status: %s%n",
                    PaymentService.getTotalPaid(jobId),
                    PaymentService.getRemainingBalance(jobId),
                    PaymentService.getPaymentStatus(jobId).getDisplayName());
        }
        input.pressEnterToContinue();
    }

    private void checkOutstandingBalance() {
        System.out.println("\n--- Check Outstanding Balance ---");
        int jobId = input.readInt("Enter Repair Job ID: ");
        RepairJob job = RepairService.getRepairJobById(jobId);
        if (job == null) {
            System.out.println("[!] Repair Job #" + jobId + " not found.");
            input.pressEnterToContinue();
            return;
        }

        double total = PaymentService.calculateTotalCost(jobId);
        double paid = PaymentService.getTotalPaid(jobId);
        double balance = PaymentService.getRemainingBalance(jobId);
        PaymentStatus status = PaymentService.getPaymentStatus(jobId);

        System.out.println("========================================");
        System.out.printf("  BALANCE INQUIRY - JOB #%d%n", jobId);
        System.out.println("========================================");
        System.out.println("Customer           : " + job.getCustomerName());
        System.out.println("Device             : " + job.getDeviceSummary());
        System.out.println("Repair Status      : " + job.getCurrentStatus().getDisplayName());
        System.out.printf("Labour Charges     : LKR %.2f%n", job.getLabourCharges());
        System.out.printf("Spare Parts Cost   : LKR %.2f%n", InventoryService.getTotalPartsCostForJob(jobId));
        System.out.printf("Total Repair Cost  : LKR %.2f%n", total);
        System.out.printf("Total Amount Paid  : LKR %.2f%n", paid);
        System.out.printf("Outstanding Balance: LKR %.2f%n", balance);
        System.out.printf("Settlement Status  : %s%n", status.getDisplayName());
        System.out.println("========================================");
        input.pressEnterToContinue();
    }

    private void listAllPayments() {
        System.out.println("\n--- All Recorded Payment Transactions ---");
        List<Payment> list = PaymentService.getAllPayments();
        if (list.isEmpty()) {
            System.out.println("No payments recorded in the system.");
        } else {
            printPaymentTable(list);
        }
        input.pressEnterToContinue();
    }

    private void generatePdfInvoice() {
        System.out.println("\n--- Generate PDF Invoice Receipt ---");
        int paymentId = input.readInt("Enter Payment ID: ");
        Payment p = PaymentService.getPaymentById(paymentId);
        if (p == null) {
            System.out.println("[!] Payment record #" + paymentId + " not found.");
            input.pressEnterToContinue();
            return;
        }

        File invoicesDir = new File("invoices");
        if (!invoicesDir.exists()) {
            invoicesDir.mkdirs();
        }

        File pdfFile = new File(invoicesDir, "Invoice_Pay" + paymentId + "_Job" + p.getJobId() + ".pdf");
        try {
            PdfInvoiceGenerator.generateInvoice(paymentId, pdfFile);
            System.out.println("\n[✓] PDF Invoice generated successfully!");
            System.out.println("    File location: " + pdfFile.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("\n[!] Failed to generate PDF: " + e.getMessage());
        }
        input.pressEnterToContinue();
    }

    private void printPaymentTable(List<Payment> list) {
        System.out.printf("%-6s | %-8s | %-14s | %-18s | %-20s%n",
                "ID", "Job ID", "Amount (LKR)", "Payment Date", "Notes");
        System.out.println("-------+----------+----------------+--------------------+----------------------");
        for (Payment p : list) {
            String note = p.getNotes() != null ? p.getNotes() : "-";
            System.out.printf("#%-5d | #%-7d | %14.2f | %-18s | %-20s%n",
                    p.getPaymentId(),
                    p.getJobId(),
                    p.getAmountPaid(),
                    truncate(p.getPaymentDate(), 18),
                    truncate(note, 20));
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "-";
        return str.length() <= maxLen ? str : str.substring(0, maxLen - 3) + "...";
    }
}
