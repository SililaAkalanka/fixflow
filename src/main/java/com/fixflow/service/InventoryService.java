package com.fixflow.service;

import com.fixflow.exception.InsufficientStockException;
import com.fixflow.model.RepairPartUsage;
import com.fixflow.model.SparePart;
import com.fixflow.repository.InventoryRepository;
import com.fixflow.repository.PartUsageRepository;
import com.fixflow.util.ValidationUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service class for inventory management, stock deductions, and spare part usage.
 * Implements InsufficientStockException enforcement.
 */
public class InventoryService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static InventoryRepository inventoryRepo = new InventoryRepository();
    private static PartUsageRepository usageRepo = new PartUsageRepository();

    private InventoryService() {
    }

    public static void setRepositories(InventoryRepository ir, PartUsageRepository ur) {
        inventoryRepo = ir;
        usageRepo = ur;
    }

    public static InventoryRepository getInventoryRepository() {
        return inventoryRepo;
    }

    public static PartUsageRepository getPartUsageRepository() {
        return usageRepo;
    }

    public static int addPart(SparePart part) {
        if (part == null) {
            throw new IllegalArgumentException("Spare part cannot be null.");
        }
        if (!ValidationUtil.isNotEmpty(part.getPartName())) {
            throw new IllegalArgumentException("Spare part name is required.");
        }
        if (part.getPurchaseCost() < 0) {
            throw new IllegalArgumentException("Purchase cost cannot be negative.");
        }
        if (part.getSellingPrice() < 0) {
            throw new IllegalArgumentException("Selling price cannot be negative.");
        }
        if (part.getAvailableQuantity() < 0) {
            throw new IllegalArgumentException("Available quantity cannot be negative.");
        }

        inventoryRepo.add(part);
        return part.getPartId();
    }

    public static SparePart getPartById(int partId) {
        return inventoryRepo.findById(partId);
    }

    public static List<SparePart> getAllParts() {
        return inventoryRepo.findAll();
    }

    public static List<SparePart> searchParts(String query) {
        return inventoryRepo.search(query);
    }

    public static List<SparePart> getLowStockParts() {
        return inventoryRepo.findLowStockParts();
    }

    public static boolean updatePart(SparePart part) {
        if (part == null || part.getPartId() <= 0) {
            throw new IllegalArgumentException("Valid part ID is required.");
        }
        SparePart existing = inventoryRepo.findById(part.getPartId());
        if (existing == null) {
            return false;
        }

        existing.setPartName(part.getPartName());
        existing.setCategory(part.getCategory());
        existing.setCompatibleDeviceModel(part.getCompatibleDeviceModel());
        existing.setPurchaseCost(part.getPurchaseCost());
        existing.setSellingPrice(part.getSellingPrice());
        existing.setMinStockLevel(part.getMinStockLevel());

        inventoryRepo.update(existing);
        return true;
    }

    public static boolean addStock(int partId, int quantityToAdd) {
        if (quantityToAdd <= 0) {
            throw new IllegalArgumentException("Stock quantity addition must be greater than zero.");
        }
        SparePart part = inventoryRepo.findById(partId);
        if (part == null) {
            throw new IllegalArgumentException("Spare part #" + partId + " not found.");
        }

        part.setAvailableQuantity(part.getAvailableQuantity() + quantityToAdd);
        inventoryRepo.update(part);
        return true;
    }

    /**
     * Issues spare parts to a repair job and deducts inventory stock.
     * Enforces the business rule that stock cannot become negative.
     * Demonstrates custom checked exception handling (InsufficientStockException).
     *
     * @param jobId repair job ID
     * @param partId spare part ID
     * @param quantity quantity to issue
     * @param issuedByRole user role issuing the part
     * @throws InsufficientStockException if available quantity is less than requested quantity
     */
    public static void issuePartToJob(int jobId, int partId, int quantity, String issuedByRole)
            throws InsufficientStockException {

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity issued must be at least 1.");
        }

        SparePart part = inventoryRepo.findById(partId);
        if (part == null) {
            throw new IllegalArgumentException("Spare part #" + partId + " not found.");
        }

        // Check if sufficient stock is available
        if (part.getAvailableQuantity() < quantity) {
            throw new InsufficientStockException(partId, part.getPartName(), part.getAvailableQuantity(), quantity);
        }

        // Deduct stock
        part.setAvailableQuantity(part.getAvailableQuantity() - quantity);
        inventoryRepo.update(part);

        // Record usage with price snapshot
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        RepairPartUsage usage = new RepairPartUsage(jobId, partId, quantity, part.getSellingPrice(), now);
        usageRepo.add(usage);
    }

    public static List<RepairPartUsage> getPartsUsedForJob(int jobId) {
        List<RepairPartUsage> usages = usageRepo.findByJobId(jobId);
        for (RepairPartUsage u : usages) {
            SparePart p = inventoryRepo.findById(u.getPartId());
            if (p != null) {
                u.setPartName(p.getPartName());
                u.setPartCategory(p.getCategory());
                u.setCompatibleDeviceModel(p.getCompatibleDeviceModel());
            }
        }
        return usages;
    }

    public static double getTotalPartsCostForJob(int jobId) {
        return usageRepo.getTotalCostForJob(jobId);
    }

    public static boolean deletePart(int partId) {
        SparePart existing = inventoryRepo.findById(partId);
        if (existing == null) {
            return false;
        }

        // Verify if part has usage history
        List<RepairPartUsage> allUsages = usageRepo.findAll();
        for (RepairPartUsage u : allUsages) {
            if (u.getPartId() == partId) {
                throw new IllegalStateException("Cannot delete part #" + partId +
                        ": Part has historical usage in repair jobs.");
            }
        }

        return inventoryRepo.delete(partId);
    }
}
