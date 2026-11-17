package com.fixflow.service;

import com.fixflow.model.Customer;
import com.fixflow.model.Device;
import com.fixflow.model.RepairJob;
import com.fixflow.repository.CustomerRepository;
import com.fixflow.repository.DeviceRepository;
import com.fixflow.repository.RepairRepository;
import com.fixflow.util.ValidationUtil;

import java.util.List;

/**
 * Service class for device business operations and customer relationship validation.
 */
public class DeviceService {

    private static DeviceRepository deviceRepo = new DeviceRepository();
    private static CustomerRepository customerRepo = new CustomerRepository();
    private static RepairRepository repairRepo = new RepairRepository();

    private DeviceService() {
    }

    public static void setRepositories(DeviceRepository dr, CustomerRepository cr, RepairRepository rr) {
        deviceRepo = dr;
        customerRepo = cr;
        repairRepo = rr;
    }

    public static DeviceRepository getDeviceRepository() {
        return deviceRepo;
    }

    public static int createDevice(Device device) {
        if (device == null) {
            throw new IllegalArgumentException("Device cannot be null.");
        }
        if (!ValidationUtil.isNotEmpty(device.getBrand())) {
            throw new IllegalArgumentException("Device brand is required.");
        }
        if (!ValidationUtil.isNotEmpty(device.getModel())) {
            throw new IllegalArgumentException("Device model is required.");
        }

        Customer customer = customerRepo.findById(device.getCustomerId());
        if (customer == null) {
            throw new IllegalArgumentException("Referenced customer #" + device.getCustomerId() + " does not exist.");
        }

        deviceRepo.add(device);
        return device.getDeviceId();
    }

    public static Device getDeviceById(int deviceId) {
        Device device = deviceRepo.findById(deviceId);
        if (device != null) {
            populateTransientCustomerDetails(device);
        }
        return device;
    }

    public static List<Device> getAllDevices() {
        List<Device> list = deviceRepo.findAll();
        for (Device d : list) {
            populateTransientCustomerDetails(d);
        }
        return list;
    }

    public static List<Device> getDevicesByCustomerId(int customerId) {
        List<Device> list = deviceRepo.findByCustomerId(customerId);
        for (Device d : list) {
            populateTransientCustomerDetails(d);
        }
        return list;
    }

    public static List<Device> searchDevices(String query) {
        List<Device> list = deviceRepo.search(query);
        for (Device d : list) {
            populateTransientCustomerDetails(d);
        }
        return list;
    }

    public static boolean updateDevice(Device device) {
        if (device == null || device.getDeviceId() <= 0) {
            throw new IllegalArgumentException("Valid device ID is required.");
        }
        Device existing = deviceRepo.findById(device.getDeviceId());
        if (existing == null) {
            return false;
        }

        existing.setCategory(device.getCategory());
        existing.setBrand(device.getBrand());
        existing.setModel(device.getModel());
        existing.setSerialNumber(device.getSerialNumber());
        existing.setColour(device.getColour());
        existing.setAccessories(device.getAccessories());
        existing.setWarrantyMonths(device.getWarrantyMonths());
        existing.setWarrantyStartDate(device.getWarrantyStartDate());
        existing.setWarrantyExpiryDate(device.getWarrantyExpiryDate());

        deviceRepo.update(existing);
        return true;
    }

    public static boolean deleteDevice(int deviceId) {
        Device existing = deviceRepo.findById(deviceId);
        if (existing == null) {
            return false;
        }

        // Check referential integrity: reject if device has repair jobs
        List<RepairJob> jobs = repairRepo.findByDeviceId(deviceId);
        if (!jobs.isEmpty()) {
            throw new IllegalStateException("Cannot delete device #" + deviceId + ": Device is linked to " +
                    jobs.size() + " repair job(s).");
        }

        return deviceRepo.delete(deviceId);
    }

    private static void populateTransientCustomerDetails(Device device) {
        Customer c = customerRepo.findById(device.getCustomerId());
        if (c != null) {
            device.setCustomerName(c.getFullName());
            device.setCustomerPhone(c.getPhone());
        }
    }
}
