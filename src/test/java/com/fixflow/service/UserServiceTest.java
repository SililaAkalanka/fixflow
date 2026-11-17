package com.fixflow.service;

import com.fixflow.enums.UserRole;
import com.fixflow.model.AdminUser;
import com.fixflow.model.TechnicianUser;
import com.fixflow.model.User;
import com.fixflow.repository.UserRepository;
import com.fixflow.util.PasswordUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    private File tempDir;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = File.createTempFile("fixflow_usertest_", "_dir");
        tempDir.delete();
        tempDir.mkdirs();

        UserRepository ur = new UserRepository(new File(tempDir, "users.json").getAbsolutePath());
        UserService.setRepository(ur);
    }

    @AfterEach
    void tearDown() {
        if (tempDir != null && tempDir.exists()) {
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
            tempDir.delete();
        }
    }

    @Test
    @DisplayName("Verify user authentication with BCrypt hashed credentials")
    void testAuthentication() {
        UserRepository ur = UserService.getUserRepository();
        AdminUser admin = new AdminUser("adminUser", PasswordUtil.hashPassword("secret123"));
        ur.add(admin);

        TechnicianUser tech = new TechnicianUser("techUser", PasswordUtil.hashPassword("techPass"));
        ur.add(tech);

        // Valid authentication
        User authedAdmin = UserService.authenticate("adminUser", "secret123");
        assertNotNull(authedAdmin);
        assertEquals(UserRole.ADMINISTRATOR, authedAdmin.getRole());
        assertTrue(authedAdmin.canManageUsers());

        User authedTech = UserService.authenticate("techUser", "techPass");
        assertNotNull(authedTech);
        assertEquals(UserRole.TECHNICIAN, authedTech.getRole());
        assertFalse(authedTech.canManageUsers());

        // Invalid password
        assertNull(UserService.authenticate("adminUser", "wrongPass"));

        // Non-existent username
        assertNull(UserService.authenticate("ghost", "pass"));
    }

    @Test
    @DisplayName("Verify password change updates the stored hash")
    void testPasswordChange() {
        UserRepository ur = UserService.getUserRepository();
        AdminUser admin = new AdminUser("adminChange", PasswordUtil.hashPassword("oldPass"));
        ur.add(admin);

        assertTrue(UserService.changePassword(admin.getUserId(), "newSecurePass"));
        assertNull(UserService.authenticate("adminChange", "oldPass"));
        assertNotNull(UserService.authenticate("adminChange", "newSecurePass"));
    }
}
