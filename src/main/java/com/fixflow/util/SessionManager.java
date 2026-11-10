package com.fixflow.util;

import com.fixflow.enums.UserRole;
import com.fixflow.model.User;

/**
 * Manages the active authenticated session for the desktop application.
 * In accordance with FixFlow's shared login model, one role is active per session.
 */
public class SessionManager {

    private static User currentUser;

    private SessionManager() {
        // Utility class
    }

    /**
     * Sets the active authenticated user for the current session.
     *
     * @param user authenticated user
     */
    public static synchronized void login(User user) {
        currentUser = user;
    }

    /**
     * Clears the current session and logs out.
     */
    public static synchronized void logout() {
        currentUser = null;
    }

    /**
     * Obtains the currently logged-in user.
     *
     * @return current {@link User}, or null if unauthenticated
     */
    public static synchronized User getCurrentUser() {
        return currentUser;
    }

    /**
     * Obtains the active user role.
     *
     * @return active {@link UserRole}, or null if unauthenticated
     */
    public static synchronized UserRole getCurrentRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }

    /**
     * Obtains the ID of the currently logged-in user.
     *
     * @return active user ID, or null if unauthenticated
     */
    public static synchronized Integer getCurrentUserId() {
        return currentUser != null ? currentUser.getUserId() : null;
    }

    /**
     * Checks if a user is currently logged in.
     *
     * @return true if logged in, false otherwise
     */
    public static synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Checks if the active user possesses a specific role.
     *
     * @param role role to verify
     * @return true if active role matches, false otherwise
     */
    public static synchronized boolean hasRole(UserRole role) {
        return currentUser != null && currentUser.getRole() == role;
    }

    /**
     * Checks if the active user is an Administrator.
     *
     * @return true if active user is Administrator
     */
    public static synchronized boolean isAdmin() {
        return hasRole(UserRole.ADMINISTRATOR);
    }
}

