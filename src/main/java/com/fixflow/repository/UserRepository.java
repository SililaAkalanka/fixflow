package com.fixflow.repository;

import com.fixflow.model.User;
import com.fixflow.util.JsonFileHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing User persistence in JSON format.
 * Implements Repository<User> using an ArrayList.
 * Supports polymorphic User instances (AdminUser, TechnicianUser).
 */
public class UserRepository implements Repository<User> {

    private static final String DEFAULT_FILE_PATH = "data/users.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String filePath;
    private List<User> users;

    public UserRepository() {
        this(DEFAULT_FILE_PATH);
    }

    public UserRepository(String filePath) {
        this.filePath = filePath;
        this.users = new ArrayList<>();
        load();
    }

    @Override
    public void add(User user) {
        if (user.getUserId() <= 0) {
            user.setUserId(generateNextId());
        }
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        if (user.getCreatedAt() == null || user.getCreatedAt().isEmpty()) {
            user.setCreatedAt(now);
        }
        user.setUpdatedAt(now);
        users.add(user);
        save();
    }

    @Override
    public User findById(int id) {
        for (User u : users) {
            if (u.getUserId() == id) {
                return u;
            }
        }
        return null;
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    @Override
    public void update(User user) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUserId() == user.getUserId()) {
                user.setUpdatedAt(LocalDateTime.now().format(DATE_FORMATTER));
                users.set(i, user);
                save();
                return;
            }
        }
    }

    @Override
    public boolean delete(int id) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUserId() == id) {
                users.remove(i);
                save();
                return true;
            }
        }
        return false;
    }

    @Override
    public void save() {
        JsonFileHandler.saveList(filePath, users);
    }

    @Override
    public void load() {
        users = JsonFileHandler.loadList(filePath, User.class);
    }

    public User findByUsername(String username) {
        if (username == null) return null;
        for (User u : users) {
            if (u.getUsername() != null && u.getUsername().equalsIgnoreCase(username.trim())) {
                return u;
            }
        }
        return null;
    }

    private int generateNextId() {
        int maxId = 0;
        for (User u : users) {
            if (u.getUserId() > maxId) {
                maxId = u.getUserId();
            }
        }
        return maxId + 1;
    }
}
