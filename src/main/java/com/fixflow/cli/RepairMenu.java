package com.fixflow.cli;

import com.fixflow.enums.PaymentStatus;
import com.fixflow.enums.RepairStatus;
import com.fixflow.exception.InvalidStatusTransitionException;
import com.fixflow.model.*;
import com.fixflow.service.*;

import java.util.List;
import java.util.Set;

/**
 * CLI menu for end-to-end Repair Job lifecycle and assignment management.
 * Demonstrates InvalidStatusTransitionException and business rule validation.
 */
public class RepairMenu {

    private final InputHandler input;

    public RepairMenu(InputHandler input) {
        this.input = input;
    }

    public void display() {
        boolean back = false;
        while (!back) {
            System.out.println("\n========================================");
            System.out.println("           REPAIR MANAGEMENT            ");
            System.out.println("========================================");
            System.out.println("1. Create New Repair Job");
            System.out.println("2. List All Repair Jobs");
            System.out.println("3. Search Repair Jobs");
            System.out.println("4. View Full Job Details");
            System.out.println("5. Assign Technician to Job");
            System.out.println("6. Record Diagnosis & Labour Charges");
            System.out.println("7. Advance / Update Repair Status");
            System.out.println("8. Delete Repair Job");
            System.out.println("0. Back to Main Menu");
            System.out.println("========================================");

            int choice = input.readInt("Enter choice: ", 0, 8);
            switch (choice) {
                case 1 -> createRepairJob();
                case 2 -> listRepairJobs();
                case 3 -> searchRepairJobs();
                case 4 -> viewRepairJobDetails();
                case 5 -> assignTechnician();
                case 6 -> recordDiagnosis();
                case 7 -> updateRepairStatus();
                case 8 -> deleteRepairJob();
                case 0 -> back = true;
            }
        }
    }

    private void createRepairJob() {
        System.out.println("\n--- Create New Repair Job ---");
        int customerId = input.readInt("Enter Customer ID: ");
        Customer customer = CustomerService.getCustomerById(customerId);
        if (customer == null) {
            System.out.println("[!] Customer #" + customerId + " not found. Register customer first.");
            input.pressEnterToContinue();
            return;
        }

        List<Device> devices = DeviceService.getDevicesByCustomerId(customerId);
        if (devices.isEmpty()) {
            System.out.println("[!] Customer #" + customerId + " (" + customer.getFullName() + ") has no registered devices.");
            System.out.println("    Please register a device for this customer first in Device Management.");
            input.pressEnterToContinue();
            return;
        }

        System.out.println("Registered devices for " + customer.getFullName() + ":");
        for (Device d : devices) {
            System.out.printf("  [%d] %s %s (Category: %s, Serial: %s)%n",
                    d.getDeviceId(), d.getBrand(), d.getModel(), d.getCategory(), d.getSerialNumber());
        }

        int deviceId = input.readInt("Enter Device ID to service: ");
        Device device = DeviceService.getDeviceById(deviceId);
        if (device == null || device.getCustomerId() != customerId) {
            System.out.println("[!] Invalid Device ID selected.");
            input.pressEnterToContinue();
            return;
        }

        String problem = input.readString("Enter Initial Problem Description: ");

        try {
            RepairJob job = new RepairJob(customerId, deviceId, problem);
            int id = RepairService.createRepairJob(job);
            System.out.println("\n[✓] Repair Job #" + id + " created successfully with status: RECEIVED");
        } catch (IllegalArgumentException e) {
            System.out.println("\n[!] Creation failed: " + e.getMessage());
        }
        input.pressEnterToContinue();
    }

    private void listRepairJobs() {
        System.out.println("\n--- Repair Jobs Directory ---");
        List<RepairJob> list = RepairService.getAllRepairJobs();
        if (list.isEmpty()) {
            System.out.println("No repair jobs found.");
        } else {
            printJobTable(list);
        }
        input.pressEnterToContinue();
    }

