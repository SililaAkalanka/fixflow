package com.fixflow.ui;

import com.fixflow.database.DatabaseConnection;
import com.fixflow.database.UserDB;
import com.fixflow.enums.UserRole;
import com.fixflow.model.User;
import com.fixflow.util.SessionManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
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
 * Settings and credentials management panel for Administrator and Technician accounts.
 */
public class SettingsPanel extends BasePanel {

    private static final Logger LOGGER = Logger.getLogger(SettingsPanel.class.getName());

    private final JPanel contentWrapper = new JPanel();
    private final JLabel accessWarningLabel = new JLabel();

    // Role table
    private final DefaultTableModel roleTableModel = new DefaultTableModel(
            new Object[]{"Role", "Username", "Last Modified"}, 0
    ) { @Override public boolean isCellEditable(int r, int c) { return false; } };
    private final JTable roleTable = new JTable(roleTableModel);

    // DB Info Labels
    private final JLabel dbPathLabel = new JLabel("-");

    private JButton changePasswordBtn;

    public SettingsPanel() {
        super("System Settings & Credentials", "Manage system credentials and application runtime configuration");
        initUI();
    }

    private void initUI() {
        contentArea.setLayout(new BorderLayout());

        contentWrapper.setLayout(new BoxLayout(contentWrapper, BoxLayout.Y_AXIS));
        contentWrapper.setBorder(new EmptyBorder(0, 0, 10, 0));

        // Access warning if not admin
        accessWarningLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        accessWarningLabel.setForeground(new Color(180, 20, 20));
        accessWarningLabel.setVisible(false);
        contentArea.add(accessWarningLabel, BorderLayout.NORTH);

        // 1. Role Credentials Card
        JPanel credCard = new JPanel(new BorderLayout(0, 8));
        credCard.setBorder(new CompoundBorder(
                new TitledBorder("User Credentials Management"),
                new EmptyBorder(8, 12, 10, 12)
        ));

        roleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roleTable.setRowHeight(26);
        roleTable.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        roleTable.getColumnModel().getColumn(0).setCellRenderer(center);

        JScrollPane roleScroll = new JScrollPane(roleTable);
        roleScroll.setPreferredSize(new Dimension(750, 140));

        JPanel credBtnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        changePasswordBtn = new JButton("Update Password / Username...");
        changePasswordBtn.setFont(changePasswordBtn.getFont().deriveFont(Font.BOLD));
        changePasswordBtn.addActionListener(e -> handleChangePassword());
        credBtnBar.add(changePasswordBtn);

        credCard.add(roleScroll, BorderLayout.CENTER);
        credCard.add(credBtnBar, BorderLayout.SOUTH);

        // 2. Database & System Environment Card
        JPanel sysCard = new JPanel(new GridLayout(4, 2, 8, 8));
        sysCard.setBorder(new CompoundBorder(
                new TitledBorder("System Environment & Database Runtime"),
                new EmptyBorder(8, 12, 10, 12)
        ));
        sysCard.add(new JLabel("Database Connection URL:"));
        dbPathLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sysCard.add(dbPathLabel);

        sysCard.add(new JLabel("Java Runtime Version:"));
        sysCard.add(new JLabel(System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")"));

        sysCard.add(new JLabel("Operating System:"));
        sysCard.add(new JLabel(System.getProperty("os.name") + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")"));

        sysCard.add(new JLabel("Application Edition:"));
        sysCard.add(new JLabel("FixFlow Desktop Application (Java 21 / SQLite)"));

        contentWrapper.add(credCard);
        contentWrapper.add(Box.createRigidArea(new Dimension(0, 12)));
        contentWrapper.add(sysCard);

        contentArea.add(new JScrollPane(contentWrapper), BorderLayout.CENTER);
    }

    @Override
    public void onPanelShown() {
        applyRolePermissions();
        loadRoleUsers();
        refreshDatabaseInfo();
    }

    private void applyRolePermissions() {
        boolean isAdmin = SessionManager.isAdmin();
        changePasswordBtn.setEnabled(isAdmin);

        if (!isAdmin) {
            accessWarningLabel.setText("Restricted Access: Credential modifications are reserved for the Administrator.");
            accessWarningLabel.setVisible(true);
        } else {
            accessWarningLabel.setVisible(false);
        }
    }

    private void loadRoleUsers() {
        try {
            roleTableModel.setRowCount(0);
            List<User> users = UserDB.getAllUsers();
            for (User u : users) {
                roleTableModel.addRow(new Object[]{
                        u.getRole().getDisplayName(),
                        u.getUsername(),
                        u.getUpdatedAt() != null ? u.getUpdatedAt() : u.getCreatedAt()
                });
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load users", ex);
        }
    }

    private void refreshDatabaseInfo() {
        dbPathLabel.setText(DatabaseConnection.getJdbcUrl());
    }

    private void handleChangePassword() {
        int row = roleTable.getSelectedRow();
        UserRole targetRole = null;
        String currentUsername = "";

        if (row != -1) {
            String roleName = (String) roleTableModel.getValueAt(row, 0);
            for (UserRole r : UserRole.values()) {
                if (r.getDisplayName().equalsIgnoreCase(roleName)) {
                    targetRole = r;
                    break;
                }
            }
            currentUsername = (String) roleTableModel.getValueAt(row, 1);
        }

        if (targetRole == null) {
            targetRole = UserRole.ADMINISTRATOR;
        }

        JComboBox<UserRole> roleCombo = new JComboBox<>(UserRole.values());
        roleCombo.setSelectedItem(targetRole);

        JTextField usernameField = new JTextField(currentUsername.isEmpty() ? "admin" : currentUsername, 15);
        JPasswordField newPassField = new JPasswordField(15);
        JPasswordField confirmPassField = new JPasswordField(15);

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Target Role:"));
        panel.add(roleCombo);
        panel.add(new JLabel("New Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("New Password (min 4 chars):"));
        panel.add(newPassField);
        panel.add(new JLabel("Confirm Password:"));
        panel.add(confirmPassField);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Update Role Credentials",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String newUsername = usernameField.getText().trim();
            String newPassword = new String(newPassField.getPassword());
            String confirmPassword = new String(confirmPassField.getPassword());
            UserRole selectedRole = (UserRole) roleCombo.getSelectedItem();

            if (newUsername.isEmpty() || newPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username and password cannot be blank.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (newPassword.length() < 4) {
                JOptionPane.showMessageDialog(this, "Password must be at least 4 characters long.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                UserDB.updateUserCredentials(selectedRole, newUsername, newPassword);
                JOptionPane.showMessageDialog(this,
                        "Credentials for " + selectedRole.getDisplayName() + " have been successfully updated.",
                        "Credentials Updated",
                        JOptionPane.INFORMATION_MESSAGE);
                loadRoleUsers();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Error updating credentials", ex);
                JOptionPane.showMessageDialog(this, "Failed to update credentials: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
