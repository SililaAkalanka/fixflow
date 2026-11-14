package com.fixflow;

import com.fixflow.database.DatabaseInitializer;
import com.fixflow.database.UserDB;
import com.fixflow.ui.LoginFrame;
import com.fixflow.ui.SetupFrame;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
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
        try {
            DatabaseInitializer.initializeDatabase();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize SQLite database.", e);
            JOptionPane.showMessageDialog(
                    null,
                    "FixFlow was unable to initialize its local database.\nPlease verify file permissions and restart the application.\n\nError: " + e.getMessage(),
                    "Database Initialization Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Determine whether first-launch setup or normal login is required
        boolean setupNeeded = true;
        try {
            setupNeeded = UserDB.isSetupRequired();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking user setup status.", e);
        }

        final boolean isFirstLaunch = setupNeeded;

        // Launch user interface on the Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            if (isFirstLaunch) {
                LOGGER.info("Initial setup required. Launching SetupFrame wizard.");
                SetupFrame setupFrame = new SetupFrame();
                setupFrame.setVisible(true);
            } else {
                LOGGER.info("Setup verified. Launching LoginFrame.");
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            }
        });
    }
}
