package com.fixflow.ui;

import com.fixflow.database.CustomerDB;
import com.fixflow.database.DeviceDB;
import com.fixflow.enums.NavigationItem;
import com.fixflow.enums.UserRole;
import com.fixflow.model.Customer;
import com.fixflow.model.Device;
import com.fixflow.util.SessionManager;
import com.fixflow.util.ValidationUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Customer directory panel with full CRUD, search, and device linkage.
 * Implements Section 7.1 of the FixFlow Project Specification.
 */
public class CustomerPanel extends BasePanel {

    private static final Logger LOGGER = Logger.getLogger(CustomerPanel.class.getName());

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Full Name", "Phone", "Email", "Address", "Registered Date"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable customerTable = new JTable(tableModel);
    private final JTextField searchField = new JTextField(20);
    private final JLabel accessBadge = new JLabel();

    private JButton registerBtn;
    private JButton editBtn;
    private JButton deleteBtn;
    private JButton viewDeviceBtn;

    public CustomerPanel() {
        super("Customer Directory", "Register, update, and search customer records and associated device details");
        initUI();
    }

    private void initUI() {
        contentArea.setLayout(new BorderLayout(0, 15));

        // Top Toolbar
        JPanel topBar = new JPanel(new BorderLayout());

        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchField.putClientProperty("JTextField.placeholderText", "Search by name, phone, or ID...");
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());

