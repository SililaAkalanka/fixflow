package com.fixflow;

import com.fixflow.cli.InputHandler;
import com.fixflow.cli.MainMenu;
import com.fixflow.service.*;
import com.fixflow.util.DataSeeder;

/**
 * Application entry point for FixFlow – Smart Repair Management System.
 * Starts the CLI application with JSON-file-based persistence.
 */
public class Main {

    public static void main(String[] args) {
        // Ensure data directory exists and seed initial demo data if empty
        DataSeeder.seedIfEmpty(
                UserService.getUserRepository(),
                CustomerService.getCustomerRepository(),
                DeviceService.getDeviceRepository(),
                TechnicianService.getTechnicianRepository(),
                InventoryService.getInventoryRepository(),
                RepairService.getRepairRepository(),
                InventoryService.getPartUsageRepository(),
                PaymentService.getPaymentRepository()
        );

        // Initialize terminal input handler and launch main menu loop
        InputHandler inputHandler = new InputHandler();
        MainMenu mainMenu = new MainMenu(inputHandler);
        mainMenu.start();
    }
}
