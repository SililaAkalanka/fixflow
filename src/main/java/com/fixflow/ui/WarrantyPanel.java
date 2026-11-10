package com.fixflow.ui;

import com.fixflow.database.WarrantyDB;
import com.fixflow.enums.UserRole;
import com.fixflow.enums.WarrantyStatus;
import com.fixflow.model.Warranty;
import com.fixflow.util.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 30-day warranty tracking and warranty claim processing panel.
 * Implements Section 7.7, Section 8.14, and Section 8.15 of the FixFlow Project Specification.
 */
public class WarrantyPanel extends BasePanel {

    private static final Logger LOGGER = Logger.getLogger(WarrantyPanel.class.getName());

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Warranty #", "Job #", "Customer", "Phone", "Device", "Serial #", "Start Date", "Expiry Date", "Days Left", "Status", "Original Problem"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };

    private final JTable warrantyTable = new JTable(tableModel);
    private final JTextField searchField = new JTextField(22);
    private final JComboBox<String> statusFilterCombo = new JComboBox<>(new String[]{"All Statuses", "ACTIVE", "EXPIRED", "CLAIMED"});
    private JButton fileClaimBtn;
    private JButton refreshBtn;

    public WarrantyPanel() {
        super("Warranty Management", "Verify 30-day repair warranty eligibility, monitor expirations, and initiate linked warranty claims");
        initUI();
    }

    private void initUI() {
        contentArea.setLayout(new BorderLayout(0, 15));

        // Top Filter Bar
        JPanel topBar = new JPanel(new BorderLayout(10, 0));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchField.putClientProperty("JTextField.placeholderText", "Search by customer, phone, serial #, or job #...");
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());

        statusFilterCombo.addActionListener(e -> performSearch());

        refreshBtn = new JButton("Reset");
        refreshBtn.addActionListener(e -> {
            searchField.setText("");
            statusFilterCombo.setSelectedIndex(0);
            loadWarranties();
        });

        searchPanel.add(new JLabel("Find:"));
        searchPanel.add(searchField);
        searchPanel.add(new JLabel("Status:"));
        searchPanel.add(statusFilterCombo);
        searchPanel.add(searchBtn);
        searchPanel.add(refreshBtn);

        // Action Buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        fileClaimBtn = new JButton("+ File Warranty Claim");
        fileClaimBtn.setFont(fileClaimBtn.getFont().deriveFont(Font.BOLD));
        fileClaimBtn.setBackground(new Color(210, 110, 0)); // Warm Amber
        fileClaimBtn.setForeground(Color.WHITE);
        fileClaimBtn.addActionListener(e -> handleFileWarrantyClaim());

        actionPanel.add(fileClaimBtn);

        topBar.add(searchPanel, BorderLayout.WEST);
        topBar.add(actionPanel, BorderLayout.EAST);

