package com.fixflow.service;

import com.fixflow.enums.RepairStatus;
import com.fixflow.model.Customer;
import com.fixflow.model.DashboardMetrics;
import com.fixflow.model.Payment;
import com.fixflow.model.RepairJob;
import com.fixflow.model.SparePart;
import com.fixflow.model.Technician;
import com.fixflow.repository.CustomerRepository;
import com.fixflow.repository.DeviceRepository;
import com.fixflow.repository.InventoryRepository;
import com.fixflow.repository.PaymentRepository;
import com.fixflow.repository.RepairRepository;
import com.fixflow.repository.TechnicianRepository;

import java.util.List;

/**
 * Service class for calculating business analytics, revenue reports, and operational summaries.
 * Uses straightforward loops and counters suitable for first-year OOP coursework.
 */
public class ReportService {

    private static CustomerRepository customerRepo = new CustomerRepository();
    private static DeviceRepository deviceRepo = new DeviceRepository();
    private static TechnicianRepository technicianRepo = new TechnicianRepository();
    private static InventoryRepository inventoryRepo = new InventoryRepository();
    private static RepairRepository repairRepo = new RepairRepository();
    private static PaymentRepository paymentRepo = new PaymentRepository();

    private ReportService() {
    }

    public static void setRepositories(CustomerRepository cr, DeviceRepository dr, TechnicianRepository tr,
                                      InventoryRepository ir, RepairRepository rr, PaymentRepository pr) {
        customerRepo = cr;
        deviceRepo = dr;
        technicianRepo = tr;
        inventoryRepo = ir;
        repairRepo = rr;
        paymentRepo = pr;
    }

    public static DashboardMetrics getDashboardMetrics() {
        DashboardMetrics metrics = new DashboardMetrics();

        List<Customer> customers = customerRepo.findAll();
        metrics.setTotalCustomers(customers.size());

        List<Technician> technicians = technicianRepo.findAll();
        metrics.setTotalTechnicians(technicians.size());

        List<RepairJob> jobs = repairRepo.findAll();
        metrics.setTotalJobs(jobs.size());

        int active = 0;
        int completed = 0;
        int collected = 0;
        int cancelled = 0;

        for (RepairJob j : jobs) {
            RepairStatus s = j.getCurrentStatus();
            metrics.setStatusCount(s, metrics.getStatusCount(s) + 1);

            if (s == RepairStatus.RECEIVED || s == RepairStatus.DIAGNOSING || s == RepairStatus.IN_PROGRESS) {
                active++;
            } else if (s == RepairStatus.COMPLETED) {
                completed++;
            } else if (s == RepairStatus.COLLECTED) {
                collected++;
            } else if (s == RepairStatus.CANCELLED) {
                cancelled++;
            }
        }

        metrics.setActiveJobs(active);
        metrics.setCompletedJobs(completed);
        metrics.setCollectedJobs(collected);
        metrics.setCancelledJobs(cancelled);

        // Revenue calculations
        double totalRev = 0.0;
        List<Payment> payments = paymentRepo.findAll();
        for (Payment p : payments) {
            totalRev += p.getAmountPaid();
        }
        metrics.setTotalRevenue(totalRev);

        // Pending revenue (outstanding balance on non-cancelled jobs)
        double pending = 0.0;
        for (RepairJob j : jobs) {
            if (j.getCurrentStatus() != RepairStatus.CANCELLED) {
                pending += PaymentService.getRemainingBalance(j.getJobId());
            }
        }
        metrics.setPendingRevenue(pending);

        // Inventory valuation and stock alerts
        int lowStock = 0;
        int outStock = 0;
        double inventoryVal = 0.0;
        List<SparePart> parts = inventoryRepo.findAll();
        for (SparePart p : parts) {
            if (p.isOutOfStock()) {
                outStock++;
            } else if (p.isLowStock()) {
                lowStock++;
            }
            inventoryVal += p.getPurchaseCost() * p.getAvailableQuantity();
        }

        metrics.setLowStockCount(lowStock);
        metrics.setOutOfStockCount(outStock);
        metrics.setTotalInventoryValuation(inventoryVal);

        return metrics;
    }
}
