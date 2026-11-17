package com.fixflow.cli;

import com.fixflow.exception.InsufficientStockException;
import com.fixflow.model.RepairJob;
import com.fixflow.model.SparePart;
import com.fixflow.service.InventoryService;
import com.fixflow.service.RepairService;
import com.fixflow.util.SessionManager;

import java.util.List;

/**
 * CLI menu for Inventory and Spare Part management.
 * Demonstrates InsufficientStockException exception handling.
 */
public class InventoryMenu {

    private final InputHandler input;

    public InventoryMenu(InputHandler input) {
        this.input = input;
    }

    public void display() {
        boolean back = false;
        while (!back) {
            System.out.println("\n========================================");
            System.out.println("          INVENTORY MANAGEMENT          ");
            System.out.println("========================================");
            System.out.println("1. Add New Spare Part");
            System.out.println("2. List All Spare Parts");
            System.out.println("3. Search Spare Parts");
            System.out.println("4. Add / Replenish Stock");
            System.out.println("5. Issue Spare Part to Repair Job");
            System.out.println("6. View Low-Stock Alerts");
            System.out.println("7. Update Spare Part Details");
            System.out.println("8. Delete Spare Part");
            System.out.println("0. Back to Main Menu");
            System.out.println("========================================");

            int choice = input.readInt("Enter choice: ", 0, 8);
            switch (choice) {
                case 1 -> addPart();
                case 2 -> listParts();
                case 3 -> searchParts();
                case 4 -> addStock();
                case 5 -> issuePartToJob();
                case 6 -> viewLowStockAlerts();
                case 7 -> updatePart();
                case 8 -> deletePart();
                case 0 -> back = true;
            }
        }
    }

    private void addPart() {
        System.out.println("\n--- Add New Spare Part ---");
        String name = input.readString("Enter Part Name: ");
        String category = input.readString("Enter Category (e.g. Display, Battery, Charging, IC): ");
        String model = input.readString("Enter Compatible Device Model (or 'Universal'): ");
        double purchase = input.readDouble("Enter Purchase Cost (LKR): ", 0, 1000000);
        double selling = input.readDouble("Enter Selling Price (LKR): ", purchase, 1000000);
        int qty = input.readInt("Enter Initial Quantity: ", 0, 10000);
        int minStock = input.readInt("Enter Minimum Stock Threshold (for alerts): ", 1, 1000);

        try {
            SparePart part = new SparePart(name, category, model, purchase, selling, qty, minStock);
            int id = InventoryService.addPart(part);
            System.out.println("\n[✓] Spare Part registered with Part ID #" + id);
        } catch (IllegalArgumentException e) {
            System.out.println("\n[!] Registration failed: " + e.getMessage());
        }
        input.pressEnterToContinue();
    }

    private void listParts() {
        System.out.println("\n--- Inventory Parts Catalog ---");
        List<SparePart> list = InventoryService.getAllParts();
        if (list.isEmpty()) {
            System.out.println("No spare parts in inventory.");
        } else {
            printPartTable(list);
        }
        input.pressEnterToContinue();
    }

    private void searchParts() {
        System.out.println("\n--- Search Spare Parts ---");
        String query = input.readString("Enter search keyword (name, category, or model): ");
        List<SparePart> results = InventoryService.searchParts(query);
        if (results.isEmpty()) {
            System.out.println("No matching spare parts found for: '" + query + "'");
        } else {
            System.out.println("Found " + results.size() + " matching part(s):");
            printPartTable(results);
        }
        input.pressEnterToContinue();
    }

    private void addStock() {
        System.out.println("\n--- Add / Replenish Stock ---");
        int id = input.readInt("Enter Part ID: ");
        SparePart part = InventoryService.getPartById(id);
        if (part == null) {
            System.out.println("[!] Part #" + id + " not found.");
            input.pressEnterToContinue();
            return;
        }

        System.out.printf("Part: %s (Current Available Stock: %d)%n", part.getPartName(), part.getAvailableQuantity());
        int addQty = input.readInt("Enter quantity to add to stock: ", 1, 10000);

        try {
            InventoryService.addStock(id, addQty);
            SparePart updated = InventoryService.getPartById(id);
            System.out.printf("[✓] Stock updated successfully. New Available Quantity: %d%n", updated.getAvailableQuantity());
        } catch (Exception e) {
            System.out.println("[!] Failed to add stock: " + e.getMessage());
        }
        input.pressEnterToContinue();
    }

