package com.fixflow.ui;

import com.fixflow.enums.UserRole;
import com.fixflow.util.SessionManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Dashboard screen presenting role-relevant operational KPIs.
 */
public class DashboardPanel extends BasePanel {

    private final JLabel welcomeLabel = new JLabel();
    private final JLabel roleBadgeLabel = new JLabel();

    public DashboardPanel() {
        super("Operational Dashboard", "Centralized overview of repairs, inventory alerts, and operational activities");
        initDashboard();
    }

    private void initDashboard() {
        contentArea.setLayout(new BorderLayout(0, 20));

        // Welcome Banner
        JPanel bannerPanel = new JPanel(new BorderLayout(15, 0));
        bannerPanel.setBorder(new CompoundBorder(
                new LineBorder(new Color(210, 215, 220), 1, true),
                new EmptyBorder(16, 20, 16, 20)
        ));

        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        roleBadgeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        roleBadgeLabel.setForeground(Color.GRAY);

        JPanel textStack = new JPanel(new GridLayout(2, 1, 0, 4));
        textStack.setOpaque(false);
        textStack.add(welcomeLabel);
        textStack.add(roleBadgeLabel);

        bannerPanel.add(textStack, BorderLayout.WEST);

        // Metric Cards Grid
        JPanel cardsGrid = new JPanel(new GridLayout(2, 2, 16, 16));
        cardsGrid.add(createMetricCard("Open Repair Jobs", "Active jobs in progress", "–", new Color(0, 120, 215)));
        cardsGrid.add(createMetricCard("Waiting for Parts", "Repairs pending spare-part arrival", "–", new Color(216, 59, 1)));
        cardsGrid.add(createMetricCard("Ready for Collection", "Completed repairs awaiting cash payment", "–", new Color(16, 124, 65)));
        cardsGrid.add(createMetricCard("Low Stock Alerts", "Parts at or below minimum threshold", "–", new Color(177, 24, 1)));

        contentArea.add(bannerPanel, BorderLayout.NORTH);
        contentArea.add(cardsGrid, BorderLayout.CENTER);
    }

    private JPanel createMetricCard(String title, String subtitle, String value, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(225, 228, 232), 1, true),
                new EmptyBorder(18, 20, 18, 20)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(accentColor);

        JLabel subLabel = new JLabel(subtitle);
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLabel.setForeground(Color.GRAY);

        JLabel valLabel = new JLabel(value);
        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));

        JPanel topSection = new JPanel(new GridLayout(2, 1, 0, 2));
        topSection.setOpaque(false);
        topSection.add(titleLabel);
        topSection.add(subLabel);

        card.add(topSection, BorderLayout.NORTH);
        card.add(valLabel, BorderLayout.CENTER);

        return card;
    }

    @Override
    public void onPanelShown() {
        UserRole role = SessionManager.getCurrentRole();
        String username = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getUsername() : "User";
        welcomeLabel.setText("Welcome back, " + username + "!");
        roleBadgeLabel.setText("Active Role: " + (role != null ? role.getDisplayName() : "Unknown") + " — Showing operational metrics");
    }
}

