package com.fixflow.model;

import com.fixflow.enums.UserRole;

import java.util.Objects;

/**
 * Model representing a shared role user account in FixFlow.
 */
public class User {

    private int userId;
    private UserRole role;
    private String username;
    private String passwordHash;
    private String createdAt;
    private String updatedAt;

    public User() {
    }

    public User(int userId, UserRole role, String username, String passwordHash, String createdAt, String updatedAt) {
        this.userId = userId;
        this.role = role;
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public User(UserRole role, String username, String passwordHash) {
        this.role = role;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
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
        return userId == user.userId && role == user.role && Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, role, username);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", role=" + role +
                ", username='" + username + '\'' +
                '}';
    }
}

