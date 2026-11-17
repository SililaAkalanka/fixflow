package com.fixflow.cli;

import com.fixflow.enums.RepairStatus;
import com.fixflow.model.DashboardMetrics;
import com.fixflow.model.SparePart;
import com.fixflow.service.InventoryService;
import com.fixflow.service.ReportService;

import java.util.List;

/**
 * CLI menu for viewing text-based operational, inventory, and revenue reports.
 */
public class ReportMenu {

    private final InputHandler input;

    public ReportMenu(InputHandler input) {
        this.input = input;
    }

    public void display() {
        boolean back = false;
        while (!back) {
            System.out.println("\n========================================");
            System.out.println("          REPORTS & ANALYTICS           ");
            System.out.println("========================================");
            System.out.println("1. Operational Dashboard Overview");
            System.out.println("2. Repair Jobs Grouped by Status");
            System.out.println("3. Financial Revenue & Outstanding Balance");
            System.out.println("4. Inventory Low-Stock Summary");
            System.out.println("0. Back to Main Menu");
            System.out.println("========================================");

            int choice = input.readInt("Enter choice: ", 0, 4);
            switch (choice) {
                case 1 -> showDashboard();
                case 2 -> showStatusBreakdown();
                case 3 -> showFinancialReport();
                case 4 -> showLowStockReport();
                case 0 -> back = true;
            }
        }
    }

    private void showDashboard() {
        DashboardMetrics m = ReportService.getDashboardMetrics();
        System.out.println("\n==================================================");
        System.out.println("       FIXFLOW OPERATIONAL DASHBOARD SUMMARY       ");
        System.out.println("==================================================");
        System.out.println("CUSTOMERS & STAFF:");
        System.out.printf("  Total Customers Registered   : %d%n", m.getTotalCustomers());
        System.out.printf("  Total Technicians Registered : %d%n", m.getTotalTechnicians());
        System.out.println("--------------------------------------------------");
        System.out.println("REPAIR JOBS LIFECYCLE:");
        System.out.printf("  Total Repair Jobs            : %d%n", m.getTotalJobs());
        System.out.printf("  Active Jobs (Diagnosing/Prog): %d%n", m.getActiveJobs());
        System.out.printf("  Completed (Ready for Pickup) : %d%n", m.getCompletedJobs());
        System.out.printf("  Collected by Customers       : %d%n", m.getCollectedJobs());
        System.out.printf("  Cancelled Jobs               : %d%n", m.getCancelledJobs());
        System.out.println("--------------------------------------------------");
        System.out.println("FINANCIAL REVENUE:");
        System.out.printf("  Total Revenue Collected      : LKR %.2f%n", m.getTotalRevenue());
        System.out.printf("  Total Outstanding Balance    : LKR %.2f%n", m.getPendingRevenue());
        System.out.println("--------------------------------------------------");
        System.out.println("INVENTORY STATUS:");
        System.out.printf("  Low-Stock Items (<= min)     : %d%n", m.getLowStockCount());
        System.out.printf("  Out-of-Stock Items (0 qty)   : %d%n", m.getOutOfStockCount());
        System.out.printf("  Total Inventory Valuation    : LKR %.2f%n", m.getTotalInventoryValuation());
        System.out.println("==================================================");
        input.pressEnterToContinue();
    }

    private void showStatusBreakdown() {
        DashboardMetrics m = ReportService.getDashboardMetrics();
        System.out.println("\n========================================");
        System.out.println("      REPAIR JOBS BY STATUS             ");
        System.out.println("========================================");
        for (RepairStatus s : RepairStatus.values()) {
            int count = m.getStatusCount(s);
            System.out.printf("  %-16s : %4d job(s)%n", s.getDisplayName(), count);
        }
        System.out.println("----------------------------------------");
        System.out.printf("  Total Repairs    : %4d%n", m.getTotalJobs());
        System.out.println("========================================");
        input.pressEnterToContinue();
    }

    private void showFinancialReport() {
        DashboardMetrics m = ReportService.getDashboardMetrics();
        System.out.println("\n========================================");
        System.out.println("     FINANCIAL & REVENUE REPORT         ");
        System.out.println("========================================");
        System.out.printf("Total Cash Collected   : LKR %12.2f%n", m.getTotalRevenue());
        System.out.printf("Outstanding Receivables: LKR %12.2f%n", m.getPendingRevenue());
        System.out.println("----------------------------------------");
        System.out.printf("Total Gross Invoiced   : LKR %12.2f%n", m.getTotalRevenue() + m.getPendingRevenue());
        System.out.println("========================================");
        input.pressEnterToContinue();
    }

    private void showLowStockReport() {
        System.out.println("\n========================================");
        System.out.println("       LOW STOCK INVENTORY REPORT       ");
        System.out.println("========================================");
        List<SparePart> low = InventoryService.getLowStockParts();
        if (low.isEmpty()) {
            System.out.println("[✓] All parts currently exceed minimum stock levels.");
        } else {
            System.out.printf("%-6s | %-28s | %-8s | %-8s%n", "ID", "Part Name", "Available", "Min Stock");
            System.out.println("-------+------------------------------+----------+----------");
            for (SparePart p : low) {
                System.out.printf("#%-5d | %-28s | %8d | %8d%n",
                        p.getPartId(),
                        p.getPartName().length() > 28 ? p.getPartName().substring(0, 25) + "..." : p.getPartName(),
                        p.getAvailableQuantity(),
                        p.getMinStockLevel());
            }
        }
        System.out.println("========================================");
        input.pressEnterToContinue();
    }
}
