package com.fixflow.cli;

import com.fixflow.model.Customer;
import com.fixflow.model.Device;
import com.fixflow.service.CustomerService;
import com.fixflow.service.DeviceService;

import java.time.LocalDate;
import java.util.List;

/**
 * CLI menu for Device registration and warranty tracking operations.
 */
public class DeviceMenu {

    private final InputHandler input;

    public DeviceMenu(InputHandler input) {
        this.input = input;
    }

    public void display() {
        boolean back = false;
        while (!back) {
            System.out.println("\n========================================");
            System.out.println("           DEVICE MANAGEMENT            ");
            System.out.println("========================================");
            System.out.println("1. Register Device for Customer");
            System.out.println("2. List All Devices");
            System.out.println("3. Search Devices");
            System.out.println("4. View Device & Warranty Details");
            System.out.println("5. Update Device Information");
            System.out.println("6. Delete Device");
            System.out.println("0. Back to Main Menu");
            System.out.println("========================================");

            int choice = input.readInt("Enter choice: ", 0, 6);
            switch (choice) {
                case 1 -> registerDevice();
                case 2 -> listDevices();
                case 3 -> searchDevices();
                case 4 -> viewDeviceDetails();
                case 5 -> updateDevice();
                case 6 -> deleteDevice();
                case 0 -> back = true;
            }
        }
    }

    private void registerDevice() {
        System.out.println("\n--- Register Device ---");
        int customerId = input.readInt("Enter Customer ID: ");
        Customer customer = CustomerService.getCustomerById(customerId);
        if (customer == null) {
            System.out.println("[!] Customer #" + customerId + " not found. Please register customer first.");
            input.pressEnterToContinue();
            return;
        }

        System.out.println("Owner: " + customer.getFullName() + " (" + customer.getPhone() + ")");
        System.out.println("Categories: 1. Smartphone  2. Laptop  3. Tablet  4. Audio  5. Other");
        int catChoice = input.readInt("Select category (1-5): ", 1, 5);
        String category = switch (catChoice) {
            case 1 -> "Smartphone";
            case 2 -> "Laptop";
            case 3 -> "Tablet";
            case 4 -> "Audio";
            default -> "Other";
        };

        String brand = input.readString("Enter Brand (e.g. Apple, Samsung, Dell): ");
        String model = input.readString("Enter Model (e.g. iPhone 13, XPS 15): ");
        String serial = input.readOptionalString("Enter Serial Number / IMEI (Optional): ");
        String colour = input.readOptionalString("Enter Device Colour (Optional): ");
        String accessories = input.readOptionalString("Enter Included Accessories (e.g. Charger, Case): ");

        boolean hasWarranty = input.readBoolean("Does this device have a warranty?");
        int warrantyMonths = 0;
        String startDate = "";
        String expiryDate = "";

        if (hasWarranty) {
            warrantyMonths = input.readInt("Enter warranty duration in months (e.g. 12, 24): ", 1, 60);
            startDate = input.readOptionalString("Enter warranty start date (YYYY-MM-DD, or press Enter for today): ");
            if (startDate.isEmpty()) {
                startDate = LocalDate.now().toString();
            }
            try {
                LocalDate start = LocalDate.parse(startDate);
                expiryDate = start.plusMonths(warrantyMonths).toString();
                System.out.println("Computed Warranty Expiry Date: " + expiryDate);
            } catch (Exception e) {
                System.out.println("Invalid date format. Skipping computed expiry.");
                expiryDate = "";
            }
        }

        try {
            Device device = new Device(0, customerId, category, brand, model, serial, colour, accessories,
                    warrantyMonths, startDate, expiryDate, null);
            int id = DeviceService.createDevice(device);
            System.out.println("\n[✓] Device registered successfully with Device ID #" + id);
        } catch (IllegalArgumentException e) {
            System.out.println("\n[!] Registration failed: " + e.getMessage());
        }
        input.pressEnterToContinue();
    }

    private void listDevices() {
        System.out.println("\n--- Registered Devices ---");
        List<Device> list = DeviceService.getAllDevices();
        if (list.isEmpty()) {
            System.out.println("No device records found.");
        } else {
            printDeviceTable(list);
        }
        input.pressEnterToContinue();
    }

    private void searchDevices() {
        System.out.println("\n--- Search Devices ---");
        String query = input.readString("Enter search keyword (brand, model, serial, or category): ");
        List<Device> results = DeviceService.searchDevices(query);
        if (results.isEmpty()) {
            System.out.println("No matching devices found for: '" + query + "'");
        } else {
            System.out.println("Found " + results.size() + " matching device(s):");
            printDeviceTable(results);
        }
        input.pressEnterToContinue();
    }