        JButton resetBtn = new JButton("Reset");
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            loadCustomers();
        });

        searchBox.add(searchField);
        searchBox.add(searchBtn);
        searchBox.add(resetBtn);

        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        registerBtn = new JButton("+ Register Customer");
        registerBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        registerBtn.putClientProperty("JButton.buttonType", "roundRect");
        registerBtn.addActionListener(e -> openCustomerDialog(null));

        rightBox.add(accessBadge);
        rightBox.add(registerBtn);

        topBar.add(searchBox, BorderLayout.WEST);
        topBar.add(rightBox, BorderLayout.EAST);

        // Table in ScrollPane
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customerTable.setRowHeight(26);
        customerTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        JScrollPane scrollPane = new JScrollPane(customerTable);

        // Bottom Action Bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomBar.setBorder(new EmptyBorder(5, 0, 0, 0));

        viewDeviceBtn = new JButton("View Associated Device");
        viewDeviceBtn.addActionListener(e -> handleViewDevice());

        editBtn = new JButton("Edit Customer");
        editBtn.addActionListener(e -> handleEditCustomer());

        deleteBtn = new JButton("Delete Record");
        deleteBtn.setForeground(new Color(200, 30, 30));
        deleteBtn.addActionListener(e -> handleDeleteCustomer());

        bottomBar.add(viewDeviceBtn);
        bottomBar.add(editBtn);
        bottomBar.add(deleteBtn);

        contentArea.add(topBar, BorderLayout.NORTH);
        contentArea.add(scrollPane, BorderLayout.CENTER);
        contentArea.add(bottomBar, BorderLayout.SOUTH);
    }

    @Override
    public void onPanelShown() {
        applyRolePermissions();
        loadCustomers();
    }

    private void applyRolePermissions() {
        UserRole role = SessionManager.getCurrentRole();
        boolean readOnly = NavigationItem.CUSTOMERS.isReadOnly(role);
        boolean isAdmin = SessionManager.isAdmin();

        registerBtn.setVisible(!readOnly);
        editBtn.setVisible(!readOnly);
        deleteBtn.setVisible(isAdmin); // Specification §7.10: Only administrators can delete records

        accessBadge.setText(readOnly ? "Mode: View Only" : "Mode: Full Access");
        accessBadge.setForeground(readOnly ? new Color(180, 100, 0) : new Color(16, 124, 65));
    }

    private void loadCustomers() {
        tableModel.setRowCount(0);
        try {
            List<Customer> list = CustomerDB.getAllCustomers();
            for (Customer c : list) {
                tableModel.addRow(new Object[]{
                        c.getCustomerId(),
                        c.getFullName(),
                        c.getPhone(),
                        c.getEmail() != null ? c.getEmail() : "–",
                        c.getAddress() != null ? c.getAddress() : "–",
                        c.getCreatedAt()
                });
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load customers", e);
            JOptionPane.showMessageDialog(this, "Failed to load customer records: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        tableModel.setRowCount(0);
        try {
            List<Customer> list = CustomerDB.searchCustomers(query);
            for (Customer c : list) {
                tableModel.addRow(new Object[]{
                        c.getCustomerId(),
                        c.getFullName(),
                        c.getPhone(),
                        c.getEmail() != null ? c.getEmail() : "–",
                        c.getAddress() != null ? c.getAddress() : "–",
                        c.getCreatedAt()
                });
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Customer search error", e);
            JOptionPane.showMessageDialog(this, "Search error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Integer getSelectedCustomerId() {
        int selectedRow = customerTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a customer from the table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return (Integer) tableModel.getValueAt(selectedRow, 0);
    }

    private void handleViewDevice() {
        Integer customerId = getSelectedCustomerId();
        if (customerId == null) return;

        try {
            Customer customer = CustomerDB.getCustomerById(customerId);
            Device device = DeviceDB.getDeviceByCustomerId(customerId);

            if (device == null) {
                JOptionPane.showMessageDialog(this,
                        "Customer " + (customer != null ? customer.getFullName() : "") + " does not have a registered device yet.",
                        "No Device Found",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String msg = "Customer: " + customer.getFullName() + " (Phone: " + customer.getPhone() + ")\n\n" +
                         "Device ID: #" + device.getDeviceId() + "\n" +
                         "Category: " + device.getCategory() + "\n" +
                         "Brand & Model: " + device.getBrand() + " " + device.getModel() + "\n" +
                         "Serial Number / IMEI: " + (device.getSerialNumber() != null ? device.getSerialNumber() : "N/A") + "\n" +
                         "Colour: " + (device.getColour() != null ? device.getColour() : "N/A") + "\n" +
                         "Accessories Received: " + (device.getAccessories() != null ? device.getAccessories() : "None") + "\n" +
                         "Registration Date: " + device.getCreatedAt();

            JOptionPane.showMessageDialog(this, msg, "Associated Device Details", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving device details", e);
            JOptionPane.showMessageDialog(this, "Error fetching device: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleEditCustomer() {
        Integer customerId = getSelectedCustomerId();
        if (customerId == null) return;

        try {
            Customer customer = CustomerDB.getCustomerById(customerId);
            if (customer != null) {
                openCustomerDialog(customer);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching customer for edit", e);
            JOptionPane.showMessageDialog(this, "Error fetching customer: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteCustomer() {
        Integer customerId = getSelectedCustomerId();
        if (customerId == null) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete Customer #" + customerId + "?\n" +
                "Note: Deletion will be rejected if associated repair records exist.",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean deleted = CustomerDB.deleteCustomer(customerId);
                if (deleted) {
                    JOptionPane.showMessageDialog(this, "Customer #" + customerId + " successfully deleted.", "Record Deleted", JOptionPane.INFORMATION_MESSAGE);
                    loadCustomers();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Deletion blocked", e);
                JOptionPane.showMessageDialog(this, e.getMessage(), "Deletion Prohibited", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Dialog for registering or updating a customer.
     */
    private void openCustomerDialog(Customer customerToEdit) {
        boolean isEdit = customerToEdit != null;
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), isEdit ? "Edit Customer" : "Register Customer", true);
        dialog.setSize(480, isEdit ? 340 : 540);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 10));

        JTextField nameField = new JTextField(isEdit ? customerToEdit.getFullName() : "");
        JTextField phoneField = new JTextField(isEdit ? customerToEdit.getPhone() : "");
        JTextField emailField = new JTextField(isEdit && customerToEdit.getEmail() != null ? customerToEdit.getEmail() : "");
        JTextField addressField = new JTextField(isEdit && customerToEdit.getAddress() != null ? customerToEdit.getAddress() : "");

        form.add(new JLabel("Full Name:*"));
        form.add(nameField);
        form.add(new JLabel("Phone Number:*"));
        form.add(phoneField);
        form.add(new JLabel("Email Address (Optional):"));
        form.add(emailField);
        form.add(new JLabel("Address (Optional):"));
        form.add(addressField);

        // Optional device registration on intake
        JCheckBox registerDeviceCheck = new JCheckBox("Register Device at Intake (1:1 link)", true);
        JComboBox<String> categoryCombo = new JComboBox<>(new String[]{"Smartphone", "Tablet", "Laptop", "Desktop", "Other"});
        JTextField brandField = new JTextField();
        JTextField modelField = new JTextField();
        JTextField serialField = new JTextField();
        JTextField colourField = new JTextField();
        JTextField accessoriesField = new JTextField();

        JPanel deviceSection = new JPanel(new GridLayout(0, 2, 8, 8));
        deviceSection.setBorder(BorderFactory.createTitledBorder("Initial Registered Device"));
        deviceSection.add(new JLabel("Category:*"));
        deviceSection.add(categoryCombo);
        deviceSection.add(new JLabel("Brand:*"));
        deviceSection.add(brandField);
        deviceSection.add(new JLabel("Model:*"));
        deviceSection.add(modelField);
        deviceSection.add(new JLabel("Serial Number / IMEI:"));
        deviceSection.add(serialField);
        deviceSection.add(new JLabel("Colour:"));
        deviceSection.add(colourField);
        deviceSection.add(new JLabel("Accessories Received:"));
        deviceSection.add(accessoriesField);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(form);

        if (!isEdit) {
            centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            centerPanel.add(registerDeviceCheck);
            centerPanel.add(Box.createRigidArea(new Dimension(0, 6)));
            centerPanel.add(deviceSection);

            registerDeviceCheck.addActionListener(e -> {
                boolean enable = registerDeviceCheck.isSelected();
                categoryCombo.setEnabled(enable);
                brandField.setEnabled(enable);
                modelField.setEnabled(enable);
                serialField.setEnabled(enable);
                colourField.setEnabled(enable);
                accessoriesField.setEnabled(enable);
            });
        }

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton(isEdit ? "Update Customer" : "Save Customer");
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();
            String address = addressField.getText().trim();

            if (!ValidationUtil.isNotEmpty(name)) {
                JOptionPane.showMessageDialog(dialog, "Customer full name is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!ValidationUtil.isValidPhone(phone)) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid phone number (e.g. 0771234567).", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!ValidationUtil.isValidOptionalEmail(email)) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid email address or leave it blank.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!isEdit && registerDeviceCheck.isSelected()) {
                if (!ValidationUtil.isNotEmpty(brandField.getText())) {
                    JOptionPane.showMessageDialog(dialog, "Device brand is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (!ValidationUtil.isNotEmpty(modelField.getText())) {
                    JOptionPane.showMessageDialog(dialog, "Device model is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            try {
                if (isEdit) {
                    customerToEdit.setFullName(name);
                    customerToEdit.setPhone(phone);
                    customerToEdit.setEmail(email);
                    customerToEdit.setAddress(address);
                    CustomerDB.updateCustomer(customerToEdit);
                    JOptionPane.showMessageDialog(dialog, "Customer details updated successfully.", "Saved", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    Customer newCustomer = new Customer(name, phone, email, address);
                    int customerId = CustomerDB.createCustomer(newCustomer);

                    if (registerDeviceCheck.isSelected()) {
                        Device newDevice = new Device(
                                customerId,
                                (String) categoryCombo.getSelectedItem(),
                                brandField.getText().trim(),
                                modelField.getText().trim(),
                                serialField.getText().trim(),
                                colourField.getText().trim(),
                                accessoriesField.getText().trim()
                        );
                        DeviceDB.createDevice(newDevice);
                    }

                    JOptionPane.showMessageDialog(dialog, "Customer registered successfully (ID: #" + customerId + ").", "Registration Complete", JOptionPane.INFORMATION_MESSAGE);
                }

                dialog.dispose();
                loadCustomers();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error saving customer", ex);
                JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);

        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }
}
