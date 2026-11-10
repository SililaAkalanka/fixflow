package com.fixflow.ui;

import com.fixflow.enums.NavigationItem;
import com.fixflow.util.SessionManager;

import javax.swing.*;
import java.awt.*;

/**
 * Technician administration panel.
 * Technician CRUD, specializations, and deactivation will be implemented in Milestone 5.
 */
public class TechnicianPanel extends BasePanel {

    private final JLabel accessBadge = new JLabel();

    public TechnicianPanel() {
        super("Technician Directory", "Manage technical staff, device specializations, and employment status");
        initUI();
    }

    private void initUI() {
        contentArea.setLayout(new BorderLayout(0, 15));

        JPanel topBar = new JPanel(new BorderLayout());
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JTextField searchField = new JTextField(20);
        searchField.putClientProperty("JTextField.placeholderText", "Search technicians...");
        searchBox.add(searchField);
        searchBox.add(new JButton("Search"));

        topBar.add(searchBox, BorderLayout.WEST);
        topBar.add(accessBadge, BorderLayout.EAST);

        JPanel placeholder = new JPanel(new GridBagLayout());
        JLabel info = new JLabel("Technician directory and assignment management will be fully enabled in Milestone 5.");
        info.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        info.setForeground(Color.GRAY);
        placeholder.add(info);

        contentArea.add(topBar, BorderLayout.NORTH);
        contentArea.add(placeholder, BorderLayout.CENTER);
    }

    @Override
    public void onPanelShown() {
        boolean readOnly = NavigationItem.TECHNICIANS.isReadOnly(SessionManager.getCurrentRole());
        accessBadge.setText(readOnly ? "Mode: View Only (Assignments restricted)" : "Mode: Full Management");
        accessBadge.setForeground(readOnly ? new Color(180, 100, 0) : new Color(16, 124, 65));
    }
}

