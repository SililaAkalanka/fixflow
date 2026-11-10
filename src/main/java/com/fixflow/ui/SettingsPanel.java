package com.fixflow.ui;

import com.fixflow.database.DatabaseManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Administrator settings panel.
 * Password resets for shared roles and manual backup procedures will be fully enabled in Milestone 9.
 */
public class SettingsPanel extends BasePanel {

    public SettingsPanel() {
        super("System Settings", "Configure shared role passwords, view database storage, and review backup guidance");
        initUI();
    }

    private void initUI() {
        contentArea.setLayout(new BoxLayout(contentArea, BoxLayout.Y_AXIS));

        // Database Location Info Card
        JPanel dbInfoCard = new JPanel(new GridLayout(2, 1, 0, 4));
        dbInfoCard.setBorder(new CompoundBorder(
                new TitledBorder("Local SQLite Database"),
                new EmptyBorder(8, 12, 10, 12)
        ));
        dbInfoCard.setMaximumSize(new Dimension(800, 80));

        JLabel dbUrlLabel = new JLabel("Database JDBC URL: " + DatabaseManager.getJdbcUrl());
        dbUrlLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel backupNote = new JLabel("Manual Backup Notice: Close the FixFlow application completely before copying the .db file.");
        backupNote.setFont(new Font("Segoe UI", Font.BOLD, 12));
        backupNote.setForeground(new Color(177, 24, 1));

        dbInfoCard.add(dbUrlLabel);
        dbInfoCard.add(backupNote);

        // Placeholder for Password Management
        JPanel placeholder = new JPanel(new GridBagLayout());
        JLabel info = new JLabel("Shared role password modification and database backup tools will be enabled in Milestone 9.");
        info.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        info.setForeground(Color.GRAY);
        placeholder.add(info);

        contentArea.add(dbInfoCard);
        contentArea.add(Box.createRigidArea(new Dimension(0, 15)));
        contentArea.add(placeholder);
    }
}

