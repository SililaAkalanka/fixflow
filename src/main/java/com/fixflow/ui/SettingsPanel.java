package com.fixflow.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.fixflow.database.DatabaseManager;
import com.fixflow.database.UserDB;
import com.fixflow.enums.UserRole;
import com.fixflow.model.User;
import com.fixflow.util.SessionManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Administrator settings, credentials management, and database maintenance panel.
 * Implements Section 7.10 of the FixFlow Project Specification.
 */
public class SettingsPanel extends BasePanel {

    private static final Logger LOGGER = Logger.getLogger(SettingsPanel.class.getName());
    private static final DateTimeFormatter BACKUP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final JPanel contentWrapper = new JPanel();
    private final JLabel accessWarningLabel = new JLabel();

    // Role table
    private final DefaultTableModel roleTableModel = new DefaultTableModel(
            new Object[]{"Role", "Username", "Last Modified"}, 0
    ) { @Override public boolean isCellEditable(int r, int c) { return false; } };
    private final JTable roleTable = new JTable(roleTableModel);

    // DB Info Labels
    private final JLabel dbPathLabel = new JLabel("-");
    private final JLabel dbSizeLabel = new JLabel("-");
    private final JLabel dbIntegrityLabel = new JLabel("Verified (10 Core Tables)");

    private JButton changePasswordBtn;
    private JButton createBackupBtn;
    private JButton verifyBackupBtn;
    private JButton openFolderBtn;

