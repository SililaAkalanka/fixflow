package com.fixflow.model;

import java.util.Objects;

/**
 * Model representing a repair technician in FixFlow.
 * Implements Section 7.4 of the FixFlow Project Specification.
 */
public class Technician {

    private int technicianId;
    private String name;
    private String phone;
    private String specialization;
    private boolean active;
    private String createdAt;

    public Technician() {
        this.active = true;
    }

    public Technician(String name, String phone, String specialization, boolean active) {
        this.name = name;
        this.phone = phone;
        this.specialization = specialization;
        this.active = active;
    }

    public Technician(int technicianId, String name, String phone, String specialization, boolean active, String createdAt) {
        this.technicianId = technicianId;
        this.name = name;
        this.phone = phone;
        this.specialization = specialization;
        this.active = active;
        this.createdAt = createdAt;
    }

    public int getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(int technicianId) {
        this.technicianId = technicianId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Technician that = (Technician) o;
        return technicianId == that.technicianId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(technicianId);
    }

    @Override
    public String toString() {
        return name + " (" + (specialization != null ? specialization : "General") + ")" + (active ? "" : " [Inactive]");
    }
}

