package com.fixflow.ui;

import javax.swing.*;
import java.awt.*;

/**
 * In-application operational and financial reporting panel.
 * Reports covering repairs, revenue, and inventory will be implemented in Milestone 9.
 */
public class ReportsPanel extends BasePanel {

    public ReportsPanel() {
        super("Operational Reports", "Analyze repair job turnaround, cash revenue summaries, and spare-part consumption");
        initUI();
    }

    private void initUI() {
        contentArea.setLayout(new BorderLayout(0, 15));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JComboBox<String> reportSelector = new JComboBox<>(new String[]{
                "1. Repair Job Summary",
                "2. Revenue & Cash Collection",
                "3. Inventory & Low-Stock Status"
        });
        topBar.add(new JLabel("Select Report:"));
        topBar.add(reportSelector);
        topBar.add(new JButton("Generate"));

        JPanel placeholder = new JPanel(new GridBagLayout());
        JLabel info = new JLabel("In-app reporting views and date-range filters will be fully enabled in Milestone 9.");
        info.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        info.setForeground(Color.GRAY);
        placeholder.add(info);

        contentArea.add(topBar, BorderLayout.NORTH);
        contentArea.add(placeholder, BorderLayout.CENTER);
    }
}

