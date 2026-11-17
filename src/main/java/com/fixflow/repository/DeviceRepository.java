package com.fixflow.repository;

import com.fixflow.model.Device;
import com.fixflow.util.JsonFileHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing Device persistence in JSON format.
 * Implements Repository<Device> using an ArrayList.
 */
public class DeviceRepository implements Repository<Device> {

    private static final String DEFAULT_FILE_PATH = "data/devices.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String filePath;
    private List<Device> devices;

    public DeviceRepository() {
        this(DEFAULT_FILE_PATH);
    }

    public DeviceRepository(String filePath) {
        this.filePath = filePath;
        this.devices = new ArrayList<>();
        load();
    }

    @Override
    public void add(Device device) {
        if (device.getDeviceId() <= 0) {
            device.setDeviceId(generateNextId());
        }
        if (device.getCreatedAt() == null || device.getCreatedAt().isEmpty()) {
            device.setCreatedAt(LocalDateTime.now().format(DATE_FORMATTER));
        }
        devices.add(device);
        save();
    }

    @Override
    public Device findById(int id) {
        for (Device d : devices) {
            if (d.getDeviceId() == id) {
                return d;
            }
        }
        return null;
    }

    @Override
    public List<Device> findAll() {
        return new ArrayList<>(devices);
    }

    @Override
    public void update(Device device) {
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getDeviceId() == device.getDeviceId()) {
                devices.set(i, device);
                save();
                return;
            }
        }
    }

    @Override
    public boolean delete(int id) {
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getDeviceId() == id) {
                devices.remove(i);
                save();
                return true;
            }
        }
        return false;
    }

    @Override
    public void save() {
        JsonFileHandler.saveList(filePath, devices);
    }

    @Override
    public void load() {
        devices = JsonFileHandler.loadList(filePath, Device.class);
    }

    public List<Device> findByCustomerId(int customerId) {
        List<Device> results = new ArrayList<>();
        for (Device d : devices) {
            if (d.getCustomerId() == customerId) {
                results.add(d);
            }
        }
        return results;
    }

    public List<Device> search(String query) {
        List<Device> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }
        String lowerQuery = query.trim().toLowerCase();
        for (Device d : devices) {
            boolean matchesBrand = d.getBrand() != null && d.getBrand().toLowerCase().contains(lowerQuery);
            boolean matchesModel = d.getModel() != null && d.getModel().toLowerCase().contains(lowerQuery);
            boolean matchesSerial = d.getSerialNumber() != null && d.getSerialNumber().toLowerCase().contains(lowerQuery);
            boolean matchesCategory = d.getCategory() != null && d.getCategory().toLowerCase().contains(lowerQuery);
            if (matchesBrand || matchesModel || matchesSerial || matchesCategory) {
                results.add(d);
            }
        }
        return results;
    }

    private int generateNextId() {
        int maxId = 0;
        for (Device d : devices) {
            if (d.getDeviceId() > maxId) {
                maxId = d.getDeviceId();
            }
        }
        return maxId + 1;
    }
}
