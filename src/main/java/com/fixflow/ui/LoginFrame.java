package com.fixflow.ui;

import com.fixflow.database.UserDB;
import com.fixflow.model.User;
import com.fixflow.util.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Login interface for role-based authentication.
 * Implements Section 6.1 of the FixFlow Project Specification.
 */
public class LoginFrame extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(LoginFrame.class.getName());

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JLabel errorLabel = new JLabel(" ");

    public LoginFrame() {
        super("FixFlow – Login");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(460, 480);
        setMinimumSize(new Dimension(400, 440));
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(35, 40, 35, 40));

        // Header
        JLabel titleLabel = new JLabel("FixFlow");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Smart Repair Workflow & Warranty");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(Color.GRAY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel loginHeader = new JLabel("Sign In");
        loginHeader.setFont(new Font("Segoe UI", Font.BOLD, 18));
        loginHeader.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Form fields
        JPanel formPanel = new JPanel(new GridLayout(4, 1, 4, 8));
        formPanel.setMaximumSize(new Dimension(360, 190));

        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        formPanel.add(userLabel);
        formPanel.add(usernameField);
        formPanel.add(passLabel);
        formPanel.add(passwordField);

        // Error message label
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        errorLabel.setForeground(new Color(210, 40, 40));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Sign In Button
        JButton loginButton = new JButton("Sign In");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.putClientProperty("JButton.buttonType", "roundRect");
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(360, 40));
        loginButton.addActionListener(e -> handleLogin());

        // Enter key listener
        KeyAdapter enterKeyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleLogin();
                }
            }
        };
        usernameField.addKeyListener(enterKeyListener);
        passwordField.addKeyListener(enterKeyListener);

        // Role guide hint
        JLabel hintLabel = new JLabel("Supports shared login for all 5 roles");
        hintLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hintLabel.setForeground(Color.LIGHT_GRAY);
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Layout Assembly
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(loginHeader);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(formPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        mainPanel.add(errorLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        mainPanel.add(loginButton);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(hintLabel);

        setContentPane(mainPanel);
        getRootPane().setDefaultButton(loginButton);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }

        try {
            User user = UserDB.authenticate(username, password);
            if (user != null) {
                errorLabel.setText(" ");
                SessionManager.login(user);
                LOGGER.info("Successful login for role: " + user.getRole() + " (username: " + user.getUsername() + ")");

                dispose();
                SwingUtilities.invokeLater(() -> {
                    DashboardFrame dashboard = new DashboardFrame();
                    dashboard.setVisible(true);
                });
            } else {
                showError("Invalid username or password. Please try again.");
                passwordField.setText("");
                passwordField.requestFocusInWindow();
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Database error during login authentication.", ex);
            showError("Database connection error. Please try again.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }
}

