package com.fixflow.repository;

import com.fixflow.model.Technician;
import com.fixflow.util.JsonFileHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing Technician persistence in JSON format.
 * Implements Repository<Technician> using an ArrayList.
 */
public class TechnicianRepository implements Repository<Technician> {

    private static final String DEFAULT_FILE_PATH = "data/technicians.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String filePath;
    private List<Technician> technicians;

    public TechnicianRepository() {
        this(DEFAULT_FILE_PATH);
    }

    public TechnicianRepository(String filePath) {
        this.filePath = filePath;
        this.technicians = new ArrayList<>();
        load();
    }

    @Override
    public void add(Technician tech) {
        if (tech.getTechnicianId() <= 0) {
            tech.setTechnicianId(generateNextId());
        }
        if (tech.getCreatedAt() == null || tech.getCreatedAt().isEmpty()) {
            tech.setCreatedAt(LocalDateTime.now().format(DATE_FORMATTER));
        }
        technicians.add(tech);
        save();
    }

    @Override
    public Technician findById(int id) {
        for (Technician t : technicians) {
            if (t.getTechnicianId() == id) {
                return t;
            }
        }
        return null;
    }

    @Override
    public List<Technician> findAll() {
        return new ArrayList<>(technicians);
    }

    @Override
    public void update(Technician tech) {
        for (int i = 0; i < technicians.size(); i++) {
            if (technicians.get(i).getTechnicianId() == tech.getTechnicianId()) {
                technicians.set(i, tech);
                save();
                return;
            }
        }
    }

    @Override
    public boolean delete(int id) {
        for (int i = 0; i < technicians.size(); i++) {
            if (technicians.get(i).getTechnicianId() == id) {
                technicians.remove(i);
                save();
                return true;
            }
        }
        return false;
    }

    @Override
    public void save() {
        JsonFileHandler.saveList(filePath, technicians);
    }

    @Override
    public void load() {
        technicians = JsonFileHandler.loadList(filePath, Technician.class);
    }

    public List<Technician> findActiveTechnicians() {
        List<Technician> activeList = new ArrayList<>();
        for (Technician t : technicians) {
            if (t.isActive()) {
                activeList.add(t);
            }
        }
        return activeList;
    }

    public List<Technician> search(String query) {
        List<Technician> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }
        String lowerQuery = query.trim().toLowerCase();
        for (Technician t : technicians) {
            boolean matchesName = t.getName() != null && t.getName().toLowerCase().contains(lowerQuery);
            boolean matchesPhone = t.getPhone() != null && t.getPhone().toLowerCase().contains(lowerQuery);
            boolean matchesSpec = t.getSpecialization() != null && t.getSpecialization().toLowerCase().contains(lowerQuery);
            if (matchesName || matchesPhone || matchesSpec) {
                results.add(t);
            }
        }
        return results;
    }

    private int generateNextId() {
        int maxId = 0;
        for (Technician t : technicians) {
            if (t.getTechnicianId() > maxId) {
                maxId = t.getTechnicianId();
            }
        }
        return maxId + 1;
    }
}
