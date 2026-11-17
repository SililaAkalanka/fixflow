package com.fixflow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fixflow.enums.UserRole;

import java.util.Objects;

/**
 * Abstract base class representing a user account in FixFlow.
 * Demonstrates the OOP principles of Abstraction, Encapsulation, and Polymorphism.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "role"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AdminUser.class, name = "ADMINISTRATOR"),
        @JsonSubTypes.Type(value = TechnicianUser.class, name = "TECHNICIAN")
})
public abstract class User {

    private int userId;
    private String username;
    private String passwordHash;
    private String createdAt;
    private String updatedAt;

    public User() {
    }

    public User(int userId, String username, String passwordHash, String createdAt, String updatedAt) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    /**
     * Abstract method returning the user's role.
     * Overridden polymorphically in concrete subclasses.
     *
     * @return UserRole of the user
     */
    public abstract UserRole getRole();

    /**
     * Abstract method checking whether this user has administrative management permissions.
     * Demonstrates Polymorphism.
     *
     * @return true if permitted, false otherwise
     */
    public abstract boolean canManageUsers();

    /**
     * Abstract method checking whether this user has technician management permissions.
     * Demonstrates Polymorphism.
     *
     * @return true if permitted, false otherwise
     */
    public abstract boolean canManageTechnicians();

    /**
     * Abstract method returning a descriptive summary of role permissions.
     * Demonstrates Polymorphism.
     *
     * @return String describing user role permissions
     */
    public abstract String getRoleDescription();

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return userId == user.userId && Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, username);
    }

    @Override
    public String toString() {
        return getRole().getDisplayName() + " [" + username + "] (User ID #" + userId + ")";
    }
}