    private void searchRepairJobs() {
        System.out.println("\n--- Search Repair Jobs ---");
        String query = input.readOptionalString("Enter search keyword (or Enter to skip): ");

        System.out.println("Filter by status: 0. Any  1. RECEIVED  2. DIAGNOSING  3. IN_PROGRESS  4. COMPLETED  5. COLLECTED  6. CANCELLED");
        int stChoice = input.readInt("Select status filter (0-6): ", 0, 6);
        RepairStatus statusFilter = switch (stChoice) {
            case 1 -> RepairStatus.RECEIVED;
            case 2 -> RepairStatus.DIAGNOSING;
            case 3 -> RepairStatus.IN_PROGRESS;
            case 4 -> RepairStatus.COMPLETED;
            case 5 -> RepairStatus.COLLECTED;
            case 6 -> RepairStatus.CANCELLED;
            default -> null;
        };

        List<RepairJob> results = RepairService.searchRepairJobs(query, statusFilter, null);
        if (results.isEmpty()) {
            System.out.println("No matching repair jobs found.");
        } else {
            System.out.println("Found " + results.size() + " repair job(s):");
            printJobTable(results);
        }
        input.pressEnterToContinue();
    }

    private void viewRepairJobDetails() {
        System.out.println("\n--- View Full Repair Job Details ---");
        int jobId = input.readInt("Enter Repair Job ID: ");
        RepairJob j = RepairService.getRepairJobById(jobId);
        if (j == null) {
            System.out.println("[!] Repair Job #" + jobId + " not found.");
            input.pressEnterToContinue();
            return;
        }

        System.out.println("==================================================");
        System.out.printf("  REPAIR JOB #%d  [%s]%n", j.getJobId(), j.getCurrentStatus().getDisplayName().toUpperCase());
        System.out.println("==================================================");
        System.out.println("Date Received       : " + j.getDateReceived());
        System.out.println("Customer            : " + j.getCustomerName() + " (Cust #" + j.getCustomerId() + ", Tel: " + j.getCustomerPhone() + ")");
        System.out.println("Device              : " + j.getDeviceSummary() + " (Dev #" + j.getDeviceId() + ")");
        System.out.println("Assigned Technician : " + (j.getTechnicianName() != null ? j.getTechnicianName() : "Unassigned"));
        System.out.println("Initial Problem     : " + j.getInitialProblem());
        System.out.println("Diagnosis Findings  : " + (j.getDiagnosisFindings() != null ? j.getDiagnosisFindings() : "(Pending diagnosis)"));
        System.out.println("Estimated Cost      : " + (j.getEstimatedCost() != null ? String.format("LKR %.2f", j.getEstimatedCost()) : "Not estimated"));
        System.out.println("Labour Charges      : " + String.format("LKR %.2f", j.getLabourCharges()));
        System.out.println("Est. Completion     : " + (j.getEstimatedCompletionDate() != null ? j.getEstimatedCompletionDate() : "-"));
        System.out.println("Actual Completion   : " + (j.getActualCompletionDate() != null ? j.getActualCompletionDate() : "-"));
        System.out.println("Notes / Reason      : " + (j.getNotes() != null ? j.getNotes() : "-"));
        System.out.println("--------------------------------------------------");

        // Parts Used
        List<RepairPartUsage> parts = InventoryService.getPartsUsedForJob(jobId);
        System.out.println("Spare Parts Issued (" + parts.size() + "):");
        if (parts.isEmpty()) {
            System.out.println("  No spare parts used.");
        } else {
            for (RepairPartUsage u : parts) {
                System.out.printf("  - %dx %s @ LKR %.2f = LKR %.2f (Date: %s)%n",
                        u.getQuantity(), u.getPartName(), u.getSellingPriceAtTime(), u.getTotalCost(), u.getDateUsed());
            }
        }

        // Financial Overview
        double totalCost = PaymentService.calculateTotalCost(jobId);
        double paid = PaymentService.getTotalPaid(jobId);
        double balance = PaymentService.getRemainingBalance(jobId);
        PaymentStatus payStatus = PaymentService.getPaymentStatus(jobId);

        System.out.println("--------------------------------------------------");
        System.out.printf("Total Repair Cost   : LKR %.2f (Labour: %.2f + Parts: %.2f)%n",
                totalCost, j.getLabourCharges(), InventoryService.getTotalPartsCostForJob(jobId));
        System.out.printf("Total Paid to Date  : LKR %.2f%n", paid);
        System.out.printf("Remaining Balance   : LKR %.2f%n", balance);
        System.out.printf("Payment Status      : %s%n", payStatus.getDisplayName());
        System.out.println("==================================================");

        input.pressEnterToContinue();
    }

