package com.fixflow.repository;

import com.fixflow.enums.RepairStatus;
import com.fixflow.model.RepairJob;
import com.fixflow.util.JsonFileHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing RepairJob persistence in JSON format.
 * Implements Repository<RepairJob> using an ArrayList.
 */
public class RepairRepository implements Repository<RepairJob> {

    private static final String DEFAULT_FILE_PATH = "data/repair_jobs.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String filePath;
    private List<RepairJob> repairJobs;

    public RepairRepository() {
        this(DEFAULT_FILE_PATH);
    }

    public RepairRepository(String filePath) {
        this.filePath = filePath;
        this.repairJobs = new ArrayList<>();
        load();
    }

    @Override
    public void add(RepairJob job) {
        if (job.getJobId() <= 0) {
            job.setJobId(generateNextId());
        }
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        if (job.getDateReceived() == null || job.getDateReceived().isEmpty()) {
            job.setDateReceived(now.substring(0, 10)); // YYYY-MM-DD
        }
        if (job.getCreatedAt() == null || job.getCreatedAt().isEmpty()) {
            job.setCreatedAt(now);
        }
        job.setUpdatedAt(now);
        repairJobs.add(job);
        save();
    }

    @Override
    public RepairJob findById(int id) {
        for (RepairJob j : repairJobs) {
            if (j.getJobId() == id) {
                return j;
            }
        }
        return null;
    }

    @Override
    public List<RepairJob> findAll() {
        return new ArrayList<>(repairJobs);
    }

    @Override
    public void update(RepairJob job) {
        for (int i = 0; i < repairJobs.size(); i++) {
            if (repairJobs.get(i).getJobId() == job.getJobId()) {
                job.setUpdatedAt(LocalDateTime.now().format(DATE_FORMATTER));
                repairJobs.set(i, job);
                save();
                return;
            }
        }
    }

    @Override
    public boolean delete(int id) {
        for (int i = 0; i < repairJobs.size(); i++) {
            if (repairJobs.get(i).getJobId() == id) {
                repairJobs.remove(i);
                save();
                return true;
            }
        }
        return false;
    }

    @Override
    public void save() {
        JsonFileHandler.saveList(filePath, repairJobs);
    }

    @Override
    public void load() {
        repairJobs = JsonFileHandler.loadList(filePath, RepairJob.class);
    }

    public List<RepairJob> findByCustomerId(int customerId) {
        List<RepairJob> results = new ArrayList<>();
        for (RepairJob j : repairJobs) {
            if (j.getCustomerId() == customerId) {
                results.add(j);
            }
        }
        return results;
    }

    public List<RepairJob> findByDeviceId(int deviceId) {
        List<RepairJob> results = new ArrayList<>();
        for (RepairJob j : repairJobs) {
            if (j.getDeviceId() == deviceId) {
                results.add(j);
            }
        }
        return results;
    }

    public List<RepairJob> findByTechnicianId(int technicianId) {
        List<RepairJob> results = new ArrayList<>();
        for (RepairJob j : repairJobs) {
            if (j.getTechnicianId() != null && j.getTechnicianId() == technicianId) {
                results.add(j);
            }
        }
        return results;
    }

    public List<RepairJob> search(String query, RepairStatus statusFilter, Integer technicianIdFilter) {
        List<RepairJob> results = new ArrayList<>();
        String lowerQuery = query != null ? query.trim().toLowerCase() : "";

        for (RepairJob j : repairJobs) {
            // Filter by status if specified
            if (statusFilter != null && j.getCurrentStatus() != statusFilter) {
                continue;
            }
            // Filter by technician if specified
            if (technicianIdFilter != null) {
                if (j.getTechnicianId() == null || j.getTechnicianId().intValue() != technicianIdFilter.intValue()) {
                    continue;
                }
            }
            // Filter by search query if specified
            if (!lowerQuery.isEmpty()) {
                boolean matchesProblem = j.getInitialProblem() != null && j.getInitialProblem().toLowerCase().contains(lowerQuery);
                boolean matchesFindings = j.getDiagnosisFindings() != null && j.getDiagnosisFindings().toLowerCase().contains(lowerQuery);
                boolean matchesNotes = j.getNotes() != null && j.getNotes().toLowerCase().contains(lowerQuery);
                boolean matchesJobId = String.valueOf(j.getJobId()).equals(lowerQuery);
                if (!matchesProblem && !matchesFindings && !matchesNotes && !matchesJobId) {
                    continue;
                }
            }
            results.add(j);
        }
        return results;
    }

    private int generateNextId() {
        int maxId = 0;
        for (RepairJob j : repairJobs) {
            if (j.getJobId() > maxId) {
                maxId = j.getJobId();
            }
        }
        return maxId + 1;
    }
}
