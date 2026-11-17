package com.fixflow.service;

import com.fixflow.enums.RepairStatus;
import com.fixflow.exception.InvalidStatusTransitionException;
import com.fixflow.model.Customer;
import com.fixflow.model.Device;
import com.fixflow.model.RepairJob;
import com.fixflow.model.Technician;
import com.fixflow.repository.CustomerRepository;
import com.fixflow.repository.DeviceRepository;
import com.fixflow.repository.RepairRepository;
import com.fixflow.repository.TechnicianRepository;
import com.fixflow.util.ValidationUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service class for repair job business logic, lifecycle transitions, and collection validation.
 * Demonstrates business rule enforcement and custom checked exceptions.
 */
public class RepairService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static RepairRepository repairRepo = new RepairRepository();
    private static CustomerRepository customerRepo = new CustomerRepository();
    private static DeviceRepository deviceRepo = new DeviceRepository();
    private static TechnicianRepository technicianRepo = new TechnicianRepository();

    private RepairService() {
    }

    public static void setRepositories(RepairRepository rr, CustomerRepository cr, DeviceRepository dr, TechnicianRepository tr) {
        repairRepo = rr;
        customerRepo = cr;
        deviceRepo = dr;
        technicianRepo = tr;
    }

    public static RepairRepository getRepairRepository() {
        return repairRepo;
    }

    /**
     * Creates a new repair job.
     */
    public static int createRepairJob(RepairJob job) {
        if (job == null) {
            throw new IllegalArgumentException("Repair job cannot be null.");
        }
        if (!ValidationUtil.isNotEmpty(job.getInitialProblem())) {
            throw new IllegalArgumentException("Initial problem description is required.");
        }

        Customer customer = customerRepo.findById(job.getCustomerId());
        if (customer == null) {
            throw new IllegalArgumentException("Customer #" + job.getCustomerId() + " does not exist.");
        }

        Device device = deviceRepo.findById(job.getDeviceId());
        if (device == null) {
            throw new IllegalArgumentException("Device #" + job.getDeviceId() + " does not exist.");
        }

        if (device.getCustomerId() != job.getCustomerId()) {
            throw new IllegalArgumentException("Device #" + job.getDeviceId() + " does not belong to Customer #" + job.getCustomerId());
        }

        repairRepo.add(job);
        return job.getJobId();
    }

    /**
     * Calculates the total cost for a repair job.
     */
    public static double calculateTotalCost(int jobId) {
        return PaymentService.calculateTotalCost(jobId);
    }

    /**
     * Advances or updates the repair job status with strict lifecycle and collection checks.
     * Lifecycle: RECEIVED -> DIAGNOSING -> IN_PROGRESS -> COMPLETED -> COLLECTED
     *                                                  \-> CANCELLED
     *
     * @param jobId repair job ID
     * @param targetStatus target status
     * @param notes notes or cancellation reason
     * @throws InvalidStatusTransitionException if transition is invalid
     * @throws IllegalStateException if collecting an unpaid device
     * @throws IllegalArgumentException if cancellation reason is missing or job not found
     */
    public static void updateStatus(int jobId, RepairStatus targetStatus, String notes)
            throws InvalidStatusTransitionException {

        RepairJob job = repairRepo.findById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Repair Job #" + jobId + " not found.");
        }

        // Validate lifecycle transition rules in model
        job.validateTransition(targetStatus);

        // Guard: A device cannot be marked COLLECTED if there is an outstanding balance
        if (targetStatus == RepairStatus.COLLECTED) {
            if (!PaymentService.isJobFullyPaid(jobId)) {
                double balance = PaymentService.getRemainingBalance(jobId);
                throw new IllegalStateException(String.format(
                        "Cannot collect device: Repair Job #%d has an outstanding balance of LKR %.2f.",
                        jobId, balance));
            }
        }

        // Guard: Cancellation requires a reason
        if (targetStatus == RepairStatus.CANCELLED) {
            if (notes == null || notes.trim().isEmpty()) {
                throw new IllegalArgumentException("A cancellation reason must be provided when cancelling a repair job.");
            }
        }

        job.setCurrentStatus(targetStatus);
        if (notes != null && !notes.trim().isEmpty()) {
            job.setNotes(notes.trim());
        }

        if (targetStatus == RepairStatus.COMPLETED && (job.getActualCompletionDate() == null || job.getActualCompletionDate().isEmpty())) {
            job.setActualCompletionDate(LocalDate.now().toString());
        }

        repairRepo.update(job);
    }

    /**
     * Assigns a technician to a repair job after validating technician existence and active status.
     *
     * @param jobId repair job ID
     * @param technicianId technician ID
     * @return true if updated
     * @throws IllegalArgumentException if technician does not exist or is inactive
     */
    public static boolean assignTechnician(int jobId, int technicianId) {
        RepairJob job = repairRepo.findById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Repair Job #" + jobId + " not found.");
        }

        Technician tech = technicianRepo.findById(technicianId);
        if (tech == null) {
            throw new IllegalArgumentException("Technician #" + technicianId + " not found.");
        }
        if (!tech.isActive()) {
            throw new IllegalArgumentException("Cannot assign technician '" + tech.getName() + "': Technician is marked as Inactive.");
        }

        job.setTechnicianId(technicianId);
        repairRepo.update(job);
        return true;
    }

    /**
     * Records technician diagnosis and estimates.
     */
    public static void recordDiagnosis(int jobId, String findings, Double estimatedCost, double labourCharges,
                                       String estCompletionDate) {
        RepairJob job = repairRepo.findById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Repair Job #" + jobId + " not found.");
        }

        job.setDiagnosisFindings(findings);
        if (estimatedCost != null && estimatedCost >= 0) {
            job.setEstimatedCost(estimatedCost);
        }
        if (labourCharges >= 0) {
            job.setLabourCharges(labourCharges);
        }
        if (estCompletionDate != null && !estCompletionDate.trim().isEmpty()) {
            job.setEstimatedCompletionDate(estCompletionDate.trim());
        }

        repairRepo.update(job);
    }

    /**
     * Retrieves a repair job by ID with transient details populated.
     */
    public static RepairJob getRepairJobById(int jobId) {
        RepairJob job = repairRepo.findById(jobId);
        if (job != null) {
            populateTransientJobDetails(job);
        }
        return job;
    }

    /**
     * Retrieves all repair jobs.
     */
    public static List<RepairJob> getAllRepairJobs() {
        List<RepairJob> list = repairRepo.findAll();
        for (RepairJob j : list) {
            populateTransientJobDetails(j);
        }
        return list;
    }

    /**
     * Retrieves repair jobs for a technician.
     */
    public static List<RepairJob> getRepairJobsForTechnician(int technicianId) {
        List<RepairJob> list = repairRepo.findByTechnicianId(technicianId);
        for (RepairJob j : list) {
            populateTransientJobDetails(j);
        }
        return list;
    }

    /**
     * Searches repair jobs with filters.
     */
    public static List<RepairJob> searchRepairJobs(String query, RepairStatus statusFilter, Integer technicianIdFilter) {
        List<RepairJob> list = repairRepo.search(query, statusFilter, technicianIdFilter);
        for (RepairJob j : list) {
            populateTransientJobDetails(j);
        }
        return list;
    }

    /**
     * Deletes a repair job if safe.
     */
    public static boolean deleteRepairJob(int jobId) {
        RepairJob job = repairRepo.findById(jobId);
        if (job == null) {
            return false;
        }

        if (InventoryService.getTotalPartsCostForJob(jobId) > 0) {
            throw new IllegalStateException("Cannot delete repair job #" + jobId + ": Spare parts have been issued to this job.");
        }

        if (PaymentService.getTotalPaid(jobId) > 0) {
            throw new IllegalStateException("Cannot delete repair job #" + jobId + ": Payments have been recorded for this job.");
        }

        return repairRepo.delete(jobId);
    }

    private static void populateTransientJobDetails(RepairJob job) {
        Customer c = customerRepo.findById(job.getCustomerId());
        if (c != null) {
            job.setCustomerName(c.getFullName());
            job.setCustomerPhone(c.getPhone());
        }
        Device d = deviceRepo.findById(job.getDeviceId());
        if (d != null) {
            job.setDeviceSummary(d.getBrand() + " " + d.getModel() + " (" + d.getCategory() + ")");
        }
        if (job.getTechnicianId() != null) {
            Technician t = technicianRepo.findById(job.getTechnicianId());
            if (t != null) {
                job.setTechnicianName(t.getName());
            }
        }
    }
}
