package com.fixflow;

import com.fixflow.database.DatabaseManager;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application entry point for FixFlow – Smart Repair Workflow & Warranty Tracking System.
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // Apply FlatLaf Light Look and Feel by default
        FlatLightLaf.setup();

        // Initialize SQLite local database and create schema
        boolean dbReady = false;
        try {
            DatabaseManager.initializeDatabase();
            dbReady = true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize SQLite database.", e);
            JOptionPane.showMessageDialog(
                    null,
                    "FixFlow was unable to initialize its local database.\nPlease verify file permissions and restart the application.\n\nError: " + e.getMessage(),
                    "Database Initialization Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }

        final boolean databaseInitialized = dbReady;

        // Launch user interface on the Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> createAndShowGUI(databaseInitialized));
    }

    /**
     * Initializes and displays the primary application window.
     *
     * @param databaseInitialized true if the local database is ready
     */
    private static void createAndShowGUI(boolean databaseInitialized) {
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

        String statusText = databaseInitialized
                ? "Milestone 2: SQLite database and schema initialized successfully."
                : "Milestone 2: Database failed to initialize. Please check logs.";

        JLabel statusLabel = new JLabel(statusText);
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        statusLabel.setForeground(databaseInitialized ? new Color(34, 139, 34) : Color.RED);
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
