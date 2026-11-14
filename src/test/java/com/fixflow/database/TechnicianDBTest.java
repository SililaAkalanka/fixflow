package com.fixflow.database;

import com.fixflow.model.Technician;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TechnicianDBTest {

    private Connection sharedMemoryConnection;

    @BeforeEach
    void setUp() throws SQLException {
        String testUrl = "jdbc:sqlite:file:techdb_test?mode=memory&cache=shared";
        DatabaseConnection.setCustomJdbcUrl(testUrl);

        sharedMemoryConnection = DatabaseConnection.getConnection();
        DatabaseInitializer.initializeDatabase();
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (sharedMemoryConnection != null && !sharedMemoryConnection.isClosed()) {
            sharedMemoryConnection.close();
        }
        DatabaseConnection.setCustomJdbcUrl(null);
    }

    @Test
    @DisplayName("Verify technician creation, retrieval, and active status")
    void testCreateAndGetTechnician() throws SQLException {
        Technician tech = new Technician("Marcus Vance", "0771122334", "Micro-soldering & Logic Boards", true);
        int id = TechnicianDB.createTechnician(tech);
        assertTrue(id > 0);

        Technician retrieved = TechnicianDB.getTechnicianById(id);
        assertNotNull(retrieved);
        assertEquals("Marcus Vance", retrieved.getName());
        assertEquals("0771122334", retrieved.getPhone());
        assertEquals("Micro-soldering & Logic Boards", retrieved.getSpecialization());
        assertTrue(retrieved.isActive());
    }

    @Test
    @DisplayName("Verify technician deactivation and active filter")
    void testDeactivateTechnician() throws SQLException {
        Technician t1 = new Technician("Active Tech", "0771111111", "Screens", true);
        Technician t2 = new Technician("To-Deactivate Tech", "0772222222", "Batteries", true);

        TechnicianDB.createTechnician(t1);
        int id2 = TechnicianDB.createTechnician(t2);

        assertEquals(2, TechnicianDB.getActiveTechnicians().size());

        // Deactivate t2
        boolean updated = TechnicianDB.setTechnicianActive(id2, false);
        assertTrue(updated);

        Technician deactivated = TechnicianDB.getTechnicianById(id2);
        assertFalse(deactivated.isActive());

        // getActiveTechnicians must now return only 1
        List<Technician> activeList = TechnicianDB.getActiveTechnicians();
        assertEquals(1, activeList.size());
        assertEquals("Active Tech", activeList.get(0).getName());
    }

    @Test
    @DisplayName("Verify technician search with active filter")
    void testSearchTechnicians() throws SQLException {
        TechnicianDB.createTechnician(new Technician("Sarah Connor", "0712345678", "Hardware Diagnostics", true));
        TechnicianDB.createTechnician(new Technician("John Connor", "0723456789", "Screen Replacement", false));

        List<Technician> activeResults = TechnicianDB.searchTechnicians("Connor", "Active Only");
        assertEquals(1, activeResults.size());
        assertEquals("Sarah Connor", activeResults.get(0).getName());

        List<Technician> inactiveResults = TechnicianDB.searchTechnicians("Connor", "Inactive Only");
        assertEquals(1, inactiveResults.size());
        assertEquals("John Connor", inactiveResults.get(0).getName());

        List<Technician> allResults = TechnicianDB.searchTechnicians("Connor", "All");
        assertEquals(2, allResults.size());
    }

    @Test
    @DisplayName("Verify deletion blocked if technician has assigned repair jobs")
    void testDeleteTechnicianBlockedWithJobs() throws SQLException {
        Technician tech = new Technician("Assigned Tech", "0799998888", "General", true);
        int techId = TechnicianDB.createTechnician(tech);

        // Insert prerequisite customer, device, and job assigned to this tech
        int custId;
        int devId;
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO customers (full_name, phone, created_at) VALUES ('Client A', '07111', datetime('now'))",
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.executeUpdate();
                var rs = ps.getGeneratedKeys();
                rs.next();
                custId = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO devices (customer_id, category, brand, model, created_at) VALUES (?, 'Phone', 'Brand', 'Model', datetime('now'))",
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, custId);
                ps.executeUpdate();
                var rs = ps.getGeneratedKeys();
                rs.next();
                devId = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO repair_jobs (customer_id, device_id, technician_id, date_received, initial_problem, current_status, created_at, updated_at) " +
                            "VALUES (?, ?, ?, datetime('now'), 'Broken screen', 'RECEIVED', datetime('now'), datetime('now'))")) {
                ps.setInt(1, custId);
                ps.setInt(2, devId);
                ps.setInt(3, techId);
                ps.executeUpdate();
            }
        }

        SQLException ex = assertThrows(SQLException.class, () -> TechnicianDB.deleteTechnician(techId));
        assertTrue(ex.getMessage().contains("Associated repair assignment history exists"));
    }
}