    /**
     * Demonstrates checked exception handling (InsufficientStockException).
     */
    private void issuePartToJob() {
        System.out.println("\n--- Issue Spare Part to Repair Job ---");
        int jobId = input.readInt("Enter Repair Job ID: ");
        RepairJob job = RepairService.getRepairJobById(jobId);
        if (job == null) {
            System.out.println("[!] Repair Job #" + jobId + " not found.");
            input.pressEnterToContinue();
            return;
        }

        if (job.getCurrentStatus().isTerminal()) {
            System.out.println("[!] Cannot issue parts to a " + job.getCurrentStatus() + " repair job.");
            input.pressEnterToContinue();
            return;
        }

        int partId = input.readInt("Enter Spare Part ID to issue: ");
        SparePart part = InventoryService.getPartById(partId);
        if (part == null) {
            System.out.println("[!] Part #" + partId + " not found.");
            input.pressEnterToContinue();
            return;
        }

        System.out.printf("Part selected: %s | Unit Selling Price: LKR %.2f | Available: %d%n",
                part.getPartName(), part.getSellingPrice(), part.getAvailableQuantity());
        int quantity = input.readInt("Enter quantity to issue: ", 1, 1000);

        String role = SessionManager.getCurrentRole() != null ? SessionManager.getCurrentRole().getDisplayName() : "Technician";

        // Catch and handle InsufficientStockException explicitly
        try {
            InventoryService.issuePartToJob(jobId, partId, quantity, role);
            System.out.printf("%n[✓] Successfully issued %dx '%s' to Repair Job #%d.%n",
                    quantity, part.getPartName(), jobId);
            System.out.printf("    Total line cost: LKR %.2f (deducted from inventory stock).%n",
                    quantity * part.getSellingPrice());
        } catch (InsufficientStockException e) {
            System.out.println("\n[!] STOCK EXCEPTION: " + e.getMessage());
            System.out.println("    Operation aborted. Stock was not modified.");
        } catch (Exception e) {
            System.out.println("\n[!] Error issuing part: " + e.getMessage());
        }
        input.pressEnterToContinue();
    }

    private void viewLowStockAlerts() {
        System.out.println("\n--- Low Stock & Out of Stock Alerts ---");
        List<SparePart> lowList = InventoryService.getLowStockParts();
        if (lowList.isEmpty()) {
            System.out.println("[✓] All inventory items are adequately stocked above their minimum thresholds.");
        } else {
            System.out.println("ALERT: The following " + lowList.size() + " item(s) need replenishment:");
            printPartTable(lowList);
        }
        input.pressEnterToContinue();
    }

    private void updatePart() {
        System.out.println("\n--- Update Spare Part Details ---");
        int id = input.readInt("Enter Part ID: ");
        SparePart part = InventoryService.getPartById(id);
        if (part == null) {
            System.out.println("[!] Part #" + id + " not found.");
            input.pressEnterToContinue();
            return;
        }

        System.out.println("Current Name: " + part.getPartName());
        String name = input.readOptionalString("New Name (Enter to keep): ");
        if (!name.isEmpty()) part.setPartName(name);

        System.out.printf("Current Purchase Cost: LKR %.2f%n", part.getPurchaseCost());
        String purchaseStr = input.readOptionalString("New Purchase Cost (Enter to keep): ");
        if (!purchaseStr.isEmpty()) {
            try {
                part.setPurchaseCost(Double.parseDouble(purchaseStr));
            } catch (NumberFormatException ignored) {}
        }

        System.out.printf("Current Selling Price: LKR %.2f%n", part.getSellingPrice());
        String sellStr = input.readOptionalString("New Selling Price (Enter to keep): ");
        if (!sellStr.isEmpty()) {
            try {
                part.setSellingPrice(Double.parseDouble(sellStr));
            } catch (NumberFormatException ignored) {}
        }

        System.out.printf("Current Min Stock Level: %d%n", part.getMinStockLevel());
        String minStr = input.readOptionalString("New Min Stock Level (Enter to keep): ");
        if (!minStr.isEmpty()) {
            try {
                part.setMinStockLevel(Integer.parseInt(minStr));
            } catch (NumberFormatException ignored) {}
        }

        try {
            boolean updated = InventoryService.updatePart(part);
            if (updated) {
                System.out.println("\n[✓] Part #" + id + " updated successfully.");
            } else {
                System.out.println("\n[!] Failed to update part.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("\n[!] Update failed: " + e.getMessage());
        }
        input.pressEnterToContinue();
    }

    private void deletePart() {
        System.out.println("\n--- Delete Spare Part ---");
        int id = input.readInt("Enter Part ID to delete: ");
        SparePart part = InventoryService.getPartById(id);
        if (part == null) {
            System.out.println("[!] Part #" + id + " not found.");
            input.pressEnterToContinue();
            return;
        }

        boolean confirm = input.readBoolean("Are you sure you want to delete part '" + part.getPartName() + "'?");
        if (confirm) {
            try {
                boolean deleted = InventoryService.deletePart(id);
                if (deleted) {
                    System.out.println("\n[✓] Spare Part #" + id + " deleted successfully.");
                } else {
                    System.out.println("\n[!] Spare Part could not be deleted.");
                }
            } catch (IllegalStateException e) {
                System.out.println("\n[!] Cannot delete part: " + e.getMessage());
            }
        } else {
            System.out.println("Deletion cancelled.");
        }
        input.pressEnterToContinue();
    }

    private void printPartTable(List<SparePart> list) {
        System.out.printf("%-6s | %-28s | %-12s | %-12s | %-12s | %-8s | %-10s%n",
                "ID", "Part Name", "Category", "Cost (LKR)", "Price (LKR)", "Stock", "Status");
        System.out.println("-------+------------------------------+--------------+--------------+--------------+----------+-----------");
        for (SparePart p : list) {
            String status = p.isOutOfStock() ? "OUT OF STOCK" : (p.isLowStock() ? "LOW STOCK" : "OK");
            System.out.printf("#%-5d | %-28s | %-12s | %12.2f | %12.2f | %8d | %-10s%n",
                    p.getPartId(),
                    truncate(p.getPartName(), 28),
                    truncate(p.getCategory(), 12),
                    p.getPurchaseCost(),
                    p.getSellingPrice(),
                    p.getAvailableQuantity(),
                    status);
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "-";
        return str.length() <= maxLen ? str : str.substring(0, maxLen - 3) + "...";
    }
}