    private void assignTechnician() {
        System.out.println("\n--- Assign Technician to Repair Job ---");
        int jobId = input.readInt("Enter Repair Job ID: ");
        RepairJob job = RepairService.getRepairJobById(jobId);
        if (job == null) {
            System.out.println("[!] Repair Job #" + jobId + " not found.");
            input.pressEnterToContinue();
            return;
        }

        List<Technician> activeTechs = TechnicianService.getActiveTechnicians();
        if (activeTechs.isEmpty()) {
            System.out.println("[!] No active technicians available.");
            input.pressEnterToContinue();
            return;
        }

        System.out.println("Available Active Technicians:");
        for (Technician t : activeTechs) {
            System.out.printf("  [%d] %s (%s, Tel: %s)%n",
                    t.getTechnicianId(), t.getName(), t.getSpecialization(), t.getPhone());
        }

        int techId = input.readInt("Select Technician ID to assign: ");
        try {
            RepairService.assignTechnician(jobId, techId);
            Technician tech = TechnicianService.getTechnicianById(techId);
            System.out.printf("%n[✓] Technician '%s' assigned to Repair Job #%d successfully.%n",
                    tech.getName(), jobId);
        } catch (IllegalArgumentException e) {
            System.out.println("\n[!] Assignment failed: " + e.getMessage());
        }
        input.pressEnterToContinue();
    }

    private void recordDiagnosis() {
        System.out.println("\n--- Record Diagnosis & Labour Charges ---");
        int jobId = input.readInt("Enter Repair Job ID: ");
        RepairJob job = RepairService.getRepairJobById(jobId);
        if (job == null) {
            System.out.println("[!] Repair Job #" + jobId + " not found.");
            input.pressEnterToContinue();
            return;
        }

        String findings = input.readString("Enter Technician Diagnostic Findings: ");
        double estCost = input.readDouble("Enter Estimated Overall Cost (LKR): ", 0, 1000000);
        double labour = input.readDouble("Enter Service Labour Charges (LKR): ", 0, 1000000);
        String estDate = input.readOptionalString("Enter Estimated Completion Date (YYYY-MM-DD, or skip): ");

        RepairService.recordDiagnosis(jobId, findings, estCost, labour, estDate);
        System.out.println("\n[✓] Diagnosis and labour charges recorded for Job #" + jobId);
        input.pressEnterToContinue();
    }

