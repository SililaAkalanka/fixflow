package com.fixflow.ui;

import com.fixflow.database.CustomerDB;
import com.fixflow.database.DeviceDB;
import com.fixflow.database.InventoryDB;
import com.fixflow.database.PaymentDB;
import com.fixflow.database.RepairDB;
import com.fixflow.database.TechnicianDB;
import com.fixflow.enums.RepairStatus;
import com.fixflow.enums.UserRole;
import com.fixflow.exception.InsufficientStockException;
import com.fixflow.exception.InvalidStatusTransitionException;
import com.fixflow.model.Customer;
import com.fixflow.model.Device;
import com.fixflow.model.Payment;
import com.fixflow.model.RepairJob;
import com.fixflow.model.RepairPartUsage;
import com.fixflow.model.SparePart;
import com.fixflow.model.Technician;
import com.fixflow.service.PaymentService;
import com.fixflow.service.RepairService;
import com.fixflow.util.PdfInvoiceGenerator;
import com.fixflow.util.SessionManager;
import com.fixflow.util.ValidationUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core repair management panel with simplified 6-status lifecycle.
 * Lifecycle: RECEIVED -> DIAGNOSING -> IN_PROGRESS -> COMPLETED -> COLLECTED
 *                                                  \-> CANCELLED
 */
public class RepairPanel extends BasePanel {

    private static final Logger LOGGER = Logger.getLogger(RepairPanel.class.getName());

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Job #", "Customer", "Device", "Assigned Tech", "Status", "Date Received", "Est. Cost (LKR)", "Labour (LKR)", "Notes"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable repairTable = new JTable(tableModel);
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> statusFilterCombo = new JComboBox<>(new String[]{
            "All Statuses", "Received", "Diagnosing", "In Progress", "Completed", "Collected", "Cancelled"
    });
    private final JCheckBox myJobsOnlyCheck = new JCheckBox("My Assigned Jobs Only");
    private final JLabel scopeLabel = new JLabel();

    private JButton newJobBtn;
    private JButton assignTechBtn;
    private JButton recordDiagnosisBtn;
    private JButton partsUsedBtn;
    private JButton updateStatusBtn;
    private JButton invoiceBtn;
    private JButton viewDetailsBtn;
    private JButton deleteJobBtn;

    public RepairPanel() {
        super("Repair Job Management", "Track repair lifecycle, technician assignments, diagnostics, spare parts, and collection");
        initUI();
    }

    private void initUI() {
        contentArea.setLayout(new BorderLayout(0, 15));

        // Top Toolbar
        JPanel topBar = new JPanel(new BorderLayout());

        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchField.putClientProperty("JTextField.placeholderText", "Search customer, device, job #...");
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());

        statusFilterCombo.addActionListener(e -> performSearch());
        myJobsOnlyCheck.addActionListener(e -> performSearch());

