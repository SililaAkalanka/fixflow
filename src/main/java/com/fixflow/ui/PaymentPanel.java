package com.fixflow.ui;

import com.fixflow.database.PaymentDB;
import com.fixflow.database.RepairDB;
import com.fixflow.enums.RepairStatus;
import com.fixflow.model.Payment;
import com.fixflow.model.RepairJob;
import com.fixflow.service.PaymentService;
import com.fixflow.util.PdfInvoiceGenerator;
import com.fixflow.util.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Payment processing and PDF invoice management panel.
 * Supports multiple partial cash payments per repair job.
 */
public class PaymentPanel extends BasePanel {

    private static final Logger LOGGER = Logger.getLogger(PaymentPanel.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Payment #", "Job #", "Customer", "Phone", "Device", "Amount Paid (LKR)", "Method", "Notes", "Payment Date"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };

    private final JTable paymentTable = new JTable(tableModel);
    private final JTextField searchField = new JTextField(20);
    private JButton recordPaymentBtn;
    private JButton generatePdfBtn;
    private JButton refreshBtn;

    public PaymentPanel() {
        super("Payments & Invoices", "Process partial and full cash payments, track remaining balances, and export PDF invoices");
        initUI();
    }

    private void initUI() {
        contentArea.setLayout(new BorderLayout(0, 15));

        // Top Filter Bar
        JPanel topBar = new JPanel(new BorderLayout(10, 0));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchField.putClientProperty("JTextField.placeholderText", "Search by customer, phone, or job #...");
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());

        refreshBtn = new JButton("Reset");
        refreshBtn.addActionListener(e -> {
            searchField.setText("");
            loadPayments();
        });

        searchPanel.add(new JLabel("Find:"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(refreshBtn);

        // Action Buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        recordPaymentBtn = new JButton("+ Record Cash Payment");
        recordPaymentBtn.setFont(recordPaymentBtn.getFont().deriveFont(Font.BOLD));
        recordPaymentBtn.setBackground(new Color(16, 124, 65));
        recordPaymentBtn.setForeground(Color.WHITE);
        recordPaymentBtn.addActionListener(e -> showRecordPaymentDialog(null));

        generatePdfBtn = new JButton("Generate PDF Invoice");
        generatePdfBtn.addActionListener(e -> handleGeneratePdfInvoice());

        actionPanel.add(recordPaymentBtn);
        actionPanel.add(generatePdfBtn);

        topBar.add(searchPanel, BorderLayout.WEST);
        topBar.add(actionPanel, BorderLayout.EAST);

        // Main Payments Table
        paymentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        paymentTable.setRowHeight(26);
        paymentTable.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        paymentTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        paymentTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        paymentTable.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(paymentTable);

        contentArea.add(topBar, BorderLayout.NORTH);
        contentArea.add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void onPanelShown() {
        loadPayments();
    }

    private void loadPayments() {
        try {
            tableModel.setRowCount(0);
            List<Payment> list = PaymentDB.getAllPayments();
            for (Payment p : list) {
                tableModel.addRow(new Object[]{
                        p.getPaymentId(),
                        p.getJobId(),
                        p.getCustomerName() != null ? p.getCustomerName() : "N/A",
                        p.getCustomerPhone() != null ? p.getCustomerPhone() : "N/A",
                        p.getDeviceSummary() != null ? p.getDeviceSummary() : "N/A",
                        String.format("%.2f", p.getAmountPaid()),
                        p.getPaymentMethod(),
                        p.getNotes() != null ? p.getNotes() : "–",
                        p.getPaymentDate()
                });
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load payments", ex);
            JOptionPane.showMessageDialog(this, "Failed to load payments: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performSearch() {
        String query = searchField.getText().trim().toLowerCase();
        try {
            tableModel.setRowCount(0);
            List<Payment> list = PaymentDB.getAllPayments();
            for (Payment p : list) {
                boolean matches = query.isEmpty() ||
                        String.valueOf(p.getJobId()).contains(query) ||
                        String.valueOf(p.getPaymentId()).contains(query) ||
                        (p.getCustomerName() != null && p.getCustomerName().toLowerCase().contains(query)) ||
                        (p.getCustomerPhone() != null && p.getCustomerPhone().contains(query));
                if (matches) {
                    tableModel.addRow(new Object[]{
                            p.getPaymentId(),
                            p.getJobId(),
                            p.getCustomerName() != null ? p.getCustomerName() : "N/A",
                            p.getCustomerPhone() != null ? p.getCustomerPhone() : "N/A",
                            p.getDeviceSummary() != null ? p.getDeviceSummary() : "N/A",
                            String.format("%.2f", p.getAmountPaid()),
                            p.getPaymentMethod(),
                            p.getNotes() != null ? p.getNotes() : "–",
                            p.getPaymentDate()
                    });
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Search failed", ex);
            JOptionPane.showMessageDialog(this, "Search error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Opens the cash payment recording dialog with partial payment support.
     *
     * @param preselectedJob optional job to pay (or null to show picker)
     */
    public void showRecordPaymentDialog(RepairJob preselectedJob) {
        List<RepairJob> payableJobs = new ArrayList<>();
        try {
            List<RepairJob> allJobs = RepairDB.getAllRepairJobs();
            for (RepairJob j : allJobs) {
                if (j.getCurrentStatus() != RepairStatus.CANCELLED) {
                    double balance = PaymentService.getRemainingBalance(j.getJobId());
                    if (balance > 0.001) {
                        payableJobs.add(j);
                    }
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load payable jobs", ex);
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (payableJobs.isEmpty() && preselectedJob == null) {
            JOptionPane.showMessageDialog(this,
                    "There are currently no repair jobs with an outstanding balance.",
                    "No Outstanding Balance", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Record Cash Payment", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(580, 530);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JComboBox<JobItem> jobCombo = new JComboBox<>();
        if (preselectedJob != null) {
            jobCombo.addItem(new JobItem(preselectedJob));
        } else {
            for (RepairJob j : payableJobs) {
                jobCombo.addItem(new JobItem(j));
            }
        }

        // Breakdown panel
        JPanel detailsPanel = new JPanel(new GridLayout(6, 2, 8, 8));
        detailsPanel.setBorder(new TitledBorder("Payment & Balance Overview"));

        JLabel custLabel = new JLabel("-");
        JLabel devLabel = new JLabel("-");
        JLabel totalCostLabel = new JLabel("LKR 0.00");
        JLabel totalPaidLabel = new JLabel("LKR 0.00");
        JLabel balanceLabel = new JLabel("LKR 0.00");
        balanceLabel.setFont(balanceLabel.getFont().deriveFont(Font.BOLD, 14f));
        balanceLabel.setForeground(new Color(200, 40, 40));

        detailsPanel.add(new JLabel("Customer:"));
        detailsPanel.add(custLabel);
        detailsPanel.add(new JLabel("Device:"));
        detailsPanel.add(devLabel);
        detailsPanel.add(new JLabel("Total Repair Cost:"));
        detailsPanel.add(totalCostLabel);
        detailsPanel.add(new JLabel("Already Paid:"));
        detailsPanel.add(totalPaidLabel);
        detailsPanel.add(new JLabel("OUTSTANDING BALANCE:"));
        detailsPanel.add(balanceLabel);
        detailsPanel.add(new JLabel("Payment Method:"));
        detailsPanel.add(new JLabel("CASH"));

        // Inputs panel
        JTextField amountField = new JTextField(10);
        JTextField tenderedField = new JTextField(10);
        JTextField notesField = new JTextField(15);
        JLabel changeLabel = new JLabel("LKR 0.00");
        changeLabel.setFont(changeLabel.getFont().deriveFont(Font.BOLD, 13f));
        changeLabel.setForeground(new Color(16, 124, 65));

        final double[] currentBalance = {0.0};

        Runnable updateJobDetails = () -> {
            JobItem selected = (JobItem) jobCombo.getSelectedItem();
            if (selected != null) {
                RepairJob j = selected.job();
                custLabel.setText(j.getCustomerName() != null ? j.getCustomerName() : "N/A");
                devLabel.setText(j.getDeviceSummary() != null ? j.getDeviceSummary() : "N/A");
                try {
                    double total = PaymentService.calculateTotalCost(j.getJobId());
                    double paid = PaymentService.getTotalPaid(j.getJobId());
                    double balance = PaymentService.getRemainingBalance(j.getJobId());
                    currentBalance[0] = balance;

                    totalCostLabel.setText(String.format("LKR %.2f", total));
                    totalPaidLabel.setText(String.format("LKR %.2f", paid));
                    balanceLabel.setText(String.format("LKR %.2f", balance));

                    // Default payment amount to full remaining balance
                    amountField.setText(String.format("%.2f", balance));
                    tenderedField.setText(String.format("%.2f", balance));
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Failed to load balance", ex);
                }
            }
        };

        jobCombo.addActionListener(e -> updateJobDetails.run());
        updateJobDetails.run();

        // Calculate change on input change
        DocumentListener changeCalcListener = new DocumentListener() {
            private void updateChange() {
                try {
                    double amount = Double.parseDouble(amountField.getText().trim());
                    double tendered = Double.parseDouble(tenderedField.getText().trim());
                    double change = tendered - amount;
                    if (change >= 0) {
                        changeLabel.setText(String.format("LKR %.2f", change));
                        changeLabel.setForeground(new Color(16, 124, 65));
                    } else {
                        changeLabel.setText(String.format("Underpaid by LKR %.2f", Math.abs(change)));
                        changeLabel.setForeground(Color.RED);
                    }
                } catch (NumberFormatException e) {
                    changeLabel.setText("Invalid amount");
                    changeLabel.setForeground(Color.RED);
                }
            }
            @Override public void insertUpdate(DocumentEvent e) { updateChange(); }
            @Override public void removeUpdate(DocumentEvent e) { updateChange(); }
            @Override public void changedUpdate(DocumentEvent e) { updateChange(); }
        };

        amountField.getDocument().addDocumentListener(changeCalcListener);
        tenderedField.getDocument().addDocumentListener(changeCalcListener);

        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 8, 8));
        inputPanel.setBorder(new TitledBorder("Cash Payment Details"));
        inputPanel.add(new JLabel("Payment Amount (LKR):*"));
        inputPanel.add(amountField);
        inputPanel.add(new JLabel("Cash Tendered (LKR):"));
        inputPanel.add(tenderedField);
        inputPanel.add(new JLabel("Customer Change:"));
        inputPanel.add(changeLabel);
        inputPanel.add(new JLabel("Payment Notes (Optional):"));
        inputPanel.add(notesField);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        JPanel topSelect = new JPanel(new BorderLayout(8, 0));
        topSelect.add(new JLabel("Select Repair Job:"), BorderLayout.WEST);
        topSelect.add(jobCombo, BorderLayout.CENTER);

        centerPanel.add(topSelect, BorderLayout.NORTH);
        centerPanel.add(detailsPanel, BorderLayout.CENTER);
        centerPanel.add(inputPanel, BorderLayout.SOUTH);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton confirmBtn = new JButton("Record Cash Payment");
        confirmBtn.setBackground(new Color(16, 124, 65));
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFont(confirmBtn.getFont().deriveFont(Font.BOLD));

        confirmBtn.addActionListener(e -> {
            JobItem selected = (JobItem) jobCombo.getSelectedItem();
            if (selected == null) return;

            double amountToPay;
            try {
                amountToPay = Double.parseDouble(amountField.getText().trim());
                if (amountToPay <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Payment amount must be greater than zero.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (amountToPay > currentBalance[0] + 0.001) {
                    JOptionPane.showMessageDialog(dialog,
                            String.format("Payment amount (LKR %.2f) cannot exceed outstanding balance (LKR %.2f). Overpayment is not allowed.",
                                    amountToPay, currentBalance[0]),
                            "Overpayment Prohibited", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid numeric payment amount.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                String notes = notesField.getText().trim();
                int paymentId = PaymentService.recordPayment(selected.job().getJobId(), amountToPay, null, notes);

                dialog.dispose();
                loadPayments();

                int askPdf = JOptionPane.showConfirmDialog(this,
                        String.format("Payment #%d for LKR %.2f recorded successfully!\nChange Due: %s\n\nWould you like to export the PDF invoice / receipt now?",
                                paymentId, amountToPay, changeLabel.getText()),
                        "Payment Complete", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

                if (askPdf == JOptionPane.YES_OPTION) {
                    exportPdfInvoice(paymentId);
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to record payment", ex);
                JOptionPane.showMessageDialog(dialog, "Error recording payment: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(confirmBtn);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        dialog.setVisible(true);
    }

    private void handleGeneratePdfInvoice() {
        int row = paymentTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a payment record from the table first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int paymentId = (int) tableModel.getValueAt(row, 0);
        exportPdfInvoice(paymentId);
    }

    private void exportPdfInvoice(int paymentId) {
        try {
            Payment payment = PaymentDB.getPaymentById(paymentId);
            if (payment == null) {
                JOptionPane.showMessageDialog(this, "Payment record not found.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save FixFlow PDF Invoice");
            chooser.setSelectedFile(new File("Invoice_INV-" + String.format("%05d", paymentId) + "_Job-" + payment.getJobId() + ".pdf"));

            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File targetFile = chooser.getSelectedFile();
                if (!targetFile.getName().toLowerCase().endsWith(".pdf")) {
                    targetFile = new File(targetFile.getAbsolutePath() + ".pdf");
                }

                PdfInvoiceGenerator.generateInvoice(paymentId, targetFile);

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
            LOGGER.log(Level.SEVERE, "Failed to generate PDF invoice", ex);
            JOptionPane.showMessageDialog(this, "Failed to generate PDF invoice: " + ex.getMessage(), "PDF Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private record JobItem(RepairJob job) {
        @Override
        public String toString() {
            return "Job #" + job.getJobId() + " - " + (job.getCustomerName() != null ? job.getCustomerName() : "Customer") +
                    " (" + (job.getDeviceSummary() != null ? job.getDeviceSummary() : "Device") + ") [" +
                    job.getCurrentStatus().getDisplayName() + "]";
        }
    }
}
