package com.fixflow.ui;

import com.fixflow.database.UserDB;
import com.fixflow.enums.UserRole;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * First-Launch Setup Wizard for configuring Administrator and Technician credentials.
 */
public class SetupFrame extends JFrame {

    private final JTextField adminUserField = new JTextField("admin", 18);
    private final JPasswordField adminPassField = new JPasswordField(18);
    private final JPasswordField adminConfirmPassField = new JPasswordField(18);

    private final JTextField techUserField = new JTextField("tech", 18);
    private final JPasswordField techPassField = new JPasswordField(18);

    public SetupFrame() {
        super("FixFlow – Initial System Setup");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(560, 520);
        setMinimumSize(new Dimension(500, 460));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("FixFlow Initial Setup");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descLabel = new JLabel("Configure initial credentials for Administrator and Technician accounts.");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descLabel.setForeground(Color.GRAY);
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        headerPanel.add(descLabel);

        // Form Fields in ScrollPane
        JPanel formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));

        formContainer.add(createRolePanel("1. Administrator (Full System Access)", UserRole.ADMINISTRATOR, adminUserField, adminPassField, adminConfirmPassField));
        formContainer.add(Box.createRigidArea(new Dimension(0, 12)));
        formContainer.add(createRolePanel("2. Technician (Diagnostics & Repair Tasks)", UserRole.TECHNICIAN, techUserField, techPassField, null));

        JScrollPane scrollPane = new JScrollPane(formContainer);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(e -> System.exit(0));

        JButton saveButton = new JButton("Complete Setup & Continue to Login");
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveButton.putClientProperty("JButton.buttonType", "roundRect");
        saveButton.addActionListener(e -> handleSetupCompletion());

        buttonPanel.add(exitButton);
        buttonPanel.add(saveButton);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createRolePanel(String title, UserRole role, JTextField userField, JPasswordField passField, JPasswordField confirmPassField) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new CompoundBorder(
                new TitledBorder(title),
                new EmptyBorder(8, 12, 10, 12)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 6, 4, 6);

        // Username
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        panel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add(userField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        panel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add(passField, gbc);

        // Optional Confirm Password (for Administrator)
        if (confirmPassField != null) {
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.weightx = 0.3;
            panel.add(new JLabel("Confirm Password:"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.7;
            panel.add(confirmPassField, gbc);
        }

        return panel;
    }

    private void handleSetupCompletion() {
        String adminUser = adminUserField.getText().trim();
        String adminPass = new String(adminPassField.getPassword());
        String adminConfirmPass = new String(adminConfirmPassField.getPassword());

        String techUser = techUserField.getText().trim();
        String techPass = new String(techPassField.getPassword());

        // Validation: Required fields
        if (adminUser.isEmpty() || adminPass.isEmpty() || techUser.isEmpty() || techPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter usernames and passwords for both Administrator and Technician roles.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validation: Admin password match
        if (!adminPass.equals(adminConfirmPass)) {
            JOptionPane.showMessageDialog(this, "Administrator passwords do not match. Please re-enter.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validation: Minimum length
        if (adminPass.length() < 4 || techPass.length() < 4) {
            JOptionPane.showMessageDialog(this, "Passwords must be at least 4 characters in length.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validation: Unique usernames
        Set<String> usernames = new HashSet<>();
        usernames.add(adminUser.toLowerCase());
        usernames.add(techUser.toLowerCase());

        if (usernames.size() < 2) {
            JOptionPane.showMessageDialog(this, "Each role must have a distinct, unique username.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Save credentials
        Map<UserRole, UserDB.RoleCredential> credentials = new EnumMap<>(UserRole.class);
        credentials.put(UserRole.ADMINISTRATOR, new UserDB.RoleCredential(adminUser, adminPass));
        credentials.put(UserRole.TECHNICIAN, new UserDB.RoleCredential(techUser, techPass));

        try {
            UserDB.setupAllRoles(credentials);
            JOptionPane.showMessageDialog(this,
                    "FixFlow initial setup has been successfully completed!\nYou may now sign in using your credentials.",
                    "Setup Complete",
                    JOptionPane.INFORMATION_MESSAGE);

            dispose();
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            });
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to save credentials: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