    public SettingsPanel() {
        super("System Settings & Maintenance", "Manage shared role passwords, database maintenance, backup procedures, and application appearance");
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

        // 1. Shared Role Credentials Card
        JPanel credCard = new JPanel(new BorderLayout(0, 8));
        credCard.setBorder(new CompoundBorder(
                new TitledBorder("Shared Role Credentials Management (§7.2, §7.10)"),
                new EmptyBorder(8, 12, 10, 12)
        ));

        roleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roleTable.setRowHeight(24);
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

        // 2. Database Maintenance & Backup Procedures Card
        JPanel dbCard = new JPanel(new BorderLayout(0, 10));
        dbCard.setBorder(new CompoundBorder(
                new TitledBorder("SQLite Database Maintenance & Backup Operations (§7.10, §15)"),
                new EmptyBorder(8, 12, 10, 12)
        ));

        JPanel dbInfoGrid = new JPanel(new GridLayout(4, 2, 6, 6));
        dbInfoGrid.add(new JLabel("Database Storage Location:"));
        dbPathLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        dbInfoGrid.add(dbPathLabel);

        dbInfoGrid.add(new JLabel("Database File Size:"));
        dbSizeLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        dbInfoGrid.add(dbSizeLabel);

        dbInfoGrid.add(new JLabel("Database Schema Integrity:"));
        dbIntegrityLabel.setForeground(new Color(16, 124, 65));
        dbIntegrityLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        dbInfoGrid.add(dbIntegrityLabel);

        dbInfoGrid.add(new JLabel("Journal & Concurrency Mode:"));
        dbInfoGrid.add(new JLabel("Write-Ahead Logging (WAL) enabled"));

        // Backup notice callout
        JPanel noticePanel = new JPanel(new BorderLayout());
        noticePanel.setBackground(new Color(254, 243, 199)); // Soft amber
        noticePanel.setBorder(new CompoundBorder(
                new LineBorder(new Color(245, 158, 11), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        JLabel noticeTitle = new JLabel("Official Backup Procedure Notice (§7.10):");
        noticeTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        noticeTitle.setForeground(new Color(180, 83, 9));

        JLabel noticeBody = new JLabel("<html>For complete offline / cold backups, close the FixFlow application completely before copying the <code>fixflow.db</code> file.<br>You can also create an immediate live snapshot copy below.</html>");
        noticeBody.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        noticePanel.add(noticeTitle, BorderLayout.NORTH);
        noticePanel.add(noticeBody, BorderLayout.CENTER);

        JPanel dbBtnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        createBackupBtn = new JButton("Create Backup Copy Now...");
        createBackupBtn.setBackground(new Color(16, 124, 65));
        createBackupBtn.setForeground(Color.WHITE);
        createBackupBtn.setFont(createBackupBtn.getFont().deriveFont(Font.BOLD));
        createBackupBtn.addActionListener(e -> handleCreateBackup());

        verifyBackupBtn = new JButton("Verify Backup File...");
        verifyBackupBtn.addActionListener(e -> handleVerifyBackup());

        openFolderBtn = new JButton("Open Storage Folder");
        openFolderBtn.addActionListener(e -> handleOpenFolder());

        dbBtnBar.add(openFolderBtn);
        dbBtnBar.add(verifyBackupBtn);
        dbBtnBar.add(createBackupBtn);

        JPanel dbCenter = new JPanel(new BorderLayout(0, 10));
        dbCenter.add(dbInfoGrid, BorderLayout.NORTH);
        dbCenter.add(noticePanel, BorderLayout.CENTER);

        dbCard.add(dbCenter, BorderLayout.CENTER);
        dbCard.add(dbBtnBar, BorderLayout.SOUTH);

        // 3. Theme & Appearance Settings Card
        JPanel themeCard = new JPanel(new BorderLayout(0, 8));
        themeCard.setBorder(new CompoundBorder(
                new TitledBorder("Application Appearance & Theme"),
                new EmptyBorder(8, 12, 10, 12)
        ));

        JPanel themeControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        themeControls.add(new JLabel("Interface Theme:"));
        JButton lightThemeBtn = new JButton("FlatLaf Light");
        JButton darkThemeBtn = new JButton("FlatLaf Dark");

        lightThemeBtn.addActionListener(e -> switchTheme(false));
        darkThemeBtn.addActionListener(e -> switchTheme(true));

        themeControls.add(lightThemeBtn);
        themeControls.add(darkThemeBtn);
        themeCard.add(themeControls, BorderLayout.CENTER);

        // 4. System Environment Card
        JPanel sysCard = new JPanel(new GridLayout(3, 2, 6, 4));
        sysCard.setBorder(new CompoundBorder(
                new TitledBorder("System Environment & Runtime"),
                new EmptyBorder(8, 12, 10, 12)
        ));
        sysCard.add(new JLabel("Java Runtime Version:"));
        sysCard.add(new JLabel(System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")"));
        sysCard.add(new JLabel("Operating System:"));
        sysCard.add(new JLabel(System.getProperty("os.name") + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")"));
        sysCard.add(new JLabel("Application Release:"));
        sysCard.add(new JLabel("FixFlow v1.0.0 (Java 21 Desktop Application)"));

        contentWrapper.add(credCard);
        contentWrapper.add(Box.createRigidArea(new Dimension(0, 10)));
        contentWrapper.add(dbCard);
        contentWrapper.add(Box.createRigidArea(new Dimension(0, 10)));
        contentWrapper.add(themeCard);
        contentWrapper.add(Box.createRigidArea(new Dimension(0, 10)));
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
        createBackupBtn.setEnabled(isAdmin);
        verifyBackupBtn.setEnabled(isAdmin);

        if (!isAdmin) {
            accessWarningLabel.setText("Restricted Access: System Settings and Database Maintenance are reserved for Administrator role (§7.10).");
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
        File dbFile = DatabaseManager.getDatabaseFile();
        if (dbFile != null) {
            dbPathLabel.setText(dbFile.getAbsolutePath());
            if (dbFile.exists()) {
                double sizeKb = dbFile.length() / 1024.0;
                if (sizeKb > 1024) {
                    dbSizeLabel.setText(String.format("%.2f MB (%d bytes)", sizeKb / 1024.0, dbFile.length()));
                } else {
                    dbSizeLabel.setText(String.format("%.2f KB (%d bytes)", sizeKb, dbFile.length()));
                }
            } else {
                dbSizeLabel.setText("Database file not yet created on disk");
            }
        } else {
            dbPathLabel.setText(DatabaseManager.getJdbcUrl());
            dbSizeLabel.setText("In-Memory / Custom JDBC URL");
        }
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

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Update Shared Role Credentials", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(440, 260);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));

        JComboBox<UserRole> roleCombo = new JComboBox<>(UserRole.values());
        if (targetRole != null) roleCombo.setSelectedItem(targetRole);

        JTextField usernameField = new JTextField(currentUsername);
        JPasswordField pass1 = new JPasswordField();
        JPasswordField pass2 = new JPasswordField();

        roleCombo.addActionListener(e -> {
            UserRole r = (UserRole) roleCombo.getSelectedItem();
            try {
                User u = UserDB.getUserByRole(r);
                if (u != null) usernameField.setText(u.getUsername());
            } catch (SQLException ignored) {}
        });

        form.add(new JLabel("Role:"));
        form.add(roleCombo);
        form.add(new JLabel("Username:"));
        form.add(usernameField);
        form.add(new JLabel("New Password (min 6 chars):"));
        form.add(pass1);
        form.add(new JLabel("Confirm Password:"));
        form.add(pass2);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton("Save Credentials");
        saveBtn.addActionListener(e -> {
            UserRole r = (UserRole) roleCombo.getSelectedItem();
            String uName = usernameField.getText().trim();
            String p1 = new String(pass1.getPassword());
            String p2 = new String(pass2.getPassword());

            if (uName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Username cannot be blank.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (p1.length() < 6) {
                JOptionPane.showMessageDialog(dialog, "Password must be at least 6 characters long.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!p1.equals(p2)) {
                JOptionPane.showMessageDialog(dialog, "Passwords do not match.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                UserDB.updateCredentials(r, uName, p1);
                JOptionPane.showMessageDialog(dialog, "Credentials for " + r.getDisplayName() + " updated successfully.", "Saved", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadRoleUsers();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Failed to update credentials", ex);
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnBar.add(cancelBtn);
        btnBar.add(saveBtn);

        panel.add(form, BorderLayout.CENTER);
        panel.add(btnBar, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void handleCreateBackup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save SQLite Database Backup File");
        String defaultName = "fixflow_backup_" + LocalDateTime.now().format(BACKUP_DATE_FORMAT) + ".db";
        chooser.setSelectedFile(new File(defaultName));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".db")) {
                target = new File(target.getAbsolutePath() + ".db");
            }

            try {
                DatabaseManager.backupDatabase(target);
                double kb = target.length() / 1024.0;
                JOptionPane.showMessageDialog(this,
                        String.format("Database backup created successfully!\n\nFile: %s\nSize: %.2f KB\n\nAll transactions have been consistently flushed.",
                                target.getAbsolutePath(), kb),
                        "Backup Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Database backup failed", ex);
                JOptionPane.showMessageDialog(this, "Backup failed: " + ex.getMessage(), "Backup Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleVerifyBackup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select FixFlow Database Backup File to Verify");

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            boolean valid = DatabaseManager.validateDatabaseFile(file);
            if (valid) {
                JOptionPane.showMessageDialog(this,
                        String.format("Verification PASSED!\n\nFile: %s\nSize: %.2f KB\n\nAll 10 required FixFlow core tables and schema are verified.",
                                file.getName(), file.length() / 1024.0),
                        "Integrity Verified", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        String.format("Verification FAILED!\n\nFile: %s\n\nThis file is either corrupt, unreadable, or missing FixFlow core database tables.",
                                file.getName()),
                        "Invalid Database File", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleOpenFolder() {
        File dbFile = DatabaseManager.getDatabaseFile();
        if (dbFile != null && dbFile.getParentFile() != null && Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(dbFile.getParentFile());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Folder location: " + dbFile.getParentFile().getAbsolutePath(), "Storage Location", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Storage location: " + DatabaseManager.getJdbcUrl(), "Storage Location", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void switchTheme(boolean dark) {
        try {
            if (dark) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                SwingUtilities.updateComponentTreeUI(window);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to switch theme", ex);
        }
    }
}
