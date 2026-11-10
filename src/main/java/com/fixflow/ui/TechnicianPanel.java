package com.fixflow.ui;

import com.fixflow.database.TechnicianDB;
import com.fixflow.enums.NavigationItem;
import com.fixflow.enums.UserRole;
import com.fixflow.model.Technician;
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
 * Technician administration panel.
 * Implements Section 7.4 of the FixFlow Project Specification.
 */
public class TechnicianPanel extends BasePanel {

    private static final Logger LOGGER = Logger.getLogger(TechnicianPanel.class.getName());

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Name", "Phone", "Specialization", "Status", "Date Registered"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable technicianTable = new JTable(tableModel);
    private final JTextField searchField = new JTextField(18);
    private final JComboBox<String> statusFilterCombo = new JComboBox<>(new String[]{"All", "Active Only", "Inactive Only"});
    private final JLabel accessBadge = new JLabel();

    private JButton addBtn;
    private JButton editBtn;
    private JButton toggleStatusBtn;
    private JButton deleteBtn;

    public TechnicianPanel() {
        super("Technician Directory", "Manage technical staff, specializations, assignments, and employment status");
        initUI();
    }

    private void initUI() {
        contentArea.setLayout(new BorderLayout(0, 15));

        // Top Toolbar
        JPanel topBar = new JPanel(new BorderLayout());

        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchField.putClientProperty("JTextField.placeholderText", "Search name, phone, specialization...");
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());

        statusFilterCombo.addActionListener(e -> performSearch());

