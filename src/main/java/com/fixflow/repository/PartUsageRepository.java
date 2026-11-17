package com.fixflow.repository;

import com.fixflow.model.RepairPartUsage;
import com.fixflow.util.JsonFileHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing RepairPartUsage persistence in JSON format.
 * Implements Repository<RepairPartUsage> using an ArrayList.
 */
public class PartUsageRepository implements Repository<RepairPartUsage> {

    private static final String DEFAULT_FILE_PATH = "data/part_usage.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String filePath;
    private List<RepairPartUsage> usages;

    public PartUsageRepository() {
        this(DEFAULT_FILE_PATH);
    }

    public PartUsageRepository(String filePath) {
        this.filePath = filePath;
        this.usages = new ArrayList<>();
        load();
    }

    @Override
    public void add(RepairPartUsage usage) {
        if (usage.getUsageId() <= 0) {
            usage.setUsageId(generateNextId());
        }
        if (usage.getDateUsed() == null || usage.getDateUsed().isEmpty()) {
            usage.setDateUsed(LocalDateTime.now().format(DATE_FORMATTER));
        }
        usages.add(usage);
        save();
    }

    @Override
    public RepairPartUsage findById(int id) {
        for (RepairPartUsage u : usages) {
            if (u.getUsageId() == id) {
                return u;
            }
        }
        return null;
    }

    @Override
    public List<RepairPartUsage> findAll() {
        return new ArrayList<>(usages);
    }

    @Override
    public void update(RepairPartUsage usage) {
        for (int i = 0; i < usages.size(); i++) {
            if (usages.get(i).getUsageId() == usage.getUsageId()) {
                usages.set(i, usage);
                save();
                return;
            }
        }
    }

    @Override
    public boolean delete(int id) {
        for (int i = 0; i < usages.size(); i++) {
            if (usages.get(i).getUsageId() == id) {
                usages.remove(i);
                save();
                return true;
            }
        }
        return false;
    }

    @Override
    public void save() {
        JsonFileHandler.saveList(filePath, usages);
    }

    @Override
    public void load() {
        usages = JsonFileHandler.loadList(filePath, RepairPartUsage.class);
    }

    public List<RepairPartUsage> findByJobId(int jobId) {
        List<RepairPartUsage> results = new ArrayList<>();
        for (RepairPartUsage u : usages) {
            if (u.getJobId() == jobId) {
                results.add(u);
            }
        }
        return results;
    }

    public double getTotalCostForJob(int jobId) {
        double total = 0.0;
        for (RepairPartUsage u : usages) {
            if (u.getJobId() == jobId) {
                total += u.getTotalCost();
            }
        }
        return total;
    }

    private int generateNextId() {
        int maxId = 0;
        for (RepairPartUsage u : usages) {
            if (u.getUsageId() > maxId) {
                maxId = u.getUsageId();
            }
        }
        return maxId + 1;
    }
}