        // Main Warranty Table
        warrantyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        warrantyTable.setRowHeight(26);
        warrantyTable.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        warrantyTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        warrantyTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        warrantyTable.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);
        warrantyTable.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);
        warrantyTable.getColumnModel().getColumn(8).setCellRenderer(centerRenderer);

        // Custom status badge renderer
        warrantyTable.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFocus, int r, int c) {
                Component comp = super.getTableCellRendererComponent(table, val, isSel, hasFocus, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (!isSel && val != null) {
                    String s = val.toString();
                    setFont(getFont().deriveFont(Font.BOLD));
                    if ("ACTIVE".equalsIgnoreCase(s)) {
                        comp.setForeground(new Color(16, 124, 65)); // Emerald
                    } else if ("CLAIMED".equalsIgnoreCase(s)) {
                        comp.setForeground(new Color(210, 110, 0)); // Amber
                    } else {
                        comp.setForeground(Color.GRAY);
                    }
                }
                return comp;
            }
        });

        JScrollPane scrollPane = new JScrollPane(warrantyTable);

        contentArea.add(topBar, BorderLayout.NORTH);
        contentArea.add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void onPanelShown() {
        applyRolePermissions();
        loadWarranties();
    }

    private void applyRolePermissions() {
        UserRole role = SessionManager.getCurrentRole();
        boolean canClaim = (role == UserRole.ADMINISTRATOR || role == UserRole.MANAGER || role == UserRole.RECEPTIONIST);
        fileClaimBtn.setVisible(canClaim);
    }

    private void loadWarranties() {
        try {
            tableModel.setRowCount(0);
            List<Warranty> list = WarrantyDB.getAllWarranties();
            for (Warranty w : list) {
                addWarrantyRow(w);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load warranties", ex);
            JOptionPane.showMessageDialog(this, "Failed to load warranties: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        String status = (String) statusFilterCombo.getSelectedItem();
        if ("All Statuses".equalsIgnoreCase(status)) status = null;

        try {
            tableModel.setRowCount(0);
            List<Warranty> list = WarrantyDB.searchWarranties(query, status);
            for (Warranty w : list) {
                addWarrantyRow(w);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Warranty search failed", ex);
            JOptionPane.showMessageDialog(this, "Search error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addWarrantyRow(Warranty w) {
        long days = w.getDaysRemaining();
        String daysStr = w.getStatus() == WarrantyStatus.ACTIVE ? (days >= 0 ? days + " days" : "0 days") : "-";

        tableModel.addRow(new Object[]{
                w.getWarrantyId(),
                w.getJobId(),
                w.getCustomerName() != null ? w.getCustomerName() : "N/A",
                w.getCustomerPhone() != null ? w.getCustomerPhone() : "N/A",
                w.getDeviceSummary() != null ? w.getDeviceSummary() : "N/A",
                w.getSerialNumber() != null ? w.getSerialNumber() : "N/A",
                w.getStartDate(),
                w.getExpiryDate(),
                daysStr,
                w.getStatus().name(),
                w.getRepairProblem() != null ? w.getRepairProblem() : ""
        });
    }

    private void handleFileWarrantyClaim() {
        int selectedRow = warrantyTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an active warranty from the table to file a claim.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int warrantyId = (int) tableModel.getValueAt(selectedRow, 0);
        Warranty warranty;
        try {
            warranty = WarrantyDB.getWarrantyById(warrantyId);
            if (warranty == null) {
                JOptionPane.showMessageDialog(this, "Warranty #" + warrantyId + " not found.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to fetch warranty", ex);
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validate eligibility (§7.7, §8.15)
        if (!warranty.isCurrentlyValid()) {
            JOptionPane.showMessageDialog(this,
                    String.format("Warranty #%d is not eligible for a warranty claim.\n" +
                            "Status: %s\n" +
                            "Expiry Date: %s\n\n" +
                            "Claims are only valid for ACTIVE warranties within their 30-day window.",
                            warrantyId, warranty.getStatus().getDisplayName(), warranty.getExpiryDate()),
                    "Claim Ineligible", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Claim Dialog
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "File 30-Day Warranty Claim - Warranty #" + warrantyId, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(580, 440);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Details Block
        JPanel detailsPanel = new JPanel(new GridLayout(4, 2, 8, 6));
        detailsPanel.setBorder(new TitledBorder("Warranty Verification"));
        detailsPanel.add(new JLabel("Customer:"));
        detailsPanel.add(new JLabel(warranty.getCustomerName() + " (" + warranty.getCustomerPhone() + ")"));
        detailsPanel.add(new JLabel("Device:"));
        detailsPanel.add(new JLabel(warranty.getDeviceSummary() + " [SN: " + warranty.getSerialNumber() + "]"));
        detailsPanel.add(new JLabel("Original Repair:"));
        detailsPanel.add(new JLabel("Job #" + warranty.getJobId() + " (" + warranty.getRepairProblem() + ")"));
        detailsPanel.add(new JLabel("Warranty Coverage:"));
        detailsPanel.add(new JLabel("Valid until " + warranty.getExpiryDate() + " (" + warranty.getDaysRemaining() + " days remaining)"));

        // Claim Description Form
        JPanel formPanel = new JPanel(new BorderLayout(0, 8));
        formPanel.setBorder(new TitledBorder("Customer Reported Defect"));
        JLabel promptLabel = new JLabel("Describe the recurring problem or defect covered under this warranty:");
        JTextArea problemArea = new JTextArea(4, 20);
        problemArea.setLineWrap(true);
        problemArea.setWrapStyleWord(true);
        formPanel.add(promptLabel, BorderLayout.NORTH);
        formPanel.add(new JScrollPane(problemArea), BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 12));
        centerPanel.add(detailsPanel, BorderLayout.NORTH);
        centerPanel.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton submitBtn = new JButton("Submit Warranty Claim (Create Re-repair)");
        submitBtn.setBackground(new Color(210, 110, 0));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFont(submitBtn.getFont().deriveFont(Font.BOLD));

        submitBtn.addActionListener(e -> {
            String defect = problemArea.getText().trim();
            if (defect.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter the defect or reason reported by the customer for this warranty claim.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                String roleName = SessionManager.getCurrentRole() != null ? SessionManager.getCurrentRole().getDisplayName() : "Receptionist";
                int newJobId = WarrantyDB.claimWarranty(warrantyId, defect, roleName);

                dialog.dispose();
                loadWarranties();

                JOptionPane.showMessageDialog(this,
                        String.format("Warranty Claim Approved!\n\n" +
                                "New Warranty Re-repair Job #%d has been created.\n" +
                                "Linked to Original Repair Job #%d (§7.7, §8.15).\n" +
                                "Status: RECEIVED (Pre-approved under warranty).\n\n" +
                                "You can now assign a technician in the Repair Jobs panel.",
                                newJobId, warranty.getJobId()),
                        "Claim Processed", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to submit warranty claim", ex);
                JOptionPane.showMessageDialog(dialog, "Error processing claim: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(submitBtn);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.setVisible(true);
    }
}
