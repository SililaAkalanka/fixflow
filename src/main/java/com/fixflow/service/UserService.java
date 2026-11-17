package com.fixflow.service;

import com.fixflow.model.User;
import com.fixflow.repository.UserRepository;
import com.fixflow.util.PasswordUtil;

import java.util.List;

/**
 * Service class for user authentication, role verification, and credential updates.
 */
public class UserService {

    private static UserRepository userRepo = new UserRepository();

    private UserService() {
    }

    public static void setRepository(UserRepository ur) {
        userRepo = ur;
    }

    public static UserRepository getUserRepository() {
        return userRepo;
    }

    /**
     * Authenticates a user against stored hashed credentials.
     *
     * @param username entered username
     * @param plainPassword entered plain-text password
     * @return authenticated User, or null if invalid
     */
    public static User authenticate(String username, String plainPassword) {
        if (username == null || username.trim().isEmpty() || plainPassword == null || plainPassword.isEmpty()) {
            return null;
        }

        User user = userRepo.findByUsername(username.trim());
        if (user != null && user.getPasswordHash() != null) {
            if (PasswordUtil.verifyPassword(plainPassword, user.getPasswordHash())) {
                return user;
            }
        }
        return null;
    }

    public static boolean changePassword(int userId, String newPlainPassword) {
        if (newPlainPassword == null || newPlainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("New password cannot be empty.");
        }
        User user = userRepo.findById(userId);
        if (user == null) {
            return false;
        }

        user.setPasswordHash(PasswordUtil.hashPassword(newPlainPassword.trim()));
        userRepo.update(user);
        return true;
    }

    public static List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public static User getUserById(int id) {
        return userRepo.findById(id);
    }
}
