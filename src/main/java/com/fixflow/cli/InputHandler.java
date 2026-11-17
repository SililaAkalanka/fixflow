package com.fixflow.cli;

import java.util.Scanner;

/**
 * Robust terminal input handler for the FixFlow CLI.
 * Gracefully handles invalid inputs, number format errors, and empty lines.
 */
public class InputHandler {

    private final Scanner scanner;

    public InputHandler() {
        this.scanner = new Scanner(System.in);
    }

    public InputHandler(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            if (input != null && !input.trim().isEmpty()) {
                return input.trim();
            }
            System.out.println("  [!] Input cannot be empty. Please enter a valid value.");
        }
    }

    public String readOptionalString(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine();
        return input != null ? input.trim() : "";
    }

    public int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            try {
                return Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid number format. Please enter a valid whole number.");
            }
        }
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            int value = readInt(prompt);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.println("  [!] Value must be between " + min + " and " + max + ".");
        }
    }

    public double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            try {
                return Double.parseDouble(input.trim());
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid decimal number. Please enter a valid amount (e.g. 1500.00).");
            }
        }
    }

    public double readDouble(String prompt, double min, double max) {
        while (true) {
            double value = readDouble(prompt);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.printf("  [!] Amount must be between %.2f and %.2f.%n", min, max);
        }
    }

    public boolean readBoolean(String prompt) {
        while (true) {
            System.out.print(prompt + " (y/n): ");
            String input = scanner.nextLine();
            if (input != null) {
                String trimmed = input.trim().toLowerCase();
                if (trimmed.equals("y") || trimmed.equals("yes")) {
                    return true;
                }
                if (trimmed.equals("n") || trimmed.equals("no")) {
                    return false;
                }
            }
            System.out.println("  [!] Please enter 'y' for yes or 'n' for no.");
        }
    }

    public void pressEnterToContinue() {
        System.out.print("\nPress [Enter] to continue...");
        scanner.nextLine();
    }
}
