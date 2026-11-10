package com.fixflow.ui;

import com.fixflow.database.InventoryDB;
import com.fixflow.database.ReportDB;
import com.fixflow.enums.RepairStatus;
import com.fixflow.model.Payment;
import com.fixflow.model.RepairJob;
import com.fixflow.model.SparePart;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * In-application operational and financial reporting panel.
 * Implements Section 7.9 of the FixFlow Project Specification.
 */
public class ReportsPanel extends BasePanel {

    private static final Logger LOGGER = Logger.getLogger(ReportsPanel.class.getName());

    private final JComboBox<String> reportTypeCombo = new JComboBox<>(new String[]{
            "1. Repair Jobs & Turnaround Report",
            "2. Revenue & Cash Collection Summary",
            "3. Inventory Stock & Valuation Report"
    });

    // Date range filter components
    private final JComboBox<String> dateRangePresets = new JComboBox<>(new String[]{
            "All Time", "Today", "Last 7 Days", "Last 30 Days", "Custom Range"
    });
    private final JTextField dateFromField = new JTextField(9);
    private final JTextField dateToField = new JTextField(9);

    // Specific filters
    private final JComboBox<String> repairStatusCombo = new JComboBox<>(new String[]{
            "All Statuses", "Received", "Diagnosing", "Waiting for Approval",
            "Waiting for Parts", "Repairing", "Ready for Collection", "Collected", "Cancelled"
    });

    private final JComboBox<String> invStockFilterCombo = new JComboBox<>(new String[]{
            "All Parts", "Low Stock Critical Only", "Out of Stock Only", "In Stock Only"
    });

    private final JPanel filterCardContainer = new JPanel(new CardLayout());
    private final JPanel tableCardContainer = new JPanel(new CardLayout());

    // Report Tables & Models
    private final DefaultTableModel repairTableModel = new DefaultTableModel(
            new Object[]{"Job #", "Received", "Customer", "Phone", "Device", "Technician", "Status", "Approval", "Est. Cost ($)", "Labour ($)", "Completed"}, 0
    ) { @Override public boolean isCellEditable(int r, int c) { return false; } };
    private final JTable repairTable = new JTable(repairTableModel);

    private final DefaultTableModel revenueTableModel = new DefaultTableModel(
            new Object[]{"Payment #", "Payment Date", "Job #", "Customer", "Phone", "Device", "Labour ($)", "Parts ($)", "Settled Total ($)", "Method", "Status"}, 0
    ) { @Override public boolean isCellEditable(int r, int c) { return false; } };
    private final JTable revenueTable = new JTable(revenueTableModel);

    private final DefaultTableModel inventoryTableModel = new DefaultTableModel(
            new Object[]{"Part ID", "Part Name", "Category", "Compatible Model", "Cost ($)", "Price ($)", "Qty", "Min", "Status", "Valuation ($)", "Retail Potential ($)"}, 0
    ) { @Override public boolean isCellEditable(int r, int c) { return false; } };
    private final JTable inventoryTable = new JTable(inventoryTableModel);

    // Summary KPI Footer Labels
    private final JLabel footerMetric1 = new JLabel();
    private final JLabel footerMetric2 = new JLabel();
    private final JLabel footerMetric3 = new JLabel();
    private final JLabel footerMetric4 = new JLabel();

    public ReportsPanel() {
        super("Operational & Financial Reports", "In-depth operational reporting for repairs, revenue settlements, and inventory valuations");
        initUI();
    }

    private void initUI() {
        contentArea.setLayout(new BorderLayout(0, 14));

        // 1. Top Controls Bar
        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 224, 230), 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JPanel leftSelect = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftSelect.add(new JLabel("Active Report:"));
        reportTypeCombo.setFont(reportTypeCombo.getFont().deriveFont(Font.BOLD));
        leftSelect.add(reportTypeCombo);

