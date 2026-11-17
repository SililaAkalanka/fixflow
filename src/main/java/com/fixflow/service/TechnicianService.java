package com.fixflow.service;

import com.fixflow.model.RepairJob;
import com.fixflow.model.Technician;
import com.fixflow.repository.RepairRepository;
import com.fixflow.repository.TechnicianRepository;
import com.fixflow.util.ValidationUtil;

import java.util.List;

/**
 * Service class for technician management and staff active-status tracking.
 */
public class TechnicianService {

    private static TechnicianRepository technicianRepo = new TechnicianRepository();
    private static RepairRepository repairRepo = new RepairRepository();

    private TechnicianService() {
    }

    public static void setRepositories(TechnicianRepository tr, RepairRepository rr) {
        technicianRepo = tr;
        repairRepo = rr;
    }

    public static TechnicianRepository getTechnicianRepository() {
        return technicianRepo;
    }

    public static int addTechnician(Technician tech) {
        if (tech == null) {
            throw new IllegalArgumentException("Technician cannot be null.");
        }
        if (!ValidationUtil.isNotEmpty(tech.getName())) {
            throw new IllegalArgumentException("Technician name is required.");
        }
        if (!ValidationUtil.isValidPhone(tech.getPhone())) {
            throw new IllegalArgumentException("Invalid phone number format: " + tech.getPhone());
        }

        technicianRepo.add(tech);
        return tech.getTechnicianId();
    }

    public static Technician getTechnicianById(int id) {
        return technicianRepo.findById(id);
    }

    public static List<Technician> getAllTechnicians() {
        return technicianRepo.findAll();
    }

    public static List<Technician> getActiveTechnicians() {
        return technicianRepo.findActiveTechnicians();
    }

    public static List<Technician> searchTechnicians(String query) {
        return technicianRepo.search(query);
    }

    public static boolean updateTechnician(Technician tech) {
        if (tech == null || tech.getTechnicianId() <= 0) {
            throw new IllegalArgumentException("Valid technician ID is required.");
        }
        Technician existing = technicianRepo.findById(tech.getTechnicianId());
        if (existing == null) {
            return false;
        }

        existing.setName(tech.getName());
        existing.setPhone(tech.getPhone());
        existing.setSpecialization(tech.getSpecialization());
        existing.setActive(tech.isActive());

        technicianRepo.update(existing);
        return true;
    }

    public static boolean toggleActiveStatus(int technicianId) {
        Technician existing = technicianRepo.findById(technicianId);
        if (existing == null) {
            return false;
        }
        existing.setActive(!existing.isActive());
        technicianRepo.update(existing);
        return true;
    }

    public static boolean deleteTechnician(int id) {
        Technician existing = technicianRepo.findById(id);
        if (existing == null) {
            return false;
        }

        // Check if technician has assigned repair jobs that are not completed/cancelled
        List<RepairJob> jobs = repairRepo.findByTechnicianId(id);
        for (RepairJob j : jobs) {
            if (!j.getCurrentStatus().isTerminal()) {
                throw new IllegalStateException("Cannot delete technician #" + id + ": Technician has active repair job #" + j.getJobId());
            }
        }

        return technicianRepo.delete(id);
    }
}
