package com.fixflow.util;

import com.fixflow.enums.RepairStatus;
import com.fixflow.model.*;
import com.fixflow.repository.*;

import java.util.List;
import java.util.logging.Logger;

/**
 * Utility for seeding demo data on initial launch if repositories are empty.
 * Provides a ready-to-demonstrate environment for coursework evaluation and viva.
 */
public class DataSeeder {

    private static final Logger LOGGER = Logger.getLogger(DataSeeder.class.getName());

    private DataSeeder() {
        // Utility class
    }

    /**
     * Seeds initial demo data if the user repository is empty.
     */
    public static void seedIfEmpty(
            UserRepository userRepo,
            CustomerRepository customerRepo,
            DeviceRepository deviceRepo,
            TechnicianRepository technicianRepo,
            InventoryRepository inventoryRepo,
            RepairRepository repairRepo,
            PartUsageRepository usageRepo,
            PaymentRepository paymentRepo) {

        List<User> users = userRepo.findAll();
        if (!users.isEmpty()) {
            return; // Data already exists
        }

        LOGGER.info("No user records found. Initializing FixFlow demo data...");

        // 1. Seed Users (with BCrypt hashed passwords)
        AdminUser admin = new AdminUser(1, "admin", PasswordUtil.hashPassword("admin123"), "2026-01-01 09:00:00", "2026-01-01 09:00:00");
        TechnicianUser tech = new TechnicianUser(2, "tech1", PasswordUtil.hashPassword("tech123"), "2026-01-01 09:00:00", "2026-01-01 09:00:00");
        userRepo.add(admin);
        userRepo.add(tech);

        // 2. Seed Technicians
        if (technicianRepo.findAll().isEmpty()) {
            technicianRepo.add(new Technician(1, "Kamal Perera", "0771234567", "Smartphone & Tablet Specialist", true, "2026-01-05 10:00:00"));
            technicianRepo.add(new Technician(2, "Nimal Silva", "0719876543", "Laptop & Motherboard Specialist", true, "2026-01-05 10:15:00"));
            technicianRepo.add(new Technician(3, "Sunil Fernando", "0755554433", "Audio & Small Electronics", false, "2026-01-05 10:30:00"));
        }

        // 3. Seed Customers
        if (customerRepo.findAll().isEmpty()) {
            customerRepo.add(new Customer(1, "Kasun Jayawardena", "0772223344", "kasun@example.com", "45 Galle Road, Colombo 03", "2026-01-10 11:00:00"));
            customerRepo.add(new Customer(2, "Dilani Wickramasinghe", "0783334455", "dilani@example.com", "12 Kandy Road, Kiribathgoda", "2026-01-12 14:30:00"));
            customerRepo.add(new Customer(3, "Nuwan Pradeep", "0704445566", "nuwan@example.com", "88 High Level Road, Nugegoda", "2026-01-15 16:45:00"));
        }

        // 4. Seed Devices
        if (deviceRepo.findAll().isEmpty()) {
            deviceRepo.add(new Device(1, 1, "Smartphone", "Samsung", "Galaxy S22", "SM-G901-7788", "Phantom Black", "Charger, Protective Case", 12, "2026-01-10", "2027-01-10", "2026-01-10 11:15:00"));
            deviceRepo.add(new Device(2, 2, "Laptop", "Dell", "XPS 15", "DL-XP-9944", "Silver", "Power Adapter, Sleeve", 24, "2024-05-01", "2026-05-01", "2026-01-12 14:45:00"));
            deviceRepo.add(new Device(3, 3, "Smartphone", "Apple", "iPhone 13", "AP-IP13-1122", "Midnight", "Original Box", 0, "", "", "2026-01-15 17:00:00"));
        }

        // 5. Seed Spare Parts
        if (inventoryRepo.findAll().isEmpty()) {
            inventoryRepo.add(new SparePart(1, "Samsung S22 OLED Screen Assembly", "Display", "Galaxy S22", 15000.0, 22500.0, 5, 2, "2026-01-08 09:00:00", "2026-01-08 09:00:00"));
            inventoryRepo.add(new SparePart(2, "Dell XPS 15 86Wh Battery", "Battery", "Dell XPS 15", 12000.0, 18000.0, 3, 2, "2026-01-08 09:15:00", "2026-01-08 09:15:00"));
            inventoryRepo.add(new SparePart(3, "iPhone 13 Charging Port Flex", "Charging", "iPhone 13", 3500.0, 6500.0, 8, 3, "2026-01-08 09:30:00", "2026-01-08 09:30:00"));
            inventoryRepo.add(new SparePart(4, "Thermal Paste Arctic MX-4 (4g)", "Consumable", "Universal", 1200.0, 2000.0, 1, 3, "2026-01-08 09:45:00", "2026-01-08 09:45:00"));
        }

        // 6. Seed Repair Jobs
        if (repairRepo.findAll().isEmpty()) {
            RepairJob job1 = new RepairJob(1, 1, "Cracked screen and touch unresponsive");
            job1.setJobId(1);
            job1.setTechnicianId(1);
            job1.setDateReceived("2026-11-10");
            job1.setCurrentStatus(RepairStatus.IN_PROGRESS);
            job1.setDiagnosisFindings("OLED digitizer damaged; frame intact");
            job1.setEstimatedCost(26500.0);
            job1.setLabourCharges(4000.0);
            job1.setEstimatedCompletionDate("2026-11-20");
            job1.setNotes("Customer approved original screen replacement");
            job1.setCreatedAt("2026-11-10 10:00:00");
            job1.setUpdatedAt("2026-11-10 11:30:00");
            repairRepo.add(job1);

            RepairJob job2 = new RepairJob(2, 2, "Battery draining in 30 minutes and laptop overheating");
            job2.setJobId(2);
            job2.setTechnicianId(2);
            job2.setDateReceived("2026-11-05");
            job2.setCurrentStatus(RepairStatus.COMPLETED);
            job2.setDiagnosisFindings("Battery degraded to 42% capacity. Fan clogged with dust.");
            job2.setEstimatedCost(23000.0);
            job2.setLabourCharges(5000.0);
            job2.setEstimatedCompletionDate("2026-11-12");
            job2.setActualCompletionDate("2026-11-12");
            job2.setNotes("Ready for pickup. Tested battery cycle.");
            job2.setCreatedAt("2026-11-05 09:30:00");
            job2.setUpdatedAt("2026-11-12 15:00:00");
            repairRepo.add(job2);

            RepairJob job3 = new RepairJob(3, 3, "Not charging with standard lightning cable");
            job3.setJobId(3);
            job3.setDateReceived("2026-11-16");
            job3.setCurrentStatus(RepairStatus.RECEIVED);
            job3.setLabourCharges(0.0);
            job3.setNotes("Inspect lightning port contacts.");
            job3.setCreatedAt("2026-11-16 14:00:00");
            job3.setUpdatedAt("2026-11-16 14:00:00");
            repairRepo.add(job3);
        }

        // 7. Seed Part Usage
        if (usageRepo.findAll().isEmpty()) {
            usageRepo.add(new RepairPartUsage(1, 1, 1, 1, 22500.0, "2026-11-11 14:30:00"));
            usageRepo.add(new RepairPartUsage(2, 2, 2, 1, 18000.0, "2026-11-08 10:15:00"));
        }

        // 8. Seed Payments
        if (paymentRepo.findAll().isEmpty()) {
            paymentRepo.add(new Payment(1, 1, "2026-11-10 11:00:00", 10000.0, "CASH", "Advance deposit at intake", "2026-11-10 11:00:00"));
            paymentRepo.add(new Payment(2, 2, "2026-11-06 09:30:00", 10000.0, "CASH", "Advance deposit", "2026-11-06 09:30:00"));
            paymentRepo.add(new Payment(3, 2, "2026-11-12 16:00:00", 13000.0, "CASH", "Full settlement upon completion", "2026-11-12 16:00:00"));
        }

        LOGGER.info("FixFlow demo data successfully seeded.");
    }
}
