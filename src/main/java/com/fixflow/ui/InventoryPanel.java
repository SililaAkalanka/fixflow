package com.fixflow.ui;

import com.fixflow.database.InventoryDB;
import com.fixflow.enums.NavigationItem;
import com.fixflow.enums.UserRole;
import com.fixflow.model.SparePart;
import com.fixflow.util.SessionManager;
import com.fixflow.util.ValidationUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Spare-part inventory and stock tracking panel.
 * Implements Section 7.5 and Section 8.17 of the FixFlow Project Specification.
 */
public class InventoryPanel extends BasePanel {

    private static final Logger LOGGER = Logger.getLogger(InventoryPanel.class.getName());

    public static final String[] CATEGORIES = new String[]{
            "PROCESSORS",
            "MOTHERBOARDS",
            "RAM",
            "STORAGE",
            "POWER SUPPLY",
            "COOLERS",
            "GRAPHIC CARD",
            "CASING",
            "LAPTOP ACCESSORIES",
            "UPS & BATTERIES",
            "KEYBOARD & MOUSE",
            "MONITORS",
            "ACCESSORIES",
            "LAPTOP COOLING PADS",
            "COMPUTER REPAIR TOOLS",
            "LAPTOP REPAIR TOOLS",
            "SOLDERING TOOLS",
            "THERMAL PASTE & PADS",
            "CLEANING MATERIALS",
            "DIAGNOSTIC TOOLS",
            "REPLACEMENT ICs & COMPONENTS",
            "LAPTOP SCREENS",
            "KEYBOARD REPLACEMENTS",
            "LAPTOP BATTERIES",
            "CHARGERS & ADAPTERS",
            "DATA RECOVERY",
            "CABLES & CONNECTORS"
    };

    private static String[] createCategoryFilterOptions() {
        String[] options = new String[CATEGORIES.length + 1];
        options[0] = "All Categories";
        System.arraycopy(CATEGORIES, 0, options, 1, CATEGORIES.length);
        return options;
    }

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Part Name", "Category", "Compatible Model", "Purchase (LKR)", "Selling (LKR)", "Stock Qty", "Min Level", "Stock Alert Status"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable inventoryTable = new JTable(tableModel);
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> categoryCombo = new JComboBox<>(createCategoryFilterOptions());
    private final JCheckBox lowStockOnlyCheck = new JCheckBox("Low Stock Only");
    private final JLabel accessBadge = new JLabel();

    private JButton registerBtn;
    private JButton addStockBtn;
    private JButton editBtn;
    private JButton deleteBtn;

    public InventoryPanel() {
        super("Spare-Part Inventory", "Manage replacement parts, compatibility catalogs, stock quantities, and reorder alerts");
        initUI();
    }

    private void initUI() {
        contentArea.setLayout(new BorderLayout(0, 15));

        // Top Toolbar
        JPanel topBar = new JPanel(new BorderLayout());

        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchField.putClientProperty("JTextField.placeholderText", "Search part name, model, or ID...");
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());

        categoryCombo.addActionListener(e -> performSearch());
        lowStockOnlyCheck.addActionListener(e -> performSearch());