        JButton generateBtn = new JButton("Generate Report");
        generateBtn.setBackground(new Color(0, 102, 204));
        generateBtn.setForeground(Color.WHITE);
        generateBtn.setFont(generateBtn.getFont().deriveFont(Font.BOLD));
        generateBtn.addActionListener(e -> generateCurrentReport());

        topBar.add(leftSelect, BorderLayout.WEST);
        topBar.add(generateBtn, BorderLayout.EAST);

        // 2. Filters Sub-Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterBar.add(new JLabel("Period:"));
        filterBar.add(dateRangePresets);
        filterBar.add(new JLabel("From (YYYY-MM-DD):"));
        filterBar.add(dateFromField);
        filterBar.add(new JLabel("To:"));
        filterBar.add(dateToField);

        dateFromField.setEnabled(false);
        dateToField.setEnabled(false);
        dateRangePresets.addActionListener(e -> handleDatePresetChange());

        // Dynamic extra filters per report type
        JPanel repairFilterCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        repairFilterCard.add(new JLabel("Status:"));
        repairFilterCard.add(repairStatusCombo);

        JPanel revenueFilterCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        revenueFilterCard.add(new JLabel("Method: CASH ONLY (§8.12)"));

        JPanel inventoryFilterCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        inventoryFilterCard.add(new JLabel("Stock Filter:"));
        inventoryFilterCard.add(invStockFilterCombo);

        filterCardContainer.add(repairFilterCard, "REPAIR");
        filterCardContainer.add(revenueFilterCard, "REVENUE");
        filterCardContainer.add(inventoryFilterCard, "INVENTORY");

        filterBar.add(filterCardContainer);

        JPanel headerStack = new JPanel(new BorderLayout(0, 8));
        headerStack.add(topBar, BorderLayout.NORTH);
        headerStack.add(filterBar, BorderLayout.SOUTH);

        // 3. Center Tables
        configureTableAppearance(repairTable);
        configureTableAppearance(revenueTable);
        configureTableAppearance(inventoryTable);

        tableCardContainer.add(new JScrollPane(repairTable), "REPAIR");
        tableCardContainer.add(new JScrollPane(revenueTable), "REVENUE");
        tableCardContainer.add(new JScrollPane(inventoryTable), "INVENTORY");

        reportTypeCombo.addActionListener(e -> {
            int idx = reportTypeCombo.getSelectedIndex();
            CardLayout fl = (CardLayout) filterCardContainer.getLayout();
            CardLayout tl = (CardLayout) tableCardContainer.getLayout();
            if (idx == 0) {
                fl.show(filterCardContainer, "REPAIR");
                tl.show(tableCardContainer, "REPAIR");
                dateRangePresets.setEnabled(true);
            } else if (idx == 1) {
                fl.show(filterCardContainer, "REVENUE");
                tl.show(tableCardContainer, "REVENUE");
                dateRangePresets.setEnabled(true);
            } else {
                fl.show(filterCardContainer, "INVENTORY");
                tl.show(tableCardContainer, "INVENTORY");
                dateRangePresets.setEnabled(false);
                dateFromField.setText("");
                dateToField.setText("");
            }
            generateCurrentReport();
        });