    /**
     * Demonstrates custom checked exception handling (InvalidStatusTransitionException)
     * and strict business rules (cannot collect unpaid device; cancellation requires reason).
     */
    private void updateRepairStatus() {
        System.out.println("\n--- Advance / Update Repair Status ---");
        int jobId = input.readInt("Enter Repair Job ID: ");
        RepairJob job = RepairService.getRepairJobById(jobId);
        if (job == null) {
            System.out.println("[!] Repair Job #" + jobId + " not found.");
            input.pressEnterToContinue();
            return;
        }

        System.out.printf("Current Status of Job #%d: [%s]%n", jobId, job.getCurrentStatus().getDisplayName());

        if (job.getCurrentStatus().isTerminal()) {
            System.out.println("[!] This repair job is in a terminal state (" + job.getCurrentStatus() + ") and cannot transition further.");
            input.pressEnterToContinue();
            return;
        }

        Set<RepairStatus> validNext = job.getValidNextStatuses();
        System.out.println("Permitted Next Statuses: " + validNext);
        System.out.println("Available options: 1. DIAGNOSING  2. IN_PROGRESS  3. COMPLETED  4. COLLECTED  5. CANCELLED");

        int choice = input.readInt("Select Target Status (1-5): ", 1, 5);
        RepairStatus targetStatus = switch (choice) {
            case 1 -> RepairStatus.DIAGNOSING;
            case 2 -> RepairStatus.IN_PROGRESS;
            case 3 -> RepairStatus.COMPLETED;
            case 4 -> RepairStatus.COLLECTED;
            case 5 -> RepairStatus.CANCELLED;
            default -> null;
        };

        String notes = "";
        if (targetStatus == RepairStatus.CANCELLED) {
            notes = input.readString("Enter cancellation reason (Mandatory): ");
        } else {
            notes = input.readOptionalString("Enter status update notes (Optional): ");
        }

        // Try-catch demonstrating custom exception handling
        try {
            RepairService.updateStatus(jobId, targetStatus, notes);
            System.out.printf("%n[✓] Status for Job #%d updated to [%s].%n", jobId, targetStatus.getDisplayName());
        } catch (InvalidStatusTransitionException e) {
            System.out.println("\n[!] STATUS TRANSITION EXCEPTION: " + e.getMessage());
            System.out.println("    Allowed next transitions from " + e.getFromStatus() + ": " + validNext);
        } catch (IllegalStateException e) {
            System.out.println("\n[!] COLLECTION RULE BLOCKED: " + e.getMessage());
            System.out.println("    Please settle outstanding balance in Payment Management before marking device COLLECTED.");
        } catch (IllegalArgumentException e) {
            System.out.println("\n[!] Validation Error: " + e.getMessage());
        }
        input.pressEnterToContinue();
    }

    private void deleteRepairJob() {
        System.out.println("\n--- Delete Repair Job ---");
        int id = input.readInt("Enter Repair Job ID to delete: ");
        RepairJob job = RepairService.getRepairJobById(id);
        if (job == null) {
            System.out.println("[!] Repair Job #" + id + " not found.");
            input.pressEnterToContinue();
            return;
        }

        boolean confirm = input.readBoolean("Are you sure you want to delete Repair Job #" + id + "?");
        if (confirm) {
            try {
                boolean deleted = RepairService.deleteRepairJob(id);
                if (deleted) {
                    System.out.println("\n[✓] Repair Job #" + id + " deleted successfully.");
                } else {
                    System.out.println("\n[!] Repair Job could not be deleted.");
                }
            } catch (IllegalStateException e) {
                System.out.println("\n[!] Cannot delete repair job: " + e.getMessage());
            }
        } else {
            System.out.println("Deletion cancelled.");
        }
        input.pressEnterToContinue();
    }

    private void printJobTable(List<RepairJob> list) {
        System.out.printf("%-6s | %-14s | %-18s | %-18s | %-16s | %-14s%n",
                "Job ID", "Status", "Customer", "Device", "Technician", "Received");
        System.out.println("-------+----------------+--------------------+--------------------+------------------+---------------");
        for (RepairJob j : list) {
            String cust = j.getCustomerName() != null ? j.getCustomerName() : "Cust #" + j.getCustomerId();
            String dev = j.getDeviceSummary() != null ? j.getDeviceSummary() : "Dev #" + j.getDeviceId();
            String tech = j.getTechnicianName() != null ? j.getTechnicianName() : "Unassigned";
            System.out.printf("#%-5d | %-14s | %-18s | %-18s | %-16s | %-14s%n",
                    j.getJobId(),
                    j.getCurrentStatus().getDisplayName(),
                    truncate(cust, 18),
                    truncate(dev, 18),
                    truncate(tech, 16),
                    j.getDateReceived());
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "-";
        return str.length() <= maxLen ? str : str.substring(0, maxLen - 3) + "...";
    }
}
