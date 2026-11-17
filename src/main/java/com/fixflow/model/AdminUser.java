package com.fixflow.model;

import com.fixflow.enums.UserRole;

/**
 * Concrete user subclass representing an Administrator in FixFlow.
 * Demonstrates Inheritance and Polymorphism.
 */
public class AdminUser extends User {

    public AdminUser() {
        super();
    }

    public AdminUser(int userId, String username, String passwordHash, String createdAt, String updatedAt) {
        super(userId, username, passwordHash, createdAt, updatedAt);
    }

    public AdminUser(String username, String passwordHash) {
        super(username, passwordHash);
    }

    @Override
    public UserRole getRole() {
        return UserRole.ADMINISTRATOR;
    }

    @Override
    public boolean canManageUsers() {
        return true;
    }

    @Override
    public boolean canManageTechnicians() {
        return true;
    }

    @Override
    public String getRoleDescription() {
        return "System Administrator - Full management access across all modules, reports, and staff.";
    }
}
