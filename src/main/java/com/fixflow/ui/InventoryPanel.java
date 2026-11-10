package com.fixflow.ui;

import com.fixflow.enums.NavigationItem;
import com.fixflow.util.SessionManager;

import javax.swing.*;
import java.awt.*;

/**
 * Spare-part inventory and stock tracking panel.
 * Part CRUD, stock adjustments, and transactional deductions will be implemented in Milestone 7.
 */
public class InventoryPanel extends BasePanel {

    private final JLabel accessBadge = new JLabel();

    public InventoryPanel() {
        super("Spare-Part Inventory", "Manage replacement parts, compatibility catalogs, stock levels, and reorder alerts");
        initUI();
    }

    private void initUI() {
        contentArea.setLayout(new BorderLayout(0, 15));

        JPanel topBar = new JPanel(new BorderLayout());
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JTextField searchField = new JTextField(24);
        searchField.putClientProperty("JTextField.placeholderText", "Search parts by name, model, or category...");
        searchBox.add(searchField);
        searchBox.add(new JButton("Search"));

        topBar.add(searchBox, BorderLayout.WEST);
        topBar.add(accessBadge, BorderLayout.EAST);

        JPanel placeholder = new JPanel(new GridBagLayout());
        JLabel info = new JLabel("Spare-part catalog, stock adjustments, and usage tracking will be fully enabled in Milestone 7.");
        info.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        info.setForeground(Color.GRAY);
        placeholder.add(info);

        contentArea.add(topBar, BorderLayout.NORTH);
        contentArea.add(placeholder, BorderLayout.CENTER);
    }

    @Override
    public void onPanelShown() {
        boolean readOnly = NavigationItem.INVENTORY.isReadOnly(SessionManager.getCurrentRole());
        accessBadge.setText(readOnly ? "Mode: View Only (Part stock changes restricted)" : "Mode: Stock Management Enabled");
        accessBadge.setForeground(readOnly ? new Color(180, 100, 0) : new Color(16, 124, 65));
    }
}

