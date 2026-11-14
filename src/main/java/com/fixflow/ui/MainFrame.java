package com.fixflow.ui;

import com.fixflow.enums.NavigationItem;
import com.fixflow.enums.UserRole;
import com.fixflow.model.User;
import com.fixflow.util.SessionManager;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

/**
 * Main application window providing responsive sidebar navigation, role-based screen access,
 * and live theme switching between Light and Dark themes.
 * Implements Section 6, Section 7.10, and Section 10 of the FixFlow Project Specification.
 */
public class MainFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentCards = new JPanel(cardLayout);
    private final Map<NavigationItem, BasePanel> panelMap = new EnumMap<>(NavigationItem.class);
    private final Map<NavigationItem, JButton> navButtonMap = new EnumMap<>(NavigationItem.class);

    private NavigationItem activeItem = NavigationItem.DASHBOARD;

    public MainFrame() {
        super("FixFlow – Smart Repair Workflow & Warranty Tracking");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150, 720);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);

        JPanel rootPanel = new JPanel(new BorderLayout());

        // 1. Top Header Bar
        JPanel topBar = createTopBar();
        rootPanel.add(topBar, BorderLayout.NORTH);

        // 2. Sidebar Navigation (Filtered by Active Role)
        JPanel sidebar = createSidebar();
        rootPanel.add(sidebar, BorderLayout.WEST);

        // 3. Central Content Cards
        initContentCards();
        rootPanel.add(contentCards, BorderLayout.CENTER);

        setContentPane(rootPanel);

        // Activate default dashboard
        selectNavigation(NavigationItem.DASHBOARD);
    }

    /**
     * Builds the top branding, identity, theme toggle, and logout bar.
     */
    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setPreferredSize(new Dimension(0, 52));
        bar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(215, 218, 222)),
                new EmptyBorder(0, 20, 0, 20)
        ));

        // Brand
        JLabel logo = new JLabel("FixFlow");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JLabel tagline = new JLabel("  |  Smart Repair & Warranty");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tagline.setForeground(Color.GRAY);

        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 14));
        brandPanel.setOpaque(false);
        brandPanel.add(logo);
        brandPanel.add(tagline);

        // Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 11));
        controls.setOpaque(false);

        User currentUser = SessionManager.getCurrentUser();
        UserRole currentRole = SessionManager.getCurrentRole();
        String roleName = currentRole != null ? currentRole.getDisplayName() : "Unknown";
        String userName = currentUser != null ? currentUser.getUsername() : "User";

        JLabel userBadge = new JLabel("Signed in: " + userName + " [" + roleName + "]");
        userBadge.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JButton logoutButton = new JButton("Sign Out");
        logoutButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        logoutButton.putClientProperty("JButton.buttonType", "roundRect");
        logoutButton.addActionListener(e -> handleLogout());

        controls.add(userBadge);
        controls.add(logoutButton);

        bar.add(brandPanel, BorderLayout.WEST);
        bar.add(controls, BorderLayout.EAST);

        return bar;
    }

    /**
     * Builds the sidebar containing only navigation buttons permitted for the active role.
     */
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 0, 1, new Color(220, 222, 226)),
                new EmptyBorder(15, 10, 15, 10)
        ));

        JLabel menuTitle = new JLabel("NAVIGATION");
        menuTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        menuTitle.setForeground(Color.GRAY);
        menuTitle.setBorder(new EmptyBorder(0, 10, 8, 0));
        sidebar.add(menuTitle);

        UserRole activeRole = SessionManager.getCurrentRole();

        for (NavigationItem item : NavigationItem.values()) {
            // Business rule: Enforce role-based screen restrictions (§6.1)
            if (item.isPermitted(activeRole)) {
                JButton navBtn = createNavButton(item);
                navButtonMap.put(item, navBtn);
                sidebar.add(navBtn);
                sidebar.add(Box.createRigidArea(new Dimension(0, 4)));
            }
        }

        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private JButton createNavButton(NavigationItem item) {
        JButton btn = new JButton(item.getDisplayName());
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(190, 36));
        btn.setPreferredSize(new Dimension(190, 36));
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(0, 12, 0, 12));
        btn.putClientProperty("JButton.buttonType", "roundRect");

        btn.addActionListener(e -> selectNavigation(item));
        return btn;
    }

    /**
     * Initializes all panels permitted for the active role and adds them to CardLayout.
     */
    private void initContentCards() {
        UserRole activeRole = SessionManager.getCurrentRole();

        for (NavigationItem item : NavigationItem.values()) {
            if (item.isPermitted(activeRole)) {
                BasePanel panel = instantiatePanel(item);
                panelMap.put(item, panel);
                contentCards.add(panel, item.name());
            }
        }
    }

    private BasePanel instantiatePanel(NavigationItem item) {
        return switch (item) {
            case DASHBOARD -> new DashboardPanel();
            case CUSTOMERS -> new CustomerPanel();
            case DEVICES -> new DevicePanel();
            case REPAIRS -> new RepairPanel();
            case TECHNICIANS -> new TechnicianPanel();
            case INVENTORY -> new InventoryPanel();
            case PAYMENTS -> new PaymentPanel();
            case REPORTS -> new ReportsPanel();
            case SETTINGS -> new SettingsPanel();
        };
    }

    /**
     * Switches the active card and invokes onPanelShown() for data refresh.
     *
     * @param item destination navigation item
     */
    public void selectNavigation(NavigationItem item) {
        UserRole activeRole = SessionManager.getCurrentRole();
        if (!item.isPermitted(activeRole)) {
            JOptionPane.showMessageDialog(this,
                    "Access Denied: Your active role (" + (activeRole != null ? activeRole.getDisplayName() : "Unknown") +
                            ") is not authorized to access " + item.getDisplayName() + ".",
                    "Unauthorized Action",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        this.activeItem = item;
        cardLayout.show(contentCards, item.name());

        // Update button visual states
        for (Map.Entry<NavigationItem, JButton> entry : navButtonMap.entrySet()) {
            boolean isSelected = entry.getKey() == item;
            JButton btn = entry.getValue();
            btn.setFont(new Font("Segoe UI", isSelected ? Font.BOLD : Font.PLAIN, 13));
        }

        // Trigger polymorphic lifecycle hook
        BasePanel panel = panelMap.get(item);
        if (panel != null) {
            panel.onPanelShown();
        }
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to sign out?",
                "Confirm Sign Out",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            SessionManager.logout();
            dispose();
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            });
        }
    }

    public NavigationItem getActiveItem() {
        return activeItem;
    }
}