    private void viewDeviceDetails() {
        System.out.println("\n--- View Device & Warranty Details ---");
        int id = input.readInt("Enter Device ID: ");
        Device d = DeviceService.getDeviceById(id);
        if (d == null) {
            System.out.println("[!] Device #" + id + " not found.");
            input.pressEnterToContinue();
            return;
        }

        System.out.println("----------------------------------------");
        System.out.println("Device ID    : #" + d.getDeviceId());
        System.out.println("Category     : " + d.getCategory());
        System.out.println("Brand & Model: " + d.getBrand() + " " + d.getModel());
        System.out.println("Serial / IMEI: " + (d.getSerialNumber() != null && !d.getSerialNumber().isEmpty() ? d.getSerialNumber() : "-"));
        System.out.println("Colour       : " + (d.getColour() != null && !d.getColour().isEmpty() ? d.getColour() : "-"));
        System.out.println("Accessories  : " + (d.getAccessories() != null && !d.getAccessories().isEmpty() ? d.getAccessories() : "-"));
        System.out.println("Owner ID     : Customer #" + d.getCustomerId() + " (" + d.getCustomerName() + ")");
        System.out.println("Warranty     : " + (d.getWarrantyMonths() != null && d.getWarrantyMonths() > 0 ? d.getWarrantyMonths() + " months" : "None"));
        System.out.println("Warranty Span: " + (d.getWarrantyStartDate() != null ? d.getWarrantyStartDate() : "-") +
                " to " + (d.getWarrantyExpiryDate() != null ? d.getWarrantyExpiryDate() : "-"));
        System.out.println("Active Status: " + (d.isUnderWarranty() ? "YES - Currently under warranty" : "NO - Expired or none"));
        System.out.println("Registered   : " + d.getCreatedAt());
        System.out.println("----------------------------------------");
        input.pressEnterToContinue();
    }

    private void updateDevice() {
        System.out.println("\n--- Update Device Information ---");
        int id = input.readInt("Enter Device ID: ");
        Device d = DeviceService.getDeviceById(id);
        if (d == null) {
            System.out.println("[!] Device #" + id + " not found.");
            input.pressEnterToContinue();
            return;
        }

        System.out.println("Current Brand: " + d.getBrand());
        String brand = input.readOptionalString("New Brand (Enter to keep): ");
        if (!brand.isEmpty()) d.setBrand(brand);

        System.out.println("Current Model: " + d.getModel());
        String model = input.readOptionalString("New Model (Enter to keep): ");
        if (!model.isEmpty()) d.setModel(model);

        System.out.println("Current Serial: " + d.getSerialNumber());
        String serial = input.readOptionalString("New Serial Number (Enter to keep): ");
        if (!serial.isEmpty()) d.setSerialNumber(serial);

        System.out.println("Current Accessories: " + d.getAccessories());
        String acc = input.readOptionalString("New Accessories (Enter to keep): ");
        if (!acc.isEmpty()) d.setAccessories(acc);

        try {
            boolean updated = DeviceService.updateDevice(d);
            if (updated) {
                System.out.println("\n[✓] Device #" + id + " updated successfully.");
            } else {
                System.out.println("\n[!] Failed to update device.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("\n[!] Update failed: " + e.getMessage());
        }
        input.pressEnterToContinue();
    }

    private void deleteDevice() {
        System.out.println("\n--- Delete Device ---");
        int id = input.readInt("Enter Device ID to delete: ");
        Device d = DeviceService.getDeviceById(id);
        if (d == null) {
            System.out.println("[!] Device #" + id + " not found.");
            input.pressEnterToContinue();
            return;
        }

        boolean confirm = input.readBoolean("Are you sure you want to delete device '" + d.getBrand() + " " + d.getModel() + "'?");
        if (confirm) {
            try {
                boolean deleted = DeviceService.deleteDevice(id);
                if (deleted) {
                    System.out.println("\n[✓] Device #" + id + " deleted successfully.");
                } else {
                    System.out.println("\n[!] Device could not be deleted.");
                }
            } catch (IllegalStateException e) {
                System.out.println("\n[!] Cannot delete device: " + e.getMessage());
            }
        } else {
            System.out.println("Deletion cancelled.");
        }
        input.pressEnterToContinue();
    }

    private void printDeviceTable(List<Device> list) {
        System.out.printf("%-6s | %-12s | %-20s | %-16s | %-16s | %-10s%n",
                "ID", "Category", "Brand & Model", "Serial", "Owner", "Warranty");
        System.out.println("-------+--------------+----------------------+------------------+------------------+-----------");
        for (Device d : list) {
            String brandModel = d.getBrand() + " " + d.getModel();
            String owner = d.getCustomerName() != null ? d.getCustomerName() : "Cust #" + d.getCustomerId();
            String warranty = d.isUnderWarranty() ? "ACTIVE" : "EXPIRED/NO";
            System.out.printf("#%-5d | %-12s | %-20s | %-16s | %-16s | %-10s%n",
                    d.getDeviceId(),
                    truncate(d.getCategory(), 12),
                    truncate(brandModel, 20),
                    truncate(d.getSerialNumber(), 16),
                    truncate(owner, 16),
                    warranty);
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "-";
        return str.length() <= maxLen ? str : str.substring(0, maxLen - 3) + "...";
    }
}
