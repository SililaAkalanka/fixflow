package com.fixflow.ui;

import com.fixflow.database.ReportDB;
import com.fixflow.enums.RepairStatus;
import com.fixflow.enums.UserRole;
import com.fixflow.model.DashboardMetrics;
import com.fixflow.util.SessionManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Operational Dashboard presenting real-time business KPIs, status distribution,
 * financial summaries, and inventory alerts tailored to the logged-in role.
 * Implements Section 7.8 of the FixFlow Project Specification.
 */
public class DashboardPanel extends BasePanel {

    private final JLabel welcomeLabel = new JLabel();
    private final JLabel roleBadgeLabel = new JLabel();

    // Metric Value Labels
    private final JLabel totalJobsVal = new JLabel("0");
    private final JLabel activeJobsVal = new JLabel("0");
    private final JLabel waitingPartsVal = new JLabel("0");
    private final JLabel readyCollectionVal = new JLabel("0");
    private final JLabel totalRevenueVal = new JLabel("$0.00");
    private final JLabel lowStockVal = new JLabel("0");

    // Technician Specific
    private final JPanel techCardPanel = new JPanel(new GridLayout(1, 2, 16, 0));
    private final JLabel techActiveVal = new JLabel("0");
    private final JLabel techDoneTodayVal = new JLabel("0");

    // Breakdown Labels
    private final JLabel receivedCountLbl = new JLabel("0");
    private final JLabel diagnosingCountLbl = new JLabel("0");
    private final JLabel waitingApprovalCountLbl = new JLabel("0");
    private final JLabel repairingCountLbl = new JLabel("0");
    private final JLabel collectedCountLbl = new JLabel("0");
    private final JLabel cancelledCountLbl = new JLabel("0");

    // Financial & Inventory Overview Labels
    private final JLabel todayIntakesLbl = new JLabel("0");
    private final JLabel todayRevenueLbl = new JLabel("$0.00");
    private final JLabel pendingReceivablesLbl = new JLabel("$0.00");
    private final JLabel inventoryValuationLbl = new JLabel("$0.00");
    private final JLabel outOfStockLbl = new JLabel("0");

    public DashboardPanel() {
        super("Operational Dashboard", "Centralized real-time overview of repairs, financial KPIs, and inventory health");
        initDashboard();
    }

    private void initDashboard() {
        contentArea.setLayout(new BorderLayout(0, 16));

        // 1. Welcome Banner
        JPanel bannerPanel = new JPanel(new BorderLayout(15, 0));
        bannerPanel.setBorder(new CompoundBorder(
                new LineBorder(new Color(210, 215, 220), 1, true),
                new EmptyBorder(14, 18, 14, 18)
        ));

        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        roleBadgeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        roleBadgeLabel.setForeground(new Color(100, 110, 120));

        JPanel textStack = new JPanel(new GridLayout(2, 1, 0, 3));
        textStack.setOpaque(false);
        textStack.add(welcomeLabel);
        textStack.add(roleBadgeLabel);

        JButton refreshBtn = new JButton("Refresh Dashboard");
        refreshBtn.addActionListener(e -> refreshMetrics());

        bannerPanel.add(textStack, BorderLayout.WEST);
        bannerPanel.add(refreshBtn, BorderLayout.EAST);

        // 2. Center Content: Metric Cards & Breakdown Grid
        JPanel centerContainer = new JPanel();
        centerContainer.setLayout(new BoxLayout(centerContainer, BoxLayout.Y_AXIS));

        // Metric Cards (3x2 Grid)
        JPanel cardsGrid = new JPanel(new GridLayout(2, 3, 14, 14));
        cardsGrid.add(createMetricCard("Total Repair Volume", "All-time recorded repair jobs", totalJobsVal, new Color(0, 102, 204)));
        cardsGrid.add(createMetricCard("Active In-Progress", "Repairs currently in shop", activeJobsVal, new Color(0, 120, 215)));
        cardsGrid.add(createMetricCard("Waiting for Parts", "Blocked pending component arrival", waitingPartsVal, new Color(216, 59, 1)));
        cardsGrid.add(createMetricCard("Ready for Collection", "Completed & awaiting payment/pickup", readyCollectionVal, new Color(16, 124, 65)));
        cardsGrid.add(createMetricCard("Total Revenue Settled", "Cash collected from completed jobs", totalRevenueVal, new Color(11, 107, 79)));
        cardsGrid.add(createMetricCard("Low Stock Alerts", "Parts at or below reorder threshold", lowStockVal, new Color(177, 24, 1)));

        // Technician Special Cards (shown if technician)
        techCardPanel.setBorder(new EmptyBorder(12, 0, 0, 0));
        techCardPanel.add(createMetricCard("My Assigned Workload", "Active jobs assigned to you", techActiveVal, new Color(0, 120, 215)));
        techCardPanel.add(createMetricCard("My Completed Today", "Jobs you finished today", techDoneTodayVal, new Color(16, 124, 65)));
        techCardPanel.setVisible(false);

        // 3. Lower Section: Status Breakdown & Financial Health
        JPanel lowerSection = new JPanel(new GridLayout(1, 2, 16, 0));
        lowerSection.setBorder(new EmptyBorder(16, 0, 0, 0));

        // Left Box: Status Lifecycle Breakdown
        JPanel statusBox = new JPanel(new GridLayout(6, 2, 8, 8));
        statusBox.setBorder(new CompoundBorder(
                new TitledBorder("Repair Lifecycle Distribution"),
                new EmptyBorder(8, 12, 8, 12)
        ));
        statusBox.add(new JLabel("Received (Pending Diagnostics):"));
        statusBox.add(receivedCountLbl);
        statusBox.add(new JLabel("Diagnosing:"));
        statusBox.add(diagnosingCountLbl);
        statusBox.add(new JLabel("Waiting for Approval:"));
        statusBox.add(waitingApprovalCountLbl);
        statusBox.add(new JLabel("Actively Repairing:"));
        statusBox.add(repairingCountLbl);
        statusBox.add(new JLabel("Collected & Closed:"));
        statusBox.add(collectedCountLbl);
        statusBox.add(new JLabel("Cancelled Jobs:"));
        statusBox.add(cancelledCountLbl);

        // Right Box: Financial & Inventory Health
        JPanel financeBox = new JPanel(new GridLayout(5, 2, 8, 8));
        financeBox.setBorder(new CompoundBorder(
                new TitledBorder("Financial & Inventory Health"),
                new EmptyBorder(8, 12, 8, 12)
        ));
        financeBox.add(new JLabel("Today's Intakes:"));
        financeBox.add(todayIntakesLbl);
        financeBox.add(new JLabel("Today's Cash Settled:"));
        todayRevenueLbl.setForeground(new Color(16, 124, 65));
        todayRevenueLbl.setFont(todayRevenueLbl.getFont().deriveFont(Font.BOLD));
        financeBox.add(todayRevenueLbl);
        financeBox.add(new JLabel("Pending Receivables (Ready for Collection):"));
        pendingReceivablesLbl.setForeground(new Color(210, 110, 0));
        pendingReceivablesLbl.setFont(pendingReceivablesLbl.getFont().deriveFont(Font.BOLD));
        financeBox.add(pendingReceivablesLbl);
        financeBox.add(new JLabel("Total Inventory Asset Valuation:"));
        financeBox.add(inventoryValuationLbl);
        financeBox.add(new JLabel("Out of Stock Spare Parts:"));
        outOfStockLbl.setForeground(new Color(180, 20, 20));
        financeBox.add(outOfStockLbl);

        lowerSection.add(statusBox);
        lowerSection.add(financeBox);

        centerContainer.add(cardsGrid);
        centerContainer.add(techCardPanel);
        centerContainer.add(lowerSection);

        contentArea.add(bannerPanel, BorderLayout.NORTH);
        contentArea.add(new JScrollPane(centerContainer), BorderLayout.CENTER);
    }

