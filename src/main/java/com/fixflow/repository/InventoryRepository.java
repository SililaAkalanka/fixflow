package com.fixflow.repository;

import com.fixflow.model.SparePart;
import com.fixflow.util.JsonFileHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing SparePart inventory persistence in JSON format.
 * Implements Repository<SparePart> using an ArrayList.
 */
public class InventoryRepository implements Repository<SparePart> {

    private static final String DEFAULT_FILE_PATH = "data/spare_parts.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String filePath;
    private List<SparePart> parts;

    public InventoryRepository() {
        this(DEFAULT_FILE_PATH);
    }

    public InventoryRepository(String filePath) {
        this.filePath = filePath;
        this.parts = new ArrayList<>();
        load();
    }

    @Override
    public void add(SparePart part) {
        if (part.getPartId() <= 0) {
            part.setPartId(generateNextId());
        }
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        if (part.getCreatedAt() == null || part.getCreatedAt().isEmpty()) {
            part.setCreatedAt(now);
        }
        part.setUpdatedAt(now);
        parts.add(part);
        save();
    }

    @Override
    public SparePart findById(int id) {
        for (SparePart p : parts) {
            if (p.getPartId() == id) {
                return p;
            }
        }
        return null;
    }

    @Override
    public List<SparePart> findAll() {
        return new ArrayList<>(parts);
    }

    @Override
    public void update(SparePart part) {
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i).getPartId() == part.getPartId()) {
                part.setUpdatedAt(LocalDateTime.now().format(DATE_FORMATTER));
                parts.set(i, part);
                save();
                return;
            }
        }
    }

    @Override
    public boolean delete(int id) {
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i).getPartId() == id) {
                parts.remove(i);
                save();
                return true;
            }
        }
        return false;
    }

    @Override
    public void save() {
        JsonFileHandler.saveList(filePath, parts);
    }

    @Override
    public void load() {
        parts = JsonFileHandler.loadList(filePath, SparePart.class);
    }

    public List<SparePart> findLowStockParts() {
        List<SparePart> lowStock = new ArrayList<>();
        for (SparePart p : parts) {
            if (p.isLowStock()) {
                lowStock.add(p);
            }
        }
        return lowStock;
    }

    public List<SparePart> search(String query) {
        List<SparePart> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }
        String lowerQuery = query.trim().toLowerCase();
        for (SparePart p : parts) {
            boolean matchesName = p.getPartName() != null && p.getPartName().toLowerCase().contains(lowerQuery);
            boolean matchesCat = p.getCategory() != null && p.getCategory().toLowerCase().contains(lowerQuery);
            boolean matchesModel = p.getCompatibleDeviceModel() != null && p.getCompatibleDeviceModel().toLowerCase().contains(lowerQuery);
            if (matchesName || matchesCat || matchesModel) {
                results.add(p);
            }
        }
        return results;
    }

    private int generateNextId() {
        int maxId = 0;
        for (SparePart p : parts) {
            if (p.getPartId() > maxId) {
                maxId = p.getPartId();
            }
        }
        return maxId + 1;
    }
}
