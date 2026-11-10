package com.fixflow.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Payment recording and invoice generation panel.
 * Cash payments and PDF invoice generation will be implemented in Milestone 8.
 */
public class PaymentPanel extends BasePanel {

    public PaymentPanel() {
        super("Payments & Invoices", "Record full cash payments, review labor and part charges, and print PDF invoices");
        initUI();
    }

    private void initUI() {
        contentArea.setLayout(new BorderLayout(0, 15));

        JPanel topBar = new JPanel(new BorderLayout());
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JTextField searchField = new JTextField(20);
        searchField.putClientProperty("JTextField.placeholderText", "Search by job ID or customer...");
        searchBox.add(searchField);
        searchBox.add(new JButton("Search"));

        topBar.add(searchBox, BorderLayout.WEST);

        JPanel placeholder = new JPanel(new GridBagLayout());
        JLabel info = new JLabel("Full cash payment processing and PDF invoice generation will be enabled in Milestone 8.");
        info.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        info.setForeground(Color.GRAY);
        placeholder.add(info);

        contentArea.add(topBar, BorderLayout.NORTH);
        contentArea.add(placeholder, BorderLayout.CENTER);
    }
}

