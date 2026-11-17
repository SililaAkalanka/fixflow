package com.fixflow.cli;

import com.fixflow.model.User;
import com.fixflow.service.UserService;
import com.fixflow.util.SessionManager;

/**
 * Main application navigation menu and authentication coordinator for FixFlow CLI.
 * Demonstrates role-based access control and polymorphic user permission descriptions.
 */
public class MainMenu {

    private final InputHandler input;
    private final CustomerMenu customerMenu;
    private final DeviceMenu deviceMenu;
    private final TechnicianMenu technicianMenu;
    private final RepairMenu repairMenu;
    private final InventoryMenu inventoryMenu;
    private final PaymentMenu paymentMenu;
    private final ReportMenu reportMenu;

    public MainMenu(InputHandler input) {
        this.input = input;
        this.customerMenu = new CustomerMenu(input);
        this.deviceMenu = new DeviceMenu(input);
        this.technicianMenu = new TechnicianMenu(input);
        this.repairMenu = new RepairMenu(input);
        this.inventoryMenu = new InventoryMenu(input);
        this.paymentMenu = new PaymentMenu(input);
        this.reportMenu = new ReportMenu(input);
    }

    public void start() {
        boolean running = true;
        while (running) {
            if (!SessionManager.isLoggedIn()) {
                boolean loggedIn = loginPrompt();
                if (!loggedIn) {
                    System.out.println("\nExiting FixFlow. Goodbye!");
                    return;
                }
            }

            User user = SessionManager.getCurrentUser();
            System.out.println("\n============================================================");
            System.out.println("          FIXFLOW - SMART REPAIR MANAGEMENT SYSTEM          ");
            System.out.println("============================================================");
            System.out.printf("  Logged in as : %s (%s)%n", user.getUsername(), user.getRole().getDisplayName());
            // Polymorphic method call to demonstrate OOP Polymorphism:
            System.out.printf("  Privileges   : %s%n", user.getRoleDescription());
            System.out.println("------------------------------------------------------------");
            System.out.println("  1. Customer Management");
            System.out.println("  2. Device Management");
            System.out.println("  3. Repair Management");
            System.out.println("  4. Technician Management" + (user.canManageTechnicians() ? "" : " [Admin Only]"));
            System.out.println("  5. Inventory Management");
            System.out.println("  6. Payment Management");
            System.out.println("  7. Reports & Analytics");
            System.out.println("  8. Change My Password");
            System.out.println("  9. Logout");
            System.out.println("  0. Exit FixFlow");
            System.out.println("============================================================");

            int choice = input.readInt("Enter your choice (0-9): ", 0, 9);
            switch (choice) {
                case 1 -> customerMenu.display();
                case 2 -> deviceMenu.display();
                case 3 -> repairMenu.display();
                case 4 -> {
                    // Polymorphic check to demonstrate OOP Polymorphism & Abstraction
                    if (user.canManageTechnicians()) {
                        technicianMenu.display();
                    } else {
                        System.out.println("\n[!] ACCESS DENIED: Only Administrators can access Technician Management.");
                        input.pressEnterToContinue();
                    }
                }
                case 5 -> inventoryMenu.display();
                case 6 -> paymentMenu.display();
                case 7 -> reportMenu.display();
                case 8 -> changePasswordPrompt();
                case 9 -> {
                    System.out.println("\nLogging out from session...");
                    SessionManager.logout();
                }
                case 0 -> {
                    System.out.println("\nThank you for using FixFlow. Goodbye!");
                    running = false;
                }
            }
        }
    }

    private boolean loginPrompt() {
        System.out.println("\n========================================");
        System.out.println("                FIXFLOW                 ");
        System.out.println("        Smart Repair Management         ");
        System.out.println("========================================");
        System.out.println("Demo Accounts Available:");
        System.out.println("  - Administrator: admin / admin123");
        System.out.println("  - Technician   : tech1 / tech123");
        System.out.println("----------------------------------------");

        int attempts = 0;
        while (attempts < 3) {
            String username = input.readString("Enter Username (or 'exit' to quit): ");
            if (username.equalsIgnoreCase("exit")) {
                return false;
            }

            String password = input.readString("Enter Password: ");

            User user = UserService.authenticate(username, password);
            if (user != null) {
                SessionManager.login(user);
                System.out.printf("%n[✓] Welcome back, %s! Authentication successful as %s.%n",
                        user.getUsername(), user.getRole().getDisplayName());
                return true;
            } else {
                attempts++;
                System.out.printf("  [!] Invalid username or password. (Attempt %d of 3)%n", attempts);
            }
        }

        System.out.println("\n[!] Maximum login attempts reached. Returning to login prompt.");
        return true;
    }

    private void changePasswordPrompt() {
        System.out.println("\n--- Change Password ---");
        User user = SessionManager.getCurrentUser();
        String currentPassword = input.readString("Enter Current Password: ");

        User verified = UserService.authenticate(user.getUsername(), currentPassword);
        if (verified == null) {
            System.out.println("[!] Current password incorrect. Password was not changed.");
            input.pressEnterToContinue();
            return;
        }

        String newPassword = input.readString("Enter New Password: ");
        String confirmPassword = input.readString("Confirm New Password: ");

        if (!newPassword.equals(confirmPassword)) {
            System.out.println("[!] New passwords do not match. Password was not changed.");
            input.pressEnterToContinue();
            return;
        }

        boolean updated = UserService.changePassword(user.getUserId(), newPassword);
        if (updated) {
            System.out.println("[✓] Password updated successfully.");
        } else {
            System.out.println("[!] Failed to update password.");
        }
        input.pressEnterToContinue();
    }
}