    private JPanel createMetricCard(String title, String subtitle, JLabel valLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(225, 228, 232), 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLabel.setForeground(accentColor);

        JLabel subLabel = new JLabel(subtitle);
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subLabel.setForeground(Color.GRAY);

        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));

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
        roleBadgeLabel.setText("Active Role: " + (role != null ? role.getDisplayName() : "Unknown") + " — Real-time operational oversight");

        boolean isTech = (role == UserRole.TECHNICIAN);
        techCardPanel.setVisible(isTech);

        refreshMetrics();
    }

    public void refreshMetrics() {
        Integer techId = null;
        if (SessionManager.hasRole(UserRole.TECHNICIAN)) {
            techId = SessionManager.getCurrentUserId();
        }

        DashboardMetrics m = ReportDB.getDashboardMetrics(techId);

        totalJobsVal.setText(String.valueOf(m.getTotalJobs()));
        activeJobsVal.setText(String.valueOf(m.getActiveJobs()));
        waitingPartsVal.setText(String.valueOf(m.getWaitingPartsJobs()));
        readyCollectionVal.setText(String.valueOf(m.getReadyForCollectionJobs()));
        totalRevenueVal.setText(String.format("$%.2f", m.getTotalRevenue()));
        lowStockVal.setText(String.valueOf(m.getLowStockCount()));

        // Technician cards
        techActiveVal.setText(String.valueOf(m.getTechAssignedActive()));
        techDoneTodayVal.setText(String.valueOf(m.getTechAssignedCompletedToday()));

        // Status breakdown
        receivedCountLbl.setText(String.valueOf(m.getStatusCount(RepairStatus.RECEIVED)));
        diagnosingCountLbl.setText(String.valueOf(m.getStatusCount(RepairStatus.DIAGNOSING)));
        waitingApprovalCountLbl.setText(String.valueOf(m.getStatusCount(RepairStatus.WAITING_FOR_APPROVAL)));
        repairingCountLbl.setText(String.valueOf(m.getStatusCount(RepairStatus.REPAIRING)));
        collectedCountLbl.setText(String.valueOf(m.getStatusCount(RepairStatus.COLLECTED)));
        cancelledCountLbl.setText(String.valueOf(m.getStatusCount(RepairStatus.CANCELLED)));

        // Financial & inventory health
        todayIntakesLbl.setText(String.valueOf(m.getTodayJobs()) + " devices");
        todayRevenueLbl.setText(String.format("$%.2f", m.getTodayRevenue()));
        pendingReceivablesLbl.setText(String.format("$%.2f", m.getPendingRevenue()));
        inventoryValuationLbl.setText(String.format("$%.2f", m.getTotalInventoryValuation()));
        outOfStockLbl.setText(String.valueOf(m.getOutOfStockCount()) + " parts");
    }
}
