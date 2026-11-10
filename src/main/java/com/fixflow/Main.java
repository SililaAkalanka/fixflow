package com.fixflow;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

/**
 * Application entry point for FixFlow – Smart Repair Workflow & Warranty Tracking System.
 */
public class Main {

    public static void main(String[] args) {
        // Apply FlatLaf Light Look and Feel by default
        FlatLightLaf.setup();

        // Launch user interface on the Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    /**
     * Initializes and displays the primary application window.
     */
    private static void createAndShowGUI() {
        JFrame frame = new JFrame("FixFlow – Smart Repair Workflow");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 520);
        frame.setMinimumSize(new Dimension(640, 420));
        frame.setLocationRelativeTo(null); // Center on screen

        // Main content container
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(60, 40, 60, 40));

        JLabel titleLabel = new JLabel("FixFlow");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Smart Repair Workflow & Warranty Tracking System");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitleLabel.setForeground(Color.GRAY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel statusLabel = new JLabel("Milestone 1: Project foundation initialized successfully.");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        contentPanel.add(Box.createVerticalGlue());
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        contentPanel.add(subtitleLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 24)));
        contentPanel.add(statusLabel);
        contentPanel.add(Box.createVerticalGlue());

        frame.setContentPane(contentPanel);
        frame.setVisible(true);
    }
}

