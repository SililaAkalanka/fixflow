package com.fixflow.enums;

import java.util.Set;

/**
 * Navigation items representing primary application screens with associated RBAC permissions.
 * Implements Section 6 and Section 10.5 of the FixFlow Project Specification.
 */
public enum NavigationItem {

    DASHBOARD("Dashboard", "System overview and key operational metrics",
            Set.of(UserRole.ADMINISTRATOR, UserRole.MANAGER, UserRole.RECEPTIONIST, UserRole.TECHNICIAN, UserRole.INVENTORY_STAFF)),

    CUSTOMERS("Customers", "Customer directory, contact details, and history",
            Set.of(UserRole.ADMINISTRATOR, UserRole.MANAGER, UserRole.RECEPTIONIST, UserRole.TECHNICIAN)),

    DEVICES("Devices", "Electronic device registry and category tracking",
            Set.of(UserRole.ADMINISTRATOR, UserRole.MANAGER, UserRole.RECEPTIONIST, UserRole.TECHNICIAN)),

    REPAIRS("Repair Jobs", "End-to-end repair tracking and status lifecycle",
            Set.of(UserRole.ADMINISTRATOR, UserRole.MANAGER, UserRole.RECEPTIONIST, UserRole.TECHNICIAN)),

    TECHNICIANS("Technicians", "Staff management, assignments, and active status",
            Set.of(UserRole.ADMINISTRATOR, UserRole.MANAGER, UserRole.RECEPTIONIST)),

    INVENTORY("Inventory", "Spare parts, stock tracking, and reorder levels",
            Set.of(UserRole.ADMINISTRATOR, UserRole.INVENTORY_STAFF, UserRole.MANAGER, UserRole.TECHNICIAN)),

    PAYMENTS("Payments & Invoices", "Cash payment recording and PDF invoice generation",
            Set.of(UserRole.ADMINISTRATOR, UserRole.MANAGER, UserRole.RECEPTIONIST)),

    WARRANTIES("Warranties", "30-day warranty tracking and claim processing",
            Set.of(UserRole.ADMINISTRATOR, UserRole.MANAGER, UserRole.RECEPTIONIST)),

    REPORTS("Reports", "Operational summaries, revenue, and inventory analysis",
            Set.of(UserRole.ADMINISTRATOR, UserRole.MANAGER)),

    SETTINGS("Settings", "Shared role credentials and backup management",
            Set.of(UserRole.ADMINISTRATOR));

    private final String displayName;
    private final String description;
    private final Set<UserRole> permittedRoles;

    NavigationItem(String displayName, String description, Set<UserRole> permittedRoles) {
        this.displayName = displayName;
        this.description = description;
        this.permittedRoles = permittedRoles;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if a specified role is authorized to view and access this screen.
     *
     * @param role user role to check
     * @return true if permitted, false otherwise
     */
    public boolean isPermitted(UserRole role) {
        return role != null && permittedRoles.contains(role);
    }

    /**
     * Checks if a specified role has view-only access to this module.
     *
     * @param role user role to check
     * @return true if access is restricted to read-only viewing
     */
    public boolean isReadOnly(UserRole role) {
        if (role == null) return true;
        return switch (this) {
            case CUSTOMERS, DEVICES -> role == UserRole.TECHNICIAN;
            case TECHNICIANS -> role == UserRole.RECEPTIONIST;
            case INVENTORY -> role == UserRole.MANAGER || role == UserRole.TECHNICIAN;
            default -> false;
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}