        JButton resetBtn = new JButton("Reset");
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            statusFilterCombo.setSelectedIndex(0);
            myJobsOnlyCheck.setSelected(SessionManager.hasRole(UserRole.TECHNICIAN));
            loadRepairJobs();
        });

        searchBox.add(searchField);
        searchBox.add(new JLabel("Status:"));
        searchBox.add(statusFilterCombo);
        searchBox.add(myJobsOnlyCheck);
        searchBox.add(searchBtn);
        searchBox.add(resetBtn);

        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        newJobBtn = new JButton("+ New Repair Job");
        newJobBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        newJobBtn.putClientProperty("JButton.buttonType", "roundRect");
        newJobBtn.addActionListener(e -> openCreateJobDialog());

        rightBox.add(scopeLabel);
        rightBox.add(newJobBtn);

        topBar.add(searchBox, BorderLayout.WEST);
        topBar.add(rightBox, BorderLayout.EAST);

        // Table setup
        repairTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        repairTable.setRowHeight(26);
        repairTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Color-coded status renderer
        repairTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    String statusStr = value.toString();
                    if (statusStr.contains("Received")) setForeground(new Color(70, 90, 150));
                    else if (statusStr.contains("Diagnosing")) setForeground(new Color(130, 40, 160));
                    else if (statusStr.contains("In Progress")) setForeground(new Color(0, 110, 210));
                    else if (statusStr.contains("Completed")) setForeground(new Color(16, 124, 65));
                    else if (statusStr.contains("Collected")) setForeground(new Color(100, 100, 100));
                    else if (statusStr.contains("Cancelled")) setForeground(new Color(180, 20, 20));
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(repairTable);

        // Bottom Action Bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottomBar.setBorder(new EmptyBorder(5, 0, 0, 0));

        viewDetailsBtn = new JButton("View Job Details & Notes");
        viewDetailsBtn.addActionListener(e -> handleViewDetails());

        partsUsedBtn = new JButton("Parts Used");
        partsUsedBtn.addActionListener(e -> handleManagePartsUsed());

        assignTechBtn = new JButton("Assign Technician");
        assignTechBtn.addActionListener(e -> handleAssignTechnician());

        recordDiagnosisBtn = new JButton("Record Diagnosis");
        recordDiagnosisBtn.addActionListener(e -> handleRecordDiagnosis());

        updateStatusBtn = new JButton("Update Status");
        updateStatusBtn.addActionListener(e -> handleUpdateStatus());

        invoiceBtn = new JButton("PDF Invoice");
        invoiceBtn.addActionListener(e -> handleExportPdfInvoice());

        deleteJobBtn = new JButton("Delete Job");
        deleteJobBtn.setForeground(new Color(200, 30, 30));
        deleteJobBtn.addActionListener(e -> handleDeleteJob());

        bottomBar.add(viewDetailsBtn);
        bottomBar.add(partsUsedBtn);
        bottomBar.add(assignTechBtn);
        bottomBar.add(recordDiagnosisBtn);
        bottomBar.add(updateStatusBtn);
        bottomBar.add(invoiceBtn);
        bottomBar.add(deleteJobBtn);

        contentArea.add(topBar, BorderLayout.NORTH);
        contentArea.add(scrollPane, BorderLayout.CENTER);
        contentArea.add(bottomBar, BorderLayout.SOUTH);
    }

    @Override
    public void onPanelShown() {
        applyRolePermissions();
        loadRepairJobs();
    }

    private void applyRolePermissions() {
        UserRole role = SessionManager.getCurrentRole();
        boolean isTech = role == UserRole.TECHNICIAN;
        boolean isAdmin = SessionManager.isAdmin();

        deleteJobBtn.setVisible(isAdmin);

        myJobsOnlyCheck.setVisible(isTech);
        if (isTech) {
            myJobsOnlyCheck.setSelected(true);
            scopeLabel.setText("Mode: Technician");
            scopeLabel.setForeground(new Color(0, 120, 215));
        } else {
            myJobsOnlyCheck.setSelected(false);
            scopeLabel.setText("Mode: Administrator");
            scopeLabel.setForeground(new Color(16, 124, 65));
        }
    }

    private void loadRepairJobs() {
        performSearch();
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        String statusStr = (String) statusFilterCombo.getSelectedItem();
        RepairStatus statusFilter = null;
        if (statusStr != null && !"All Statuses".equalsIgnoreCase(statusStr)) {
            String enumName = statusStr.toUpperCase().replace(" ", "_");
            try {
                statusFilter = RepairStatus.valueOf(enumName);
            } catch (IllegalArgumentException ignored) {}
        }

        Integer techFilter = null;
        if (myJobsOnlyCheck.isSelected() && SessionManager.hasRole(UserRole.TECHNICIAN)) {
            try {
                String userName = SessionManager.getCurrentUser().getUsername();
                List<Technician> techs = TechnicianDB.getAllTechnicians();
                for (Technician t : techs) {
                    if (t.getName().equalsIgnoreCase(userName) || t.getName().toLowerCase().contains(userName.toLowerCase())) {
                        techFilter = t.getTechnicianId();
                        break;
                    }
                }
            } catch (SQLException ignored) {}
        }

        tableModel.setRowCount(0);
        try {
            List<RepairJob> jobs = RepairDB.searchRepairJobs(query, statusFilter, techFilter);
            for (RepairJob j : jobs) {
                tableModel.addRow(new Object[]{
                        j.getJobId(),
                        j.getCustomerName() != null ? j.getCustomerName() : ("Cust #" + j.getCustomerId()),
                        j.getDeviceSummary() != null ? j.getDeviceSummary() : ("Dev #" + j.getDeviceId()),
                        j.getTechnicianName() != null ? j.getTechnicianName() : "Unassigned",
                        j.getCurrentStatus().getDisplayName(),
                        j.getDateReceived(),
                        j.getEstimatedCost() != null ? String.format("%.2f", j.getEstimatedCost()) : "–",
                        String.format("%.2f", j.getLabourCharges()),
                        j.getNotes() != null ? j.getNotes() : "–"
                });
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error searching repair jobs", ex);
            JOptionPane.showMessageDialog(this, "Failed to load repair jobs: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Integer getSelectedJobId() {
        int selectedRow = repairTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a repair job from the table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return (Integer) tableModel.getValueAt(selectedRow, 0);
    }

    private void handleAssignTechnician() {
        Integer jobId = getSelectedJobId();
        if (jobId == null) return;

        try {
            List<Technician> techs = TechnicianDB.getActiveTechnicians();
            if (techs.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No active technicians are registered.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            JComboBox<TechnicianItem> techCombo = new JComboBox<>();
            for (Technician t : techs) {
                techCombo.addItem(new TechnicianItem(t.getTechnicianId(), t.getName() + " (" + (t.getSpecialization() != null ? t.getSpecialization() : "General") + ")"));
            }

            int result = JOptionPane.showConfirmDialog(
                    this,
                    techCombo,
                    "Assign Technician to Job #" + jobId,
                    JOptionPane.OK_CANCEL_OPTION
            );

            if (result == JOptionPane.OK_OPTION) {
                TechnicianItem selected = (TechnicianItem) techCombo.getSelectedItem();
                if (selected != null) {
                    RepairService.assignTechnician(jobId, selected.techId);
                    JOptionPane.showMessageDialog(this, "Technician successfully assigned to Job #" + jobId + ".", "Assignment Saved", JOptionPane.INFORMATION_MESSAGE);
                    loadRepairJobs();
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error assigning technician", ex);
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRecordDiagnosis() {
        Integer jobId = getSelectedJobId();
        if (jobId == null) return;

        try {
            RepairJob job = RepairDB.getRepairJobById(jobId);
            if (job == null) return;

            if (!canUserModifyJob(job)) {
                JOptionPane.showMessageDialog(this, "Permission Denied: Technicians can only update repair jobs assigned to them.", "Restricted Action", JOptionPane.WARNING_MESSAGE);
                return;
            }

            openDiagnosisDialog(job);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error fetching job for diagnosis", ex);
        }
    }

    private void handleUpdateStatus() {
        Integer jobId = getSelectedJobId();
        if (jobId == null) return;

        try {
            RepairJob job = RepairDB.getRepairJobById(jobId);
            if (job == null) return;

            if (!canUserModifyJob(job)) {
                JOptionPane.showMessageDialog(this, "Permission Denied: Technicians can only update repair jobs assigned to them.", "Restricted Action", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Set<RepairStatus> nextStatuses = job.getValidNextStatuses();
            if (nextStatuses.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Job #" + jobId + " is in terminal state '" + job.getCurrentStatus().getDisplayName() + "' and cannot transition further.", "Terminal Status", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JComboBox<RepairStatus> statusCombo = new JComboBox<>(nextStatuses.toArray(new RepairStatus[0]));
            JTextField notesField = new JTextField(20);

            JPanel panel = new JPanel(new GridLayout(0, 1, 4, 8));
            panel.add(new JLabel("Current Status: " + job.getCurrentStatus().getDisplayName()));
            panel.add(new JLabel("Transition To:"));
            panel.add(statusCombo);
            panel.add(new JLabel("Notes / Reason (Required if cancelling):"));
            panel.add(notesField);

            int result = JOptionPane.showConfirmDialog(this, panel, "Update Status for Job #" + jobId, JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                RepairStatus selectedTarget = (RepairStatus) statusCombo.getSelectedItem();
                String note = notesField.getText().trim();

                try {
                    RepairService.updateStatus(jobId, selectedTarget, note);
                    JOptionPane.showMessageDialog(this, "Job #" + jobId + " transitioned to '" + selectedTarget.getDisplayName() + "'.", "Status Updated", JOptionPane.INFORMATION_MESSAGE);
                    loadRepairJobs();
                } catch (InvalidStatusTransitionException | IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
                } catch (IllegalStateException ex) {
                    // Collection payment guard
                    int payChoice = JOptionPane.showConfirmDialog(this,
                            ex.getMessage() + "\n\nWould you like to open the cash payment dialog now?",
                            "Payment Required Before Collection", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (payChoice == JOptionPane.YES_OPTION) {
                        PaymentPanel paymentPanel = new PaymentPanel();
                        paymentPanel.showRecordPaymentDialog(job);
                        loadRepairJobs();
                    }
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error updating status", ex);
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleExportPdfInvoice() {
        Integer jobId = getSelectedJobId();
        if (jobId == null) return;

        try {
            List<Payment> payments = PaymentDB.getPaymentsByJobId(jobId);
            if (payments.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No payment records found for Job #" + jobId + ".\n" +
                        "At least one payment must be recorded before an invoice or receipt can be generated.",
                        "Payment Required", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            Payment latestPayment = payments.get(payments.size() - 1);

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save FixFlow PDF Invoice");
            chooser.setSelectedFile(new File("Invoice_Job-" + jobId + ".pdf"));

            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File targetFile = chooser.getSelectedFile();
                if (!targetFile.getName().toLowerCase().endsWith(".pdf")) {
                    targetFile = new File(targetFile.getAbsolutePath() + ".pdf");
                }

                PdfInvoiceGenerator.generateInvoice(latestPayment.getPaymentId(), targetFile);

                int openChoice = JOptionPane.showConfirmDialog(this,
                        "PDF invoice successfully generated and saved to:\n" + targetFile.getAbsolutePath() +
                        "\n\nWould you like to open the invoice now?",
                        "Invoice Generated", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

                if (openChoice == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().open(targetFile);
                    } catch (Exception openEx) {
                        LOGGER.log(Level.WARNING, "Failed to open PDF file automatically", openEx);
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to generate invoice", ex);
            JOptionPane.showMessageDialog(this, "Failed to generate PDF invoice: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleViewDetails() {
        Integer jobId = getSelectedJobId();
        if (jobId == null) return;

        try {
            RepairJob job = RepairDB.getRepairJobById(jobId);
            if (job == null) return;

            double totalCost = PaymentService.calculateTotalCost(jobId);
            double totalPaid = PaymentService.getTotalPaid(jobId);
            double balance = PaymentService.getRemainingBalance(jobId);
            String payStatus = PaymentService.getPaymentStatus(jobId).getDisplayName();

            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Job #" + jobId + " Details", true);
            dialog.setSize(600, 520);
            dialog.setLocationRelativeTo(this);

            JPanel panel = new JPanel(new BorderLayout(0, 12));
            panel.setBorder(new EmptyBorder(15, 20, 15, 20));

            // Job Summary Overview
            JPanel summary = new JPanel(new GridLayout(0, 2, 8, 8));
            summary.setBorder(new TitledBorder("Repair Job Overview"));
            summary.add(new JLabel("Customer: " + job.getCustomerName()));
            summary.add(new JLabel("Phone: " + job.getCustomerPhone()));
            summary.add(new JLabel("Device: " + job.getDeviceSummary()));
            summary.add(new JLabel("Technician: " + (job.getTechnicianName() != null ? job.getTechnicianName() : "Unassigned")));
            summary.add(new JLabel("Status: " + job.getCurrentStatus().getDisplayName()));
            summary.add(new JLabel("Date Received: " + job.getDateReceived()));
            summary.add(new JLabel("Labour Charges: LKR " + String.format("%.2f", job.getLabourCharges())));
            summary.add(new JLabel("Total Repair Cost: LKR " + String.format("%.2f", totalCost)));
            summary.add(new JLabel("Total Paid: LKR " + String.format("%.2f", totalPaid)));
            summary.add(new JLabel("Balance Due: LKR " + String.format("%.2f", balance) + " (" + payStatus + ")"));

            // Detailed Text Areas
            JPanel textPanel = new JPanel(new GridLayout(3, 1, 6, 6));
            textPanel.setBorder(new TitledBorder("Technical Notes & Findings"));

            JTextArea probArea = new JTextArea("Reported Problem:\n" + job.getInitialProblem());
            probArea.setEditable(false);
            probArea.setLineWrap(true);
            textPanel.add(new JScrollPane(probArea));

            JTextArea diagArea = new JTextArea("Diagnosis Findings:\n" + (job.getDiagnosisFindings() != null ? job.getDiagnosisFindings() : "None recorded."));
            diagArea.setEditable(false);
            diagArea.setLineWrap(true);
            textPanel.add(new JScrollPane(diagArea));

            JTextArea notesArea = new JTextArea("Repair Notes:\n" + (job.getNotes() != null ? job.getNotes() : "None."));
            notesArea.setEditable(false);
            notesArea.setLineWrap(true);
            textPanel.add(new JScrollPane(notesArea));

            JButton closeBtn = new JButton("Close");
            closeBtn.addActionListener(e -> dialog.dispose());
            JPanel btnP = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            btnP.add(closeBtn);

            panel.add(summary, BorderLayout.NORTH);
            panel.add(textPanel, BorderLayout.CENTER);
            panel.add(btnP, BorderLayout.SOUTH);

            dialog.setContentPane(panel);
            dialog.setVisible(true);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error viewing job details", ex);
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteJob() {
        Integer jobId = getSelectedJobId();
        if (jobId == null) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete Repair Job #" + jobId + "?\n" +
                "Note: Deletion will be rejected if payment records are associated.",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean deleted = RepairService.deleteRepairJob(jobId);
                if (deleted) {
                    JOptionPane.showMessageDialog(this, "Repair Job #" + jobId + " deleted.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                    loadRepairJobs();
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "Deletion blocked", ex);
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Deletion Blocked", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private boolean canUserModifyJob(RepairJob job) {
        UserRole role = SessionManager.getCurrentRole();
        if (role == UserRole.ADMINISTRATOR) {
            return true;
        }
        if (role == UserRole.TECHNICIAN) {
            if (job.getTechnicianId() == null) return true; // Can take unassigned job
            try {
                Technician tech = TechnicianDB.getTechnicianById(job.getTechnicianId());
                String userName = SessionManager.getCurrentUser().getUsername();
                return tech != null && (tech.getName().equalsIgnoreCase(userName) || tech.getName().toLowerCase().contains(userName.toLowerCase()));
            } catch (SQLException e) {
                return false;
            }
        }
        return false;
    }

    private void openCreateJobDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Intake – Create New Repair Job", true);
        dialog.setSize(520, 500);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 10));

        JComboBox<CustomerItem> custCombo = new JComboBox<>();
        JComboBox<DeviceItem> devCombo = new JComboBox<>();
        JComboBox<TechnicianItem> techCombo = new JComboBox<>();
        techCombo.addItem(new TechnicianItem(0, "Leave Unassigned for now"));

        try {
            List<Customer> customers = CustomerDB.getAllCustomers();
            for (Customer c : customers) {
                custCombo.addItem(new CustomerItem(c.getCustomerId(), c.getFullName() + " (ID: #" + c.getCustomerId() + ")"));
            }

            List<Technician> techs = TechnicianDB.getActiveTechnicians();
            for (Technician t : techs) {
                techCombo.addItem(new TechnicianItem(t.getTechnicianId(), t.getName() + " (" + (t.getSpecialization() != null ? t.getSpecialization() : "General") + ")"));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error loading intake dropdowns", ex);
        }

        custCombo.addActionListener(e -> {
            devCombo.removeAllItems();
            CustomerItem selected = (CustomerItem) custCombo.getSelectedItem();
            if (selected != null) {
                try {
                    Device dev = DeviceDB.getDeviceByCustomerId(selected.customerId);
                    if (dev != null) {
                        devCombo.addItem(new DeviceItem(dev.getDeviceId(), dev.getBrand() + " " + dev.getModel() + " (" + dev.getCategory() + ")"));
                    }
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error loading customer device", ex);
                }
            }
        });

        if (custCombo.getItemCount() > 0) {
            custCombo.setSelectedIndex(0);
        }

        JTextArea problemArea = new JTextArea(3, 20);
        problemArea.setLineWrap(true);
        problemArea.setWrapStyleWord(true);
        JScrollPane probScroll = new JScrollPane(problemArea);

        JTextField labourField = new JTextField("0.0");
        JTextField notesField = new JTextField();

        form.add(new JLabel("Customer:*"));
        form.add(custCombo);
        form.add(new JLabel("Registered Device:*"));
        form.add(devCombo);
        form.add(new JLabel("Assign Technician:"));
        form.add(techCombo);
        form.add(new JLabel("Labour Charges (LKR):"));
        form.add(labourField);
        form.add(new JLabel("Initial Notes:"));
        form.add(notesField);
        form.add(new JLabel("Initial Problem Description:*"));
        form.add(probScroll);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton createBtn = new JButton("Create Repair Job");
        createBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        createBtn.addActionListener(e -> {
            CustomerItem selectedCust = (CustomerItem) custCombo.getSelectedItem();
            DeviceItem selectedDev = (DeviceItem) devCombo.getSelectedItem();
            String problem = problemArea.getText().trim();
            String labourStr = labourField.getText().trim();
            String notes = notesField.getText().trim();

            if (selectedCust == null) {
                JOptionPane.showMessageDialog(dialog, "Please select a customer.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (selectedDev == null) {
                JOptionPane.showMessageDialog(dialog, "Selected customer has no registered device. Please register a device first.", "Device Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!ValidationUtil.isNotEmpty(problem)) {
                JOptionPane.showMessageDialog(dialog, "Please describe the initial reported problem.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double labour = 0.0;
            if (!labourStr.isEmpty()) {
                try {
                    labour = Double.parseDouble(labourStr);
                    if (labour < 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "Labour charges must be a valid non-negative number.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            try {
                RepairJob newJob = new RepairJob(selectedCust.customerId, selectedDev.deviceId, problem);
                newJob.setLabourCharges(labour);
                newJob.setNotes(notes);

                TechnicianItem selectedTech = (TechnicianItem) techCombo.getSelectedItem();
                if (selectedTech != null && selectedTech.techId > 0) {
                    newJob.setTechnicianId(selectedTech.techId);
                }

                int newId = RepairService.createRepairJob(newJob);

                JOptionPane.showMessageDialog(dialog, "Repair Job #" + newId + " created successfully (Status: Received).", "Intake Complete", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadRepairJobs();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Failed to create repair job", ex);
                JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(createBtn);

        panel.add(form, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void openDiagnosisDialog(RepairJob job) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Record Diagnosis for Job #" + job.getJobId(), true);
        dialog.setSize(480, 400);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 10));

        JTextArea findingsArea = new JTextArea(job.getDiagnosisFindings() != null ? job.getDiagnosisFindings() : "", 3, 20);
        findingsArea.setLineWrap(true);
        findingsArea.setWrapStyleWord(true);
        JScrollPane findingsScroll = new JScrollPane(findingsArea);

        JTextField estCostField = new JTextField(job.getEstimatedCost() != null ? String.valueOf(job.getEstimatedCost()) : "");
        JTextField labourField = new JTextField(String.valueOf(job.getLabourCharges()));
        JTextField estDateField = new JTextField(job.getEstimatedCompletionDate() != null ? job.getEstimatedCompletionDate() : "");
        estDateField.putClientProperty("JTextField.placeholderText", "YYYY-MM-DD");

        form.add(new JLabel("Diagnosis Findings:*"));
        form.add(findingsScroll);
        form.add(new JLabel("Estimated Total Cost (LKR):*"));
        form.add(estCostField);
        form.add(new JLabel("Labour Charges (LKR):"));
        form.add(labourField);
        form.add(new JLabel("Est. Completion Date:"));
        form.add(estDateField);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton("Save Diagnosis");
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveBtn.addActionListener(e -> {
            String findings = findingsArea.getText().trim();
            String estCostStr = estCostField.getText().trim();
            String labourStr = labourField.getText().trim();
            String estDate = estDateField.getText().trim();

            if (!ValidationUtil.isNotEmpty(findings)) {
                JOptionPane.showMessageDialog(dialog, "Diagnosis findings are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double estCost;
            try {
                estCost = Double.parseDouble(estCostStr);
                if (estCost < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid non-negative estimated cost.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double labour = 0.0;
            if (!labourStr.isEmpty()) {
                try {
                    labour = Double.parseDouble(labourStr);
                    if (labour < 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, "Labour charges must be a valid non-negative number.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            try {
                RepairService.recordDiagnosis(job.getJobId(), findings, estCost, labour, estDate);

                // If currently RECEIVED, auto-advance to DIAGNOSING
                if (job.getCurrentStatus() == RepairStatus.RECEIVED) {
                    RepairService.updateStatus(job.getJobId(), RepairStatus.DIAGNOSING, "Diagnosis initiated.");
                }

                JOptionPane.showMessageDialog(dialog, "Diagnosis and estimate recorded successfully.", "Saved", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadRepairJobs();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to record diagnosis", ex);
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);

        panel.add(form, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void handleManagePartsUsed() {
        int selectedRow = repairTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a repair job first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int jobId = (int) tableModel.getValueAt(selectedRow, 0);
        RepairJob job;
        try {
            job = RepairDB.getRepairJobById(jobId);
            if (job == null) {
                JOptionPane.showMessageDialog(this, "Repair job not found.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve job", ex);
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Parts Issued - Job #" + jobId, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(780, 500);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 12));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Header with Job Details
        JPanel headerPanel = new JPanel(new GridLayout(2, 2, 10, 5));
        headerPanel.setBorder(new TitledBorder("Job Information"));
        headerPanel.add(new JLabel("Customer: " + (job.getCustomerName() != null ? job.getCustomerName() : "N/A")));
        headerPanel.add(new JLabel("Device: " + (job.getDeviceSummary() != null ? job.getDeviceSummary() : "N/A")));
        headerPanel.add(new JLabel("Status: " + job.getCurrentStatus().getDisplayName()));
        headerPanel.add(new JLabel("Assigned Tech: " + (job.getTechnicianName() != null ? job.getTechnicianName() : "Unassigned")));
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Table for parts used
        DefaultTableModel partsModel = new DefaultTableModel(
                new Object[]{"Usage ID", "Part ID", "Part Name", "Category", "Quantity", "Unit Price (LKR)", "Subtotal (LKR)", "Date Used"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable partsTable = new JTable(partsModel);
        partsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        partsTable.getColumnModel().getColumn(0).setMaxWidth(70);
        partsTable.getColumnModel().getColumn(1).setMaxWidth(70);

        JLabel totalCostLabel = new JLabel("Total Parts Cost: LKR 0.00");
        totalCostLabel.setFont(totalCostLabel.getFont().deriveFont(Font.BOLD, 14f));
        totalCostLabel.setForeground(new Color(0, 102, 204));

        Runnable refreshParts = () -> {
            try {
                partsModel.setRowCount(0);
                List<RepairPartUsage> usages = InventoryDB.getPartsUsedForJob(jobId);
                double totalCost = 0.0;
                for (RepairPartUsage u : usages) {
                    double subtotal = u.getSubtotal();
                    totalCost += subtotal;
                    partsModel.addRow(new Object[]{
                            u.getUsageId(),
                            u.getPartId(),
                            u.getPartName(),
                            u.getPartCategory(),
                            u.getQuantity(),
                            String.format("%.2f", u.getSellingPriceAtTime()),
                            String.format("%.2f", subtotal),
                            u.getDateUsed()
                    });
                }
                totalCostLabel.setText(String.format("Total Parts Cost: LKR %.2f", totalCost));
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Failed to load parts used", ex);
                JOptionPane.showMessageDialog(dialog, "Error loading parts: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        refreshParts.run();

        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.add(new JScrollPane(partsTable), BorderLayout.CENTER);
        centerPanel.add(totalCostLabel, BorderLayout.SOUTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Actions panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton addPartBtn = new JButton("+ Issue Part to Job");
        JButton removePartBtn = new JButton("Remove Part (Return to Stock)");
        JButton closeBtn = new JButton("Close");

        boolean jobActive = (job.getCurrentStatus() != RepairStatus.COLLECTED && job.getCurrentStatus() != RepairStatus.CANCELLED);

        addPartBtn.setEnabled(jobActive);
        removePartBtn.setEnabled(jobActive);

        addPartBtn.addActionListener(e -> showIssuePartDialog(dialog, jobId, refreshParts));

        removePartBtn.addActionListener(e -> {
            int row = partsTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(dialog, "Please select a part usage record to remove.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int usageId = (int) partsModel.getValueAt(row, 0);
            String partName = (String) partsModel.getValueAt(row, 2);
            int qty = (int) partsModel.getValueAt(row, 4);

            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Are you sure you want to return " + qty + "x \"" + partName + "\" to inventory stock?",
                    "Confirm Part Return", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    boolean ok = InventoryDB.removePartUsage(usageId);
                    if (ok) {
                        JOptionPane.showMessageDialog(dialog, "Part usage removed and stock restored successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        refreshParts.run();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Record not found.", "Warning", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Failed to remove part usage", ex);
                    JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        closeBtn.addActionListener(e -> dialog.dispose());

        actionPanel.add(addPartBtn);
        actionPanel.add(removePartBtn);
        actionPanel.add(closeBtn);
        mainPanel.add(actionPanel, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.setVisible(true);
    }

    private void showIssuePartDialog(JDialog parent, int jobId, Runnable onSaved) {
        List<SparePart> availableParts;
        try {
            availableParts = InventoryDB.getAllParts().stream()
                    .filter(p -> p.getAvailableQuantity() > 0)
                    .toList();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load parts", ex);
            JOptionPane.showMessageDialog(parent, "Failed to load parts catalog: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (availableParts.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No spare parts currently have available stock in the catalog.", "Out of Stock", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog issueDialog = new JDialog(parent, "Issue Spare Part to Job #" + jobId, Dialog.ModalityType.APPLICATION_MODAL);
        issueDialog.setSize(520, 260);
        issueDialog.setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel form = new JPanel(new GridLayout(3, 2, 10, 10));

        JComboBox<SparePartItem> partCombo = new JComboBox<>();
        for (SparePart p : availableParts) {
            partCombo.addItem(new SparePartItem(p));
        }

        JLabel stockAvailLabel = new JLabel();
        JLabel unitPriceLabel = new JLabel();
        JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));

        Runnable updatePartInfo = () -> {
            SparePartItem selected = (SparePartItem) partCombo.getSelectedItem();
            if (selected != null) {
                SparePart p = selected.part();
                stockAvailLabel.setText("In Stock: " + p.getAvailableQuantity() + " units");
                unitPriceLabel.setText(String.format("Unit Price: LKR %.2f", p.getSellingPrice()));
                ((SpinnerNumberModel) qtySpinner.getModel()).setMaximum(p.getAvailableQuantity());
            }
        };

        partCombo.addActionListener(e -> updatePartInfo.run());
        updatePartInfo.run();

        form.add(new JLabel("Select Part:"));
        form.add(partCombo);
        form.add(new JLabel("Stock & Pricing:"));
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        infoPanel.add(stockAvailLabel);
        infoPanel.add(unitPriceLabel);
        form.add(infoPanel);
        form.add(new JLabel("Quantity to Issue:"));
        form.add(qtySpinner);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> issueDialog.dispose());
        JButton issueBtn = new JButton("Confirm Issue");
        issueBtn.addActionListener(e -> {
            SparePartItem selected = (SparePartItem) partCombo.getSelectedItem();
            if (selected == null) return;
            int qty = (int) qtySpinner.getValue();
            try {
                String roleName = SessionManager.getCurrentRole() != null ? SessionManager.getCurrentRole().getDisplayName() : "Technician";
                InventoryDB.issuePartToJob(jobId, selected.part().getPartId(), qty, roleName);
                JOptionPane.showMessageDialog(issueDialog, "Part issued successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                issueDialog.dispose();
                onSaved.run();
            } catch (InsufficientStockException ex) {
                JOptionPane.showMessageDialog(issueDialog, ex.getMessage(), "Insufficient Stock", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Failed to issue part", ex);
                JOptionPane.showMessageDialog(issueDialog, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(issueBtn);

        panel.add(form, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        issueDialog.setContentPane(panel);
        issueDialog.setVisible(true);
    }

    private record SparePartItem(SparePart part) {
        @Override
        public String toString() {
            return part.getPartName() + " (" + part.getCategory() + ") - LKR " + String.format("%.2f", part.getSellingPrice());
        }
    }

    private record CustomerItem(int customerId, String label) {
        @Override
        public String toString() { return label; }
    }

    private record DeviceItem(int deviceId, String label) {
        @Override
        public String toString() { return label; }
    }

    private record TechnicianItem(int techId, String label) {
        @Override
        public String toString() { return label; }
    }
}
