package com.fixflow.ui;

import com.fixflow.enums.UserRole;
import com.fixflow.util.SessionManager;

import javax.swing.*;
import java.awt.*;

/**
 * Core repair management panel.
 * Full status transition workflows, diagnostic notes, and approval handling will be implemented in Milestone 6.
 */
public class RepairPanel extends BasePanel {

    private final JLabel roleScopeLabel = new JLabel();

    public RepairPanel() {
        super("Repair Job Workflow", "Manage device intakes, diagnostics, customer approvals, and repair progression");
        initUI();
    }

    private void initUI() {
        contentArea.setLayout(new BorderLayout(0, 15));

        JPanel topBar = new JPanel(new BorderLayout());
        JPanel filterBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JComboBox<String> statusFilter = new JComboBox<>(new String[]{
                "All Statuses", "Received", "Diagnosing", "Waiting for Approval",
                "Waiting for Parts", "Repairing", "Ready for Collection", "Collected", "Cancelled"
        });
        filterBox.add(new JLabel("Filter by Status:"));
        filterBox.add(statusFilter);

        topBar.add(filterBox, BorderLayout.WEST);
        topBar.add(roleScopeLabel, BorderLayout.EAST);

        JPanel placeholder = new JPanel(new GridBagLayout());
        JLabel info = new JLabel("Repair job lifecycle and status transition matrix will be fully enabled in Milestone 6.");
        info.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        info.setForeground(Color.GRAY);
        placeholder.add(info);

        contentArea.add(topBar, BorderLayout.NORTH);
        contentArea.add(placeholder, BorderLayout.CENTER);
    }

    @Override
    public void onPanelShown() {
        UserRole role = SessionManager.getCurrentRole();
        if (role == UserRole.TECHNICIAN) {
            roleScopeLabel.setText("Scope: Showing assigned repair jobs");
            roleScopeLabel.setForeground(new Color(0, 120, 215));
        } else if (role == UserRole.RECEPTIONIST) {
            roleScopeLabel.setText("Scope: Full job intake & customer communications");
            roleScopeLabel.setForeground(new Color(16, 124, 65));
        } else {
            roleScopeLabel.setText("Scope: Full administrative oversight");
            roleScopeLabel.setForeground(new Color(100, 100, 100));
        }
    }
}

