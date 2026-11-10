package com.fixflow.ui;

import com.fixflow.enums.UserRole;
import com.fixflow.model.User;
import com.fixflow.util.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Main application dashboard window after successful authentication.
 * Displays active role identity and provides logout capability.
 * Full tabbed navigation will be expanded in Milestone 4.
 */
public class DashboardFrame extends JFrame {

    public DashboardFrame() {
        super("FixFlow – Smart Repair Workflow & Warranty Tracking");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 560);
        setMinimumSize(new Dimension(700, 450));
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Top Navigation Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));
        topBar.setPreferredSize(new Dimension(800, 50));

        JLabel logoLabel = new JLabel("FixFlow");
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));

        User currentUser = SessionManager.getCurrentUser();
        UserRole currentRole = SessionManager.getCurrentRole();

        String roleDisplay = currentRole != null ? currentRole.getDisplayName() : "Unknown";
        String userDisplay = currentUser != null ? currentUser.getUsername() : "User";

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 5));
        JLabel userBadge = new JLabel("Signed in as: " + userDisplay + " (" + roleDisplay + ")");
        userBadge.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JButton logoutButton = new JButton("Sign Out");
        logoutButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        logoutButton.addActionListener(e -> handleLogout());

        userPanel.add(userBadge);
        userPanel.add(logoutButton);

        topBar.add(logoLabel, BorderLayout.WEST);
        topBar.add(userPanel, BorderLayout.EAST);

        // Center Content Placeholder
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(50, 40, 50, 40));

        JLabel welcomeLabel = new JLabel("Welcome, " + roleDisplay + "!");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel authSuccessLabel = new JLabel("Authentication verified successfully.");
        authSuccessLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        authSuccessLabel.setForeground(new Color(34, 139, 34));
        authSuccessLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel roleSummaryLabel = new JLabel("Role Capabilities: " + getRoleSummary(currentRole));
        roleSummaryLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        roleSummaryLabel.setForeground(Color.GRAY);
        roleSummaryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        contentPanel.add(Box.createVerticalGlue());
        contentPanel.add(welcomeLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(authSuccessLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 16)));
        contentPanel.add(roleSummaryLabel);
        contentPanel.add(Box.createVerticalGlue());

        mainPanel.add(topBar, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        setContentPane(mainPanel);
    }

    private String getRoleSummary(UserRole role) {
        if (role == null) return "";
        return switch (role) {
            case ADMINISTRATOR -> "Initial setup, shared credentials, system settings, record deletion";
            case MANAGER -> "Operational monitoring, reporting, repair job & technician oversight";
            case RECEPTIONIST -> "Customer & device intake, repair job creation, customer communication";
            case TECHNICIAN -> "Device diagnostics, repair progress updates, parts usage";
            case INVENTORY_STAFF -> "Spare-part inventory tracking, stock adjustments, part catalog";
        };
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to sign out?",
                "Confirm Sign Out",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            SessionManager.logout();
            dispose();
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            });
        }
    }
}