        JButton resetBtn = new JButton("Reset");
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            statusFilterCombo.setSelectedIndex(0);
            loadTechnicians();
        });

        searchBox.add(searchField);
        searchBox.add(new JLabel("Status:"));
        searchBox.add(statusFilterCombo);
        searchBox.add(searchBtn);
        searchBox.add(resetBtn);

        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        addBtn = new JButton("+ Add Technician");
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addBtn.putClientProperty("JButton.buttonType", "roundRect");
        addBtn.addActionListener(e -> openTechnicianDialog(null));

        rightBox.add(accessBadge);
        rightBox.add(addBtn);

        topBar.add(searchBox, BorderLayout.WEST);
        topBar.add(rightBox, BorderLayout.EAST);

        // Table Customization
        technicianTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        technicianTable.setRowHeight(26);
        technicianTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Highlight status column
        technicianTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if ("Active".equals(value)) {
                    setForeground(new Color(16, 124, 65));
                } else if ("Inactive".equals(value)) {
                    setForeground(new Color(180, 40, 40));
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(technicianTable);

        // Bottom Action Bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomBar.setBorder(new EmptyBorder(5, 0, 0, 0));

        editBtn = new JButton("Edit Technician");
        editBtn.addActionListener(e -> handleEditTechnician());

        toggleStatusBtn = new JButton("Toggle Active Status");
        toggleStatusBtn.addActionListener(e -> handleToggleStatus());

        deleteBtn = new JButton("Delete Record");
        deleteBtn.setForeground(new Color(200, 30, 30));
        deleteBtn.addActionListener(e -> handleDeleteTechnician());

        bottomBar.add(editBtn);
        bottomBar.add(toggleStatusBtn);
        bottomBar.add(deleteBtn);

        contentArea.add(topBar, BorderLayout.NORTH);
        contentArea.add(scrollPane, BorderLayout.CENTER);
        contentArea.add(bottomBar, BorderLayout.SOUTH);
    }

    @Override
    public void onPanelShown() {
        applyRolePermissions();
        loadTechnicians();
    }

    private void applyRolePermissions() {
        UserRole role = SessionManager.getCurrentRole();
        boolean readOnly = NavigationItem.TECHNICIANS.isReadOnly(role);
        boolean isAdmin = SessionManager.isAdmin();

        addBtn.setVisible(!readOnly);
        editBtn.setVisible(!readOnly);
        toggleStatusBtn.setVisible(!readOnly);
        deleteBtn.setVisible(isAdmin); // Specification §7.10: Only administrators can delete records

        accessBadge.setText(readOnly ? "Mode: View Only" : "Mode: Management Access");
        accessBadge.setForeground(readOnly ? new Color(180, 100, 0) : new Color(16, 124, 65));
    }

    private void loadTechnicians() {
        tableModel.setRowCount(0);
        try {
            List<Technician> list = TechnicianDB.getAllTechnicians();
            for (Technician t : list) {
                tableModel.addRow(new Object[]{
                        t.getTechnicianId(),
                        t.getName(),
                        t.getPhone(),
                        t.getSpecialization() != null ? t.getSpecialization() : "General Repairs",
                        t.isActive() ? "Active" : "Inactive",
                        t.getCreatedAt()
                });
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load technicians", e);
            JOptionPane.showMessageDialog(this, "Failed to load technician records: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        String filter = (String) statusFilterCombo.getSelectedItem();
        tableModel.setRowCount(0);
        try {
            List<Technician> list = TechnicianDB.searchTechnicians(query, filter);
            for (Technician t : list) {
                tableModel.addRow(new Object[]{
                        t.getTechnicianId(),
                        t.getName(),
                        t.getPhone(),
                        t.getSpecialization() != null ? t.getSpecialization() : "General Repairs",
                        t.isActive() ? "Active" : "Inactive",
                        t.getCreatedAt()
                });
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Technician search error", e);
            JOptionPane.showMessageDialog(this, "Search error: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Integer getSelectedTechnicianId() {
        int selectedRow = technicianTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a technician from the table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return (Integer) tableModel.getValueAt(selectedRow, 0);
    }

    private void handleToggleStatus() {
        Integer techId = getSelectedTechnicianId();
        if (techId == null) return;

        try {
            Technician tech = TechnicianDB.getTechnicianById(techId);
            if (tech != null) {
                boolean newStatus = !tech.isActive();
                String action = newStatus ? "activate" : "deactivate";

                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "Are you sure you want to " + action + " " + tech.getName() + "?\n" +
                        (newStatus ? "Technician will be available for new repair job assignments." :
                                "Technician will no longer receive new assignments. Existing records are preserved."),
                        "Confirm Status Change",
                        JOptionPane.YES_NO_OPTION
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    TechnicianDB.setTechnicianActive(techId, newStatus);
                    JOptionPane.showMessageDialog(this, "Technician " + (newStatus ? "activated." : "deactivated."), "Status Updated", JOptionPane.INFORMATION_MESSAGE);
                    loadTechnicians();
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating technician active status", e);
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleEditTechnician() {
        Integer techId = getSelectedTechnicianId();
        if (techId == null) return;

        try {
            Technician tech = TechnicianDB.getTechnicianById(techId);
            if (tech != null) {
                openTechnicianDialog(tech);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching technician for edit", e);
            JOptionPane.showMessageDialog(this, "Error fetching technician: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteTechnician() {
        Integer techId = getSelectedTechnicianId();
        if (techId == null) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete Technician #" + techId + "?\n" +
                "Note: Deletion will be rejected if the technician has assigned repair jobs.",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean deleted = TechnicianDB.deleteTechnician(techId);
                if (deleted) {
                    JOptionPane.showMessageDialog(this, "Technician #" + techId + " successfully deleted.", "Record Deleted", JOptionPane.INFORMATION_MESSAGE);
                    loadTechnicians();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Technician deletion blocked", e);
                JOptionPane.showMessageDialog(this, e.getMessage(), "Deletion Prohibited", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openTechnicianDialog(Technician techToEdit) {
        boolean isEdit = techToEdit != null;
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), isEdit ? "Edit Technician" : "Add Technician", true);
        dialog.setSize(440, 320);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 12));

        JTextField nameField = new JTextField(isEdit ? techToEdit.getName() : "");
        JTextField phoneField = new JTextField(isEdit ? techToEdit.getPhone() : "");
        JTextField specField = new JTextField(isEdit && techToEdit.getSpecialization() != null ? techToEdit.getSpecialization() : "");
        JCheckBox activeCheck = new JCheckBox("Active Employment Status", !isEdit || techToEdit.isActive());

        form.add(new JLabel("Full Name:*"));
        form.add(nameField);
        form.add(new JLabel("Phone Number:*"));
        form.add(phoneField);
        form.add(new JLabel("Specialization:"));
        form.add(specField);
        form.add(new JLabel("Status:"));
        form.add(activeCheck);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton(isEdit ? "Update Technician" : "Save Technician");
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            String spec = specField.getText().trim();
            boolean active = activeCheck.isSelected();

            if (!ValidationUtil.isNotEmpty(name)) {
                JOptionPane.showMessageDialog(dialog, "Technician name is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!ValidationUtil.isValidPhone(phone)) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid phone number (e.g. 0771234567).", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (isEdit) {
                    techToEdit.setName(name);
                    techToEdit.setPhone(phone);
                    techToEdit.setSpecialization(spec);
                    techToEdit.setActive(active);
                    TechnicianDB.updateTechnician(techToEdit);
                    JOptionPane.showMessageDialog(dialog, "Technician details updated successfully.", "Saved", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    Technician newTech = new Technician(name, phone, spec, active);
                    int techId = TechnicianDB.createTechnician(newTech);
                    JOptionPane.showMessageDialog(dialog, "Technician registered successfully (ID: #" + techId + ").", "Registration Complete", JOptionPane.INFORMATION_MESSAGE);
                }

                dialog.dispose();
                loadTechnicians();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error saving technician", ex);
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
