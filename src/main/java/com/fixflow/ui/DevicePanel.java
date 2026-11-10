package com.fixflow.ui;

import com.fixflow.enums.NavigationItem;
import com.fixflow.util.SessionManager;

import javax.swing.*;
import java.awt.*;

/**
 * Electronic device management panel.
 * Full CRUD and category filtering will be populated in Milestone 5.
 */
public class DevicePanel extends BasePanel {

    private final JLabel accessBadge = new JLabel();

    public DevicePanel() {
        super("Device Registry", "Track electronic devices, serial numbers, categories, and service histories");
        initUI();
    }

    private void initUI() {
        contentArea.setLayout(new BorderLayout(0, 15));

        JPanel topBar = new JPanel(new BorderLayout());
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JTextField searchField = new JTextField(24);
        searchField.putClientProperty("JTextField.placeholderText", "Search by brand, model, serial, or customer...");
        JButton searchBtn = new JButton("Search");
        searchBox.add(searchField);
        searchBox.add(searchBtn);

        topBar.add(searchBox, BorderLayout.WEST);
        topBar.add(accessBadge, BorderLayout.EAST);

        JPanel placeholder = new JPanel(new GridBagLayout());
        JLabel info = new JLabel("Device registry and category catalog will be fully enabled in Milestone 5.");
        info.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        info.setForeground(Color.GRAY);
        placeholder.add(info);

        contentArea.add(topBar, BorderLayout.NORTH);
        contentArea.add(placeholder, BorderLayout.CENTER);
    }

    @Override
    public void onPanelShown() {
        boolean readOnly = NavigationItem.DEVICES.isReadOnly(SessionManager.getCurrentRole());
        accessBadge.setText(readOnly ? "Mode: View Only" : "Mode: Full Access");
        accessBadge.setForeground(readOnly ? new Color(180, 100, 0) : new Color(16, 124, 65));
    }
}

