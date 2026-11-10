package com.fixflow.ui;

import com.fixflow.enums.NavigationItem;
import com.fixflow.util.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Customer management panel.
 * Full CRUD and search capabilities will be populated in Milestone 5.
 */
public class CustomerPanel extends BasePanel {

    private final JLabel accessBadge = new JLabel();

    public CustomerPanel() {
        super("Customer Directory", "Register, update, and search customer records and associated repair history");
        initUI();
    }

    private void initUI() {
        contentArea.setLayout(new BorderLayout(0, 15));

        JPanel topBar = new JPanel(new BorderLayout());
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JTextField searchField = new JTextField(24);
        searchField.putClientProperty("JTextField.placeholderText", "Search by customer name, phone, or ID...");
        JButton searchBtn = new JButton("Search");
        searchBox.add(searchField);
        searchBox.add(searchBtn);

        topBar.add(searchBox, BorderLayout.WEST);
        topBar.add(accessBadge, BorderLayout.EAST);

        // Placeholder content
        JPanel placeholder = new JPanel(new GridBagLayout());
        JLabel info = new JLabel("Customer directory will be fully enabled in Milestone 5 (CRUD & Search).");
        info.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        info.setForeground(Color.GRAY);
        placeholder.add(info);

        contentArea.add(topBar, BorderLayout.NORTH);
        contentArea.add(placeholder, BorderLayout.CENTER);
    }

    @Override
    public void onPanelShown() {
        boolean readOnly = NavigationItem.CUSTOMERS.isReadOnly(SessionManager.getCurrentRole());
        accessBadge.setText(readOnly ? "Mode: View Only" : "Mode: Full Access");
        accessBadge.setForeground(readOnly ? new Color(180, 100, 0) : new Color(16, 124, 65));
    }
}

