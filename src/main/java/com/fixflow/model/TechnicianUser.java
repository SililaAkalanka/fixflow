package com.fixflow.model;

import com.fixflow.enums.UserRole;

/**
 * Concrete user subclass representing a Service Technician in FixFlow.
 * Demonstrates Inheritance and Polymorphism.
 */
public class TechnicianUser extends User {

    public TechnicianUser() {
        super();
    }

    public TechnicianUser(int userId, String username, String passwordHash, String createdAt, String updatedAt) {
        super(userId, username, passwordHash, createdAt, updatedAt);
    }

    public TechnicianUser(String username, String passwordHash) {
        super(username, passwordHash);
    }

    @Override
    public UserRole getRole() {
        return UserRole.TECHNICIAN;
    }

    @Override
    public boolean canManageUsers() {
        return false;
    }

    @Override
    public boolean canManageTechnicians() {
        return false;
    }

    @Override
    public String getRoleDescription() {
        return "Service Technician - Operational access to repair diagnosing, parts usage, and customer devices.";
    }
}