        // 4. Bottom Summary Footer
        JPanel footerBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 6));
        footerBar.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 224, 230), 1, true),
                new EmptyBorder(6, 12, 6, 12)
        ));

        footerMetric1.setFont(new Font("Segoe UI", Font.BOLD, 12));
        footerMetric2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        footerMetric3.setFont(new Font("Segoe UI", Font.BOLD, 12));
        footerMetric4.setFont(new Font("Segoe UI", Font.BOLD, 12));
        footerMetric4.setForeground(new Color(16, 124, 65));

        footerBar.add(footerMetric1);
        footerBar.add(footerMetric2);
        footerBar.add(footerMetric3);
        footerBar.add(footerMetric4);

        contentArea.add(headerStack, BorderLayout.NORTH);
        contentArea.add(tableCardContainer, BorderLayout.CENTER);
        contentArea.add(footerBar, BorderLayout.SOUTH);
    }

    private void configureTableAppearance(JTable table) {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
    }

    private void handleDatePresetChange() {
        String preset = (String) dateRangePresets.getSelectedItem();
        LocalDate today = LocalDate.now();
        if ("Today".equalsIgnoreCase(preset)) {
            dateFromField.setText(today.toString());
            dateToField.setText(today.toString());
            dateFromField.setEnabled(false);
            dateToField.setEnabled(false);
        } else if ("Last 7 Days".equalsIgnoreCase(preset)) {
            dateFromField.setText(today.minusDays(7).toString());
            dateToField.setText(today.toString());
            dateFromField.setEnabled(false);
            dateToField.setEnabled(false);
        } else if ("Last 30 Days".equalsIgnoreCase(preset)) {
            dateFromField.setText(today.minusDays(30).toString());
            dateToField.setText(today.toString());
            dateFromField.setEnabled(false);
            dateToField.setEnabled(false);
        } else if ("Custom Range".equalsIgnoreCase(preset)) {
            dateFromField.setEnabled(true);
            dateToField.setEnabled(true);
        } else { // All Time
            dateFromField.setText("");
            dateToField.setText("");
            dateFromField.setEnabled(false);
            dateToField.setEnabled(false);
        }
    }

    @Override
    public void onPanelShown() {
        generateCurrentReport();
    }

    public void generateCurrentReport() {
        int idx = reportTypeCombo.getSelectedIndex();
        if (idx == 0) {
            generateRepairReport();
        } else if (idx == 1) {
            generateRevenueReport();
        } else {
            generateInventoryReport();
        }
    }

    private void generateRepairReport() {
        String from = dateFromField.getText().trim();
        String to = dateToField.getText().trim();

        String statusStr = (String) repairStatusCombo.getSelectedItem();
        RepairStatus status = null;
        if (statusStr != null && !"All Statuses".equalsIgnoreCase(statusStr)) {
            try {
                status = RepairStatus.valueOf(statusStr.toUpperCase().replace(" ", "_"));
            } catch (Exception ignored) {}
        }

        try {
            repairTableModel.setRowCount(0);
            List<RepairJob> jobs = ReportDB.getRepairJobsReport(from, to, status, null);

            int total = jobs.size();
            int completed = 0;
            int cancelled = 0;
            double totalEst = 0.0;
            double totalLabour = 0.0;

            for (RepairJob j : jobs) {
                if (j.getCurrentStatus() == RepairStatus.COLLECTED || j.getCurrentStatus() == RepairStatus.READY_FOR_COLLECTION) {
                    completed++;
                } else if (j.getCurrentStatus() == RepairStatus.CANCELLED) {
                    cancelled++;
                }

                double est = j.getEstimatedCost() != null ? j.getEstimatedCost() : 0.0;
                double labour = j.getLabourCharges();
                totalEst += est;
                totalLabour += labour;

                repairTableModel.addRow(new Object[]{
                        j.getJobId(),
                        j.getDateReceived(),
                        j.getCustomerName() != null ? j.getCustomerName() : "N/A",
                        j.getCustomerPhone() != null ? j.getCustomerPhone() : "N/A",
                        j.getDeviceSummary() != null ? j.getDeviceSummary() : "N/A",
                        j.getTechnicianName() != null ? j.getTechnicianName() : "Unassigned",
                        j.getCurrentStatus().getDisplayName(),
                        j.getApprovalStatus(),
                        String.format("%.2f", est),
                        String.format("%.2f", labour),
                        j.getActualCompletionDate() != null ? j.getActualCompletionDate() : "-"
                });
            }

            footerMetric1.setText("Total Jobs in Report: " + total);
            footerMetric2.setText("Completed / Ready: " + completed);
            footerMetric3.setText("Cancelled: " + cancelled);
            footerMetric4.setText(String.format("Total Labour Billed: $%.2f", totalLabour));

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to generate repair report", ex);
            JOptionPane.showMessageDialog(this, "Failed to generate repair report: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generateRevenueReport() {
        String from = dateFromField.getText().trim();
        String to = dateToField.getText().trim();

        try {
            revenueTableModel.setRowCount(0);
            List<Payment> payments = ReportDB.getRevenueReport(from, to);

            int count = payments.size();
            double totalLabour = 0.0;
            double totalParts = 0.0;
            double grandTotal = 0.0;

            for (Payment p : payments) {
                totalLabour += p.getLabourCharges();
                totalParts += p.getPartsCost();
                grandTotal += p.getAmountPaid();

                revenueTableModel.addRow(new Object[]{
                        p.getPaymentId(),
                        p.getPaymentDate(),
                        p.getJobId(),
                        p.getCustomerName() != null ? p.getCustomerName() : "N/A",
                        p.getCustomerPhone() != null ? p.getCustomerPhone() : "N/A",
                        p.getDeviceSummary() != null ? p.getDeviceSummary() : "N/A",
                        String.format("%.2f", p.getLabourCharges()),
                        String.format("%.2f", p.getPartsCost()),
                        String.format("%.2f", p.getAmountPaid()),
                        p.getPaymentMethod(),
                        p.getPaymentStatus()
                });
            }

            footerMetric1.setText("Settled Transactions: " + count);
            footerMetric2.setText(String.format("Total Labour Revenue: $%.2f", totalLabour));
            footerMetric3.setText(String.format("Total Spare Parts Revenue: $%.2f", totalParts));
            footerMetric4.setText(String.format("TOTAL CASH COLLECTED: $%.2f", grandTotal));

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to generate revenue report", ex);
            JOptionPane.showMessageDialog(this, "Failed to generate revenue report: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generateInventoryReport() {
        String filter = (String) invStockFilterCombo.getSelectedItem();
        String stockCode = "ALL";
        if ("Low Stock Critical Only".equalsIgnoreCase(filter)) stockCode = "LOW_STOCK";
        else if ("Out of Stock Only".equalsIgnoreCase(filter)) stockCode = "OUT_OF_STOCK";
        else if ("In Stock Only".equalsIgnoreCase(filter)) stockCode = "IN_STOCK";

        try {
            inventoryTableModel.setRowCount(0);
            List<SparePart> parts = ReportDB.getInventoryReport(null, stockCode);

            int uniqueParts = parts.size();
            int totalUnits = 0;
            int lowStockCount = 0;
            double totalValuation = 0.0;
            double retailPotential = 0.0;

            for (SparePart p : parts) {
                totalUnits += p.getAvailableQuantity();
                if (p.isLowStock()) lowStockCount++;

                double val = p.getAvailableQuantity() * p.getPurchaseCost();
                double retail = p.getAvailableQuantity() * p.getSellingPrice();
                totalValuation += val;
                retailPotential += retail;

                String status = p.getAvailableQuantity() == 0 ? "Out of Stock" : (p.isLowStock() ? "Low Stock" : "In Stock");

                inventoryTableModel.addRow(new Object[]{
                        p.getPartId(),
                        p.getPartName(),
                        p.getCategory(),
                        p.getCompatibleDeviceModel(),
                        String.format("%.2f", p.getPurchaseCost()),
                        String.format("%.2f", p.getSellingPrice()),
                        p.getAvailableQuantity(),
                        p.getMinStockLevel(),
                        status,
                        String.format("%.2f", val),
                        String.format("%.2f", retail)
                });
            }

            footerMetric1.setText("Catalog Items: " + uniqueParts);
            footerMetric2.setText("Total Units in Stock: " + totalUnits);
            footerMetric3.setText("Low Stock / Critical: " + lowStockCount);
            footerMetric4.setText(String.format("Inventory Valuation: $%.2f (Retail: $%.2f)", totalValuation, retailPotential));

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to generate inventory report", ex);
            JOptionPane.showMessageDialog(this, "Failed to generate inventory report: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
