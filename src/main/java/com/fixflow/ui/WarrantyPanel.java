package com.fixflow.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 30-day repair warranty tracking and claim processing panel.
 * Full warranty validation and linked claim jobs will be implemented in Milestone 8.
 */
public class WarrantyPanel extends BasePanel {

    public WarrantyPanel() {
        super("Warranty Management", "Verify 30-day repair warranty eligibility and initiate linked warranty claims");
        initUI();
    }

    private void initUI() {
        contentArea.setLayout(new BorderLayout(0, 15));

        JPanel topBar = new JPanel(new BorderLayout());
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JTextField searchField = new JTextField(22);
        searchField.putClientProperty("JTextField.placeholderText", "Enter original Job ID or Customer Phone...");
        searchBox.add(searchField);
        searchBox.add(new JButton("Check Warranty"));

        topBar.add(searchBox, BorderLayout.WEST);

        JPanel placeholder = new JPanel(new GridBagLayout());
        JLabel info = new JLabel("30-day warranty verification and linked warranty claims will be enabled in Milestone 8.");
        info.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        info.setForeground(Color.GRAY);
        placeholder.add(info);

        contentArea.add(topBar, BorderLayout.NORTH);
        contentArea.add(placeholder, BorderLayout.CENTER);
    }
}