        JButton resetBtn = new JButton("Reset");
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            categoryCombo.setSelectedIndex(0);
            lowStockOnlyCheck.setSelected(false);
            loadParts();
        });

        searchBox.add(searchField);
        searchBox.add(new JLabel("Category:"));
        searchBox.add(categoryCombo);
        searchBox.add(lowStockOnlyCheck);
        searchBox.add(searchBtn);
        searchBox.add(resetBtn);

        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        registerBtn = new JButton("+ Register Part");
        registerBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        registerBtn.putClientProperty("JButton.buttonType", "roundRect");
        registerBtn.addActionListener(e -> openPartDialog(null));

        rightBox.add(accessBadge);
        rightBox.add(registerBtn);

        topBar.add(searchBox, BorderLayout.WEST);
        topBar.add(rightBox, BorderLayout.EAST);

        // Table setup
        inventoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        inventoryTable.setRowHeight(26);
        inventoryTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Highlight stock status column
        inventoryTable.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    String str = value.toString();
                    if (str.contains("OUT OF STOCK")) {
                        setForeground(new Color(190, 20, 20));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if (str.contains("LOW STOCK")) {
                        setForeground(new Color(210, 80, 0));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setForeground(new Color(16, 124, 65));
                        setFont(getFont().deriveFont(Font.PLAIN));
                    }
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(inventoryTable);

        // Bottom Action Bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomBar.setBorder(new EmptyBorder(5, 0, 0, 0));

        addStockBtn = new JButton("+ Add Stock");
        addStockBtn.addActionListener(e -> handleAddStock());

        editBtn = new JButton("Edit Part Details");
        editBtn.addActionListener(e -> handleEditPart());

        deleteBtn = new JButton("Delete Part");
        deleteBtn.setForeground(new Color(200, 30, 30));
        deleteBtn.addActionListener(e -> handleDeletePart());

        bottomBar.add(addStockBtn);
        bottomBar.add(editBtn);
        bottomBar.add(deleteBtn);

        contentArea.add(topBar, BorderLayout.NORTH);
        contentArea.add(scrollPane, BorderLayout.CENTER);
        contentArea.add(bottomBar, BorderLayout.SOUTH);
    }

    @Override
    public void onPanelShown() {
        applyRolePermissions();
        loadParts();
    }

    private void applyRolePermissions() {
        UserRole role = SessionManager.getCurrentRole();
        boolean readOnly = NavigationItem.INVENTORY.isReadOnly(role);
        boolean isAdmin = SessionManager.isAdmin();

        registerBtn.setVisible(!readOnly);
        addStockBtn.setVisible(!readOnly);
        editBtn.setVisible(!readOnly);
        deleteBtn.setVisible(isAdmin); // Specification §7.10: Only administrators can delete records

        accessBadge.setText(readOnly ? "Mode: View Only" : "Mode: Stock Management Enabled");
        accessBadge.setForeground(readOnly ? new Color(180, 100, 0) : new Color(16, 124, 65));
    }

    private void loadParts() {
        performSearch();
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        String category = (String) categoryCombo.getSelectedItem();
        boolean lowStock = lowStockOnlyCheck.isSelected();

        tableModel.setRowCount(0);
        try {
            List<SparePart> list = InventoryDB.searchParts(query, category, lowStock);
            for (SparePart p : list) {
                String statusText;
                if (p.isOutOfStock()) {
                    statusText = "OUT OF STOCK (0)";
                } else if (p.isLowStock()) {
                    statusText = "⚠️ LOW STOCK (" + p.getAvailableQuantity() + ")";
                } else {
                    statusText = "IN STOCK (" + p.getAvailableQuantity() + ")";
                }

                tableModel.addRow(new Object[]{
                        p.getPartId(),
                        p.getPartName(),
                        p.getCategory(),
                        p.getCompatibleDeviceModel(),
                        String.format("%.2f", p.getPurchaseCost()),
                        String.format("%.2f", p.getSellingPrice()),
                        p.getAvailableQuantity(),
                        p.getMinStockLevel(),
                        statusText
                });
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load spare parts", e);
            JOptionPane.showMessageDialog(this, "Failed to load parts: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Integer getSelectedPartId() {
        int selectedRow = inventoryTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a part from the table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return (Integer) tableModel.getValueAt(selectedRow, 0);
    }

    private void handleAddStock() {
        Integer partId = getSelectedPartId();
        if (partId == null) return;

        try {
            SparePart part = InventoryDB.getPartById(partId);
            if (part == null) return;

            String input = JOptionPane.showInputDialog(
                    this,
                    "Part: " + part.getPartName() + " (" + part.getCompatibleDeviceModel() + ")\n" +
                    "Current Available Quantity: " + part.getAvailableQuantity() + "\n\n" +
                    "Enter quantity to add to stock:",
                    "Add Stock",
                    JOptionPane.QUESTION_MESSAGE
            );

            if (input != null && !input.trim().isEmpty()) {
                int qtyToAdd;
                try {
                    qtyToAdd = Integer.parseInt(input.trim());
                    if (qtyToAdd <= 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid positive integer.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                InventoryDB.addStock(partId, qtyToAdd);
                JOptionPane.showMessageDialog(this, "Successfully added " + qtyToAdd + " units to " + part.getPartName() + ".", "Stock Updated", JOptionPane.INFORMATION_MESSAGE);
                loadParts();
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error adding stock", ex);
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleEditPart() {
        Integer partId = getSelectedPartId();
        if (partId == null) return;

        try {
            SparePart part = InventoryDB.getPartById(partId);
            if (part != null) {
                openPartDialog(part);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error fetching part for edit", ex);
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeletePart() {
        Integer partId = getSelectedPartId();
        if (partId == null) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete Part #" + partId + "?\n" +
                "Note: Deletion will be blocked if this part has historical usage records in repair jobs.",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean deleted = InventoryDB.deletePart(partId);
                if (deleted) {
                    JOptionPane.showMessageDialog(this, "Part #" + partId + " successfully deleted.", "Record Deleted", JOptionPane.INFORMATION_MESSAGE);
                    loadParts();
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "Part deletion blocked", ex);
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Deletion Prohibited", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openPartDialog(SparePart partToEdit) {
        boolean isEdit = partToEdit != null;
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), isEdit ? "Edit Spare Part" : "Register New Spare Part", true);
        dialog.setSize(480, 420);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 10));

        JTextField nameField = new JTextField(isEdit ? partToEdit.getPartName() : "");
        JComboBox<String> catField = new JComboBox<>(CATEGORIES);
        if (isEdit) catField.setSelectedItem(partToEdit.getCategory());

        JTextField modelField = new JTextField(isEdit ? partToEdit.getCompatibleDeviceModel() : "");
        JTextField purchaseField = new JTextField(isEdit ? String.valueOf(partToEdit.getPurchaseCost()) : "");
        JTextField sellingField = new JTextField(isEdit ? String.valueOf(partToEdit.getSellingPrice()) : "");
        JTextField qtyField = new JTextField(isEdit ? String.valueOf(partToEdit.getAvailableQuantity()) : "0");
        JTextField minLevelField = new JTextField(isEdit ? String.valueOf(partToEdit.getMinStockLevel()) : "5");

        if (isEdit) {
            qtyField.setEnabled(false); // Changes to quantity should be made via + Add Stock
        }

        form.add(new JLabel("Part Name:*"));
        form.add(nameField);
        form.add(new JLabel("Category:*"));
        form.add(catField);
        form.add(new JLabel("Compatible Device / Model:*"));
        form.add(modelField);
        form.add(new JLabel("Purchase Cost (LKR):*"));
        form.add(purchaseField);
        form.add(new JLabel("Selling Price (LKR):*"));
        form.add(sellingField);
        form.add(new JLabel(isEdit ? "Stock Quantity (Current):" : "Initial Stock Quantity:*"));
        form.add(qtyField);
        form.add(new JLabel("Min. Stock Alert Level:*"));
        form.add(minLevelField);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton(isEdit ? "Update Part" : "Save Part");
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String cat = (String) catField.getSelectedItem();
            String compModel = modelField.getText().trim();
            String purchaseStr = purchaseField.getText().trim();
            String sellingStr = sellingField.getText().trim();
            String minLevelStr = minLevelField.getText().trim();
            String qtyStr = qtyField.getText().trim();

            if (!ValidationUtil.isNotEmpty(name)) {
                JOptionPane.showMessageDialog(dialog, "Part name is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!ValidationUtil.isNotEmpty(compModel)) {
                JOptionPane.showMessageDialog(dialog, "Compatible model is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double purchase;
            double selling;
            int minLevel;
            int qty = 0;

            try {
                purchase = Double.parseDouble(purchaseStr);
                selling = Double.parseDouble(sellingStr);
                minLevel = Integer.parseInt(minLevelStr);
                if (purchase < 0 || selling < 0 || minLevel < 0) throw new NumberFormatException();
                if (!isEdit) {
                    qty = Integer.parseInt(qtyStr);
                    if (qty < 0) throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter valid non-negative numbers for prices, stock, and min level.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (isEdit) {
                    partToEdit.setPartName(name);
                    partToEdit.setCategory(cat);
                    partToEdit.setCompatibleDeviceModel(compModel);
                    partToEdit.setPurchaseCost(purchase);
                    partToEdit.setSellingPrice(selling);
                    partToEdit.setMinStockLevel(minLevel);
                    InventoryDB.updatePart(partToEdit);
                    JOptionPane.showMessageDialog(dialog, "Part updated successfully.", "Saved", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    SparePart newPart = new SparePart(name, cat, compModel, purchase, selling, qty, minLevel);
                    int partId = InventoryDB.createPart(newPart);
                    JOptionPane.showMessageDialog(dialog, "Part registered successfully (ID: #" + partId + ").", "Registration Complete", JOptionPane.INFORMATION_MESSAGE);
                }

                dialog.dispose();
                loadParts();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error saving part", ex);
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
}
