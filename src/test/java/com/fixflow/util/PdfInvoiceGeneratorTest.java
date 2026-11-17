package com.fixflow.util;

import com.fixflow.enums.RepairStatus;
import com.fixflow.model.Customer;
import com.fixflow.model.Device;
import com.fixflow.model.RepairJob;
import com.fixflow.model.SparePart;
import com.fixflow.repository.*;
import com.fixflow.service.CustomerService;
import com.fixflow.service.DeviceService;
import com.fixflow.service.InventoryService;
import com.fixflow.service.PaymentService;
import com.fixflow.service.RepairService;
import com.fixflow.service.TechnicianService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;

import static org.junit.jupiter.api.Assertions.*;

public class PdfInvoiceGeneratorTest {

    private File tempDir;
    private File tempPdfFile;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = File.createTempFile("fixflow_pdftest_", "_dir");
        tempDir.delete();
        tempDir.mkdirs();

        CustomerRepository cr = new CustomerRepository(new File(tempDir, "customers.json").getAbsolutePath());
        DeviceRepository dr = new DeviceRepository(new File(tempDir, "devices.json").getAbsolutePath());
        TechnicianRepository tr = new TechnicianRepository(new File(tempDir, "technicians.json").getAbsolutePath());
        InventoryRepository ir = new InventoryRepository(new File(tempDir, "spare_parts.json").getAbsolutePath());
        PartUsageRepository ur = new PartUsageRepository(new File(tempDir, "part_usage.json").getAbsolutePath());
        RepairRepository rr = new RepairRepository(new File(tempDir, "repair_jobs.json").getAbsolutePath());
        PaymentRepository pr = new PaymentRepository(new File(tempDir, "payments.json").getAbsolutePath());

        CustomerService.setRepositories(cr, dr, rr);
        DeviceService.setRepositories(dr, cr, rr);
        TechnicianService.setRepositories(tr, rr);
        InventoryService.setRepositories(ir, ur);
        RepairService.setRepositories(rr, cr, dr, tr);
        PaymentService.setRepositories(pr, rr, cr, dr);

        tempPdfFile = File.createTempFile("fixflow_test_invoice_", ".pdf");
    }

    @AfterEach
    void tearDown() {
        if (tempDir != null && tempDir.exists()) {
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
            tempDir.delete();
        }
        if (tempPdfFile != null && tempPdfFile.exists()) {
            tempPdfFile.delete();
        }
    }

    @Test
    void testGenerateValidPdfInvoice() throws Exception {
        // 1. Create customer and device
        Customer c = new Customer("Arthur Dent", "0771239876", "arthur@hitchhiker.org", "Cottington, UK");
        int custId = CustomerService.createCustomer(c);
        Device d = new Device(custId, "Laptop", "Apple", "MacBook Pro M1", "SN-C02-9988", "Space Gray", "MagSafe Charger");
        int devId = DeviceService.createDevice(d);

        // 2. Create and complete repair job
        RepairJob job = new RepairJob(custId, devId, "Liquid spill on keyboard and power rail short");
        job.setLabourCharges(7500.0);
        int jobId = RepairService.createRepairJob(job);
        RepairService.recordDiagnosis(jobId, "Ultrasonic cleaning & keyboard top-case replaced", 16000.0, 7500.0, "2026-11-20");

        // 3. Issue spare part: MacBook Keyboard @ LKR 8500.00
        SparePart part = new SparePart("MacBook Pro M1 Topcase", "Keyboards", "MacBook Pro M1", 4500.0, 8500.0, 5, 1);
        int partId = InventoryService.addPart(part);
        InventoryService.issuePartToJob(jobId, partId, 1, "Lead Tech");

        RepairService.updateStatus(jobId, RepairStatus.DIAGNOSING, "Diagnosing");
        RepairService.updateStatus(jobId, RepairStatus.IN_PROGRESS, "Repairing");
        RepairService.updateStatus(jobId, RepairStatus.COMPLETED, "Repaired and tested");

        // Total should be 7500 (labour) + 8500 (parts) = 16000.00
        int paymentId = PaymentService.recordPayment(jobId, 16000.0, "2026-11-10 11:30:00", "Full payment");
        assertTrue(paymentId > 0);

        // 4. Generate PDF Invoice
        PdfInvoiceGenerator.generateInvoice(paymentId, tempPdfFile);

        // 5. Verification
        assertTrue(tempPdfFile.exists(), "PDF invoice file must exist");
        assertTrue(tempPdfFile.length() > 1000, "PDF file must have substantive content (Length: " + tempPdfFile.length() + " bytes)");

        // Check magic bytes "%PDF"
        byte[] header = new byte[4];
        try (FileInputStream in = new FileInputStream(tempPdfFile)) {
            int read = in.read(header);
            assertEquals(4, read);
            assertEquals("%PDF", new String(header), "File header must be valid PDF magic numbers");
        }
    }
}
