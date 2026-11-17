package com.fixflow.cli;

import com.fixflow.model.Technician;
import com.fixflow.service.TechnicianService;

import java.util.List;

/**
 * CLI menu for Technician management and assignment tracking.
 */
public class TechnicianMenu {

    private final InputHandler input;

    public TechnicianMenu(InputHandler input) {
        this.input = input;
    }

    public void display() {
        boolean back = false;
        while (!back) {
            System.out.println("\n========================================");
            System.out.println("         TECHNICIAN MANAGEMENT          ");
            System.out.println("========================================");
            System.out.println("1. Add New Technician");
            System.out.println("2. List All Technicians");
            System.out.println("3. Search Technicians");
            System.out.println("4. Toggle Active / Inactive Status");
            System.out.println("5. Update Technician Details");
            System.out.println("6. Delete Technician");
            System.out.println("0. Back to Main Menu");
            System.out.println("========================================");

            int choice = input.readInt("Enter choice: ", 0, 6);
            switch (choice) {
                case 1 -> addTechnician();
                case 2 -> listTechnicians();
                case 3 -> searchTechnicians();
                case 4 -> toggleActiveStatus();
                case 5 -> updateTechnician();
                case 6 -> deleteTechnician();
                case 0 -> back = true;
            }
        }
    }

    private void addTechnician() {
        System.out.println("\n--- Add New Technician ---");
        String name = input.readString("Enter Technician Name: ");
        String phone = input.readString("Enter Phone Number: ");
        String spec = input.readString("Enter Specialization (e.g. Smartphone, Laptop, Audio): ");

        try {
            Technician tech = new Technician(name, phone, spec, true);
            int id = TechnicianService.addTechnician(tech);
            System.out.println("\n[✓] Technician registered successfully with Technician ID #" + id);
        } catch (IllegalArgumentException e) {
            System.out.println("\n[!] Registration failed: " + e.getMessage());
        }
        input.pressEnterToContinue();
    }

    private void listTechnicians() {
        System.out.println("\n--- Technicians Directory ---");
        List<Technician> list = TechnicianService.getAllTechnicians();
        if (list.isEmpty()) {
            System.out.println("No technician records found.");
        } else {
            printTechnicianTable(list);
        }
        input.pressEnterToContinue();
    }

    private void searchTechnicians() {
        System.out.println("\n--- Search Technicians ---");
        String query = input.readString("Enter search keyword (name, phone, or specialization): ");
        List<Technician> results = TechnicianService.searchTechnicians(query);
        if (results.isEmpty()) {
            System.out.println("No matching technicians found for: '" + query + "'");
        } else {
            System.out.println("Found " + results.size() + " matching technician(s):");
            printTechnicianTable(results);
        }
        input.pressEnterToContinue();
    }

    private void toggleActiveStatus() {
        System.out.println("\n--- Toggle Technician Active Status ---");
        int id = input.readInt("Enter Technician ID: ");
        Technician tech = TechnicianService.getTechnicianById(id);
        if (tech == null) {
            System.out.println("[!] Technician #" + id + " not found.");
            input.pressEnterToContinue();
            return;
        }

        System.out.println("Current status for " + tech.getName() + ": " + (tech.isActive() ? "ACTIVE" : "INACTIVE"));
        boolean confirm = input.readBoolean("Toggle status to " + (tech.isActive() ? "INACTIVE" : "ACTIVE") + "?");
        if (confirm) {
            TechnicianService.toggleActiveStatus(id);
            Technician updated = TechnicianService.getTechnicianById(id);
            System.out.println("[✓] Status updated to: " + (updated.isActive() ? "ACTIVE" : "INACTIVE"));
        }
        input.pressEnterToContinue();
    }

    private void updateTechnician() {
        System.out.println("\n--- Update Technician Details ---");
        int id = input.readInt("Enter Technician ID: ");
        Technician tech = TechnicianService.getTechnicianById(id);
        if (tech == null) {
            System.out.println("[!] Technician #" + id + " not found.");
            input.pressEnterToContinue();
            return;
        }

        System.out.println("Current Name: " + tech.getName());
        String name = input.readOptionalString("New Name (Enter to keep): ");
        if (!name.isEmpty()) tech.setName(name);

        System.out.println("Current Phone: " + tech.getPhone());
        String phone = input.readOptionalString("New Phone (Enter to keep): ");
        if (!phone.isEmpty()) tech.setPhone(phone);

        System.out.println("Current Specialization: " + tech.getSpecialization());
        String spec = input.readOptionalString("New Specialization (Enter to keep): ");
        if (!spec.isEmpty()) tech.setSpecialization(spec);

        try {
            boolean updated = TechnicianService.updateTechnician(tech);
            if (updated) {
                System.out.println("\n[✓] Technician #" + id + " updated successfully.");
            } else {
                System.out.println("\n[!] Failed to update technician.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("\n[!] Update failed: " + e.getMessage());
        }
        input.pressEnterToContinue();
    }

    private void deleteTechnician() {
        System.out.println("\n--- Delete Technician ---");
        int id = input.readInt("Enter Technician ID to delete: ");
        Technician tech = TechnicianService.getTechnicianById(id);
        if (tech == null) {
            System.out.println("[!] Technician #" + id + " not found.");
            input.pressEnterToContinue();
            return;
        }

        boolean confirm = input.readBoolean("Are you sure you want to delete technician '" + tech.getName() + "'?");
        if (confirm) {
            try {
                boolean deleted = TechnicianService.deleteTechnician(id);
                if (deleted) {
                    System.out.println("\n[✓] Technician #" + id + " deleted successfully.");
                } else {
                    System.out.println("\n[!] Technician could not be deleted.");
                }
            } catch (IllegalStateException e) {
                System.out.println("\n[!] Cannot delete technician: " + e.getMessage());
            }
        } else {
            System.out.println("Deletion cancelled.");
        }
        input.pressEnterToContinue();
    }

    private void printTechnicianTable(List<Technician> list) {
        System.out.printf("%-6s | %-22s | %-14s | %-32s | %-10s%n",
                "ID", "Name", "Phone", "Specialization", "Status");
        System.out.println("-------+------------------------+----------------+----------------------------------+-----------");
        for (Technician t : list) {
            String status = t.isActive() ? "ACTIVE" : "INACTIVE";
            System.out.printf("#%-5d | %-22s | %-14s | %-32s | %-10s%n",
                    t.getTechnicianId(),
                    truncate(t.getName(), 22),
                    t.getPhone(),
                    truncate(t.getSpecialization(), 32),
                    status);
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "-";
        return str.length() <= maxLen ? str : str.substring(0, maxLen - 3) + "...";
    }
}
