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
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Electronic device registry panel with embedded warranty tracking.
 */
public class DevicePanel extends BasePanel {

    private static final Logger LOGGER = Logger.getLogger(DevicePanel.class.getName());

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Customer", "Phone", "Category", "Brand", "Model", "Serial / IMEI", "Warranty (Months)", "Warranty Expiry", "Warranty Status"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable deviceTable = new JTable(tableModel);
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> categoryCombo = new JComboBox<>(new String[]{
            "All Categories", "Smartphone", "Tablet", "Laptop", "Desktop", "Other"
    });
    private final JLabel accessBadge = new JLabel();

    private JButton registerBtn;
    private JButton editBtn;

    public DevicePanel() {
        super("Device Registry", "Track electronic devices, serial numbers, categories, customer associations, and warranty coverage");
        initUI();
    }

    private void initUI() {
        contentArea.setLayout(new BorderLayout(0, 15));

        // Top Toolbar
        JPanel topBar = new JPanel(new BorderLayout());

        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchField.putClientProperty("JTextField.placeholderText", "Search brand, model, serial, customer...");
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());

        categoryCombo.addActionListener(e -> performSearch());

        JButton resetBtn = new JButton("Reset");
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            categoryCombo.setSelectedIndex(0);
            loadDevices();
        });

        searchBox.add(searchField);
        searchBox.add(new JLabel("Category:"));
        searchBox.add(categoryCombo);
        searchBox.add(searchBtn);
        searchBox.add(resetBtn);

        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        registerBtn = new JButton("+ Register Device");
        registerBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        registerBtn.putClientProperty("JButton.buttonType", "roundRect");
        registerBtn.addActionListener(e -> openDeviceDialog(null));

        rightBox.add(accessBadge);
        rightBox.add(registerBtn);

        topBar.add(searchBox, BorderLayout.WEST);
        topBar.add(rightBox, BorderLayout.EAST);

        // Table
        deviceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        deviceTable.setRowHeight(26);
        deviceTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        JScrollPane scrollPane = new JScrollPane(deviceTable);

        // Bottom Action Bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomBar.setBorder(new EmptyBorder(5, 0, 0, 0));

        editBtn = new JButton("Edit Device Details / Warranty");
        editBtn.addActionListener(e -> handleEditDevice());

        bottomBar.add(editBtn);

        contentArea.add(topBar, BorderLayout.NORTH);
        contentArea.add(scrollPane, BorderLayout.CENTER);
        contentArea.add(bottomBar, BorderLayout.SOUTH);
    }

    @Override
    public void onPanelShown() {
        applyRolePermissions();
        loadDevices();
    }

    private void applyRolePermissions() {
        UserRole role = SessionManager.getCurrentRole();
        boolean readOnly = NavigationItem.DEVICES.isReadOnly(role);

        registerBtn.setVisible(!readOnly);
        editBtn.setVisible(!readOnly);

        accessBadge.setText(readOnly ? "Mode: View Only" : "Mode: Full Access");
        accessBadge.setForeground(readOnly ? new Color(180, 100, 0) : new Color(16, 124, 65));
    }

    private void loadDevices() {
        tableModel.setRowCount(0);
        try {
            List<Device> list = DeviceDB.getAllDevices();
            for (Device d : list) {
                String warrantyStatus = d.isUnderWarranty() ? "VALID (Under Warranty)" : "EXPIRED / NONE";
                tableModel.addRow(new Object[]{
                        d.getDeviceId(),
                        d.getCustomerName() != null ? d.getCustomerName() : ("Cust #" + d.getCustomerId()),
                        d.getCustomerPhone() != null ? d.getCustomerPhone() : "–",
                        d.getCategory(),
                        d.getBrand(),
                        d.getModel(),
                        d.getSerialNumber() != null ? d.getSerialNumber() : "–",
                        d.getWarrantyMonths() > 0 ? d.getWarrantyMonths() + " mo" : "None",
                        d.getWarrantyExpiryDate() != null ? d.getWarrantyExpiryDate() : "–",
                        warrantyStatus
                });
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load devices", e);
            JOptionPane.showMessageDialog(this, "Failed to load device records: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        String category = (String) categoryCombo.getSelectedItem();
        tableModel.setRowCount(0);
        try {
            List<Device> list = DeviceDB.searchDevices(query, category);
            for (Device d : list) {
                String warrantyStatus = d.isUnderWarranty() ? "VALID (Under Warranty)" : "EXPIRED / NONE";
                tableModel.addRow(new Object[]{
                        d.getDeviceId(),
                        d.getCustomerName() != null ? d.getCustomerName() : ("Cust #" + d.getCustomerId()),
                        d.getCustomerPhone() != null ? d.getCustomerPhone() : "–",
                        d.getCategory(),
                        d.getBrand(),
                        d.getModel(),
                        d.getSerialNumber() != null ? d.getSerialNumber() : "–",
                        d.getWarrantyMonths() > 0 ? d.getWarrantyMonths() + " mo" : "None",
                        d.getWarrantyExpiryDate() != null ? d.getWarrantyExpiryDate() : "–",
                        warrantyStatus
                });
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Device search error", e);
            JOptionPane.showMessageDialog(this, "Search error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Integer getSelectedDeviceId() {
        int selectedRow = deviceTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a device from the table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return (Integer) tableModel.getValueAt(selectedRow, 0);
    }

    private void handleEditDevice() {
        Integer deviceId = getSelectedDeviceId();
        if (deviceId == null) return;

        try {
            Device device = DeviceDB.getDeviceById(deviceId);
            if (device != null) {
                openDeviceDialog(device);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching device for edit", e);
            JOptionPane.showMessageDialog(this, "Error fetching device: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openDeviceDialog(Device deviceToEdit) {
        boolean isEdit = deviceToEdit != null;
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), isEdit ? "Edit Device & Warranty" : "Register Device", true);
        dialog.setSize(520, 520);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 10));

        JComboBox<CustomerItem> customerSelector = new JComboBox<>();
        if (!isEdit) {
            try {
                List<Customer> customers = CustomerDB.getAllCustomers();
                for (Customer c : customers) {
                    customerSelector.addItem(new CustomerItem(c.getCustomerId(), c.getFullName() + " (ID: #" + c.getCustomerId() + ")"));
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error loading customers for device registration", ex);
            }
        }

        JComboBox<String> categoryField = new JComboBox<>(new String[]{"Smartphone", "Tablet", "Laptop", "Desktop", "Other"});
        if (isEdit) categoryField.setSelectedItem(deviceToEdit.getCategory());

        JTextField brandField = new JTextField(isEdit ? deviceToEdit.getBrand() : "");
        JTextField modelField = new JTextField(isEdit ? deviceToEdit.getModel() : "");
        JTextField serialField = new JTextField(isEdit && deviceToEdit.getSerialNumber() != null ? deviceToEdit.getSerialNumber() : "");
        JTextField colourField = new JTextField(isEdit && deviceToEdit.getColour() != null ? deviceToEdit.getColour() : "");
        JTextField accessoriesField = new JTextField(isEdit && deviceToEdit.getAccessories() != null ? deviceToEdit.getAccessories() : "");

        JSpinner warrantyMonthsSpinner = new JSpinner(new SpinnerNumberModel(
                isEdit ? deviceToEdit.getWarrantyMonths() : 0, 0, 120, 1
        ));
        JTextField warrantyStartDateField = new JTextField(
                isEdit && deviceToEdit.getWarrantyStartDate() != null ? deviceToEdit.getWarrantyStartDate() : LocalDate.now().toString()
        );

        if (!isEdit) {
            form.add(new JLabel("Associated Customer:*"));
            form.add(customerSelector);
        }

        form.add(new JLabel("Device Category:*"));
        form.add(categoryField);
        form.add(new JLabel("Brand:*"));
        form.add(brandField);
        form.add(new JLabel("Model:*"));
        form.add(modelField);
        form.add(new JLabel("Serial Number / IMEI:"));
        form.add(serialField);
        form.add(new JLabel("Colour:"));
        form.add(colourField);
        form.add(new JLabel("Accessories Received:"));
        form.add(accessoriesField);
        form.add(new JLabel("Warranty Period (Months):"));
        form.add(warrantyMonthsSpinner);
        form.add(new JLabel("Warranty Start Date (YYYY-MM-DD):"));
        form.add(warrantyStartDateField);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton(isEdit ? "Update Device" : "Save Device");
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveBtn.addActionListener(e -> {
            String brand = brandField.getText().trim();
            String model = modelField.getText().trim();

            if (!ValidationUtil.isNotEmpty(brand)) {
                JOptionPane.showMessageDialog(dialog, "Device brand is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!ValidationUtil.isNotEmpty(model)) {
                JOptionPane.showMessageDialog(dialog, "Device model is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!isEdit && customerSelector.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(dialog, "Please select an associated customer. Register a customer first if none exist.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int wMonths = (Integer) warrantyMonthsSpinner.getValue();
            String wStart = warrantyStartDateField.getText().trim();
            String wExpiry = null;
            if (wMonths > 0) {
                try {
                    LocalDate startDate = LocalDate.parse(wStart);
                    wExpiry = startDate.plusMonths(wMonths).toString();
                } catch (Exception ex) {
                    wStart = LocalDate.now().toString();
                    wExpiry = LocalDate.now().plusMonths(wMonths).toString();
                }
            } else {
                wStart = null;
                wExpiry = null;
            }

            try {
                if (isEdit) {
                    deviceToEdit.setCategory((String) categoryField.getSelectedItem());
                    deviceToEdit.setBrand(brand);
                    deviceToEdit.setModel(model);
                    deviceToEdit.setSerialNumber(serialField.getText().trim());
                    deviceToEdit.setColour(colourField.getText().trim());
                    deviceToEdit.setAccessories(accessoriesField.getText().trim());
                    deviceToEdit.setWarrantyMonths(wMonths);
                    deviceToEdit.setWarrantyStartDate(wStart);
                    deviceToEdit.setWarrantyExpiryDate(wExpiry);

                    DeviceDB.updateDevice(deviceToEdit);
                    JOptionPane.showMessageDialog(dialog, "Device and warranty updated successfully.", "Saved", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    CustomerItem selectedCustomer = (CustomerItem) customerSelector.getSelectedItem();
                    Device newDevice = new Device(
                            selectedCustomer.customerId,
                            (String) categoryField.getSelectedItem(),
                            brand,
                            model,
                            serialField.getText().trim(),
                            colourField.getText().trim(),
                            accessoriesField.getText().trim()
                    );
                    newDevice.setWarrantyMonths(wMonths);
                    newDevice.setWarrantyStartDate(wStart);
                    newDevice.setWarrantyExpiryDate(wExpiry);

                    int deviceId = DeviceDB.createDevice(newDevice);
                    JOptionPane.showMessageDialog(dialog, "Device registered successfully (ID: #" + deviceId + ").", "Registration Complete", JOptionPane.INFORMATION_MESSAGE);
                }

                dialog.dispose();
                loadDevices();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error saving device", ex);
                JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);

        panel.add(form, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private record CustomerItem(int customerId, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
