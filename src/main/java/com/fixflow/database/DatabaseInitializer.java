package com.fixflow.database;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns schema initialization and safe migration for existing databases.
 */
public class DatabaseInitializer {

    private static final Logger LOGGER = Logger.getLogger(DatabaseInitializer.class.getName());
    private static final String SCHEMA_RESOURCE_PATH = "/db/schema.sql";

    private DatabaseInitializer() {
        // Utility class
    }

    /**
     * Initializes the database schema and performs safe migrations.
     *
     * @throws SQLException if initialization fails
     */
    public static synchronized void initializeDatabase() throws SQLException {
        LOGGER.info("Initializing database using URL: " + DatabaseConnection.getJdbcUrl());

        List<String> ddlStatements = loadSchemaStatements();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(false);
            try {
                for (String ddl : ddlStatements) {
                    String trimmed = ddl.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }

                // Apply migrations for existing schemas
                migrateDatabase(conn);

                conn.commit();
                LOGGER.info("Database schema initialized and migrated successfully.");
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to initialize or migrate database schema, rolling back.", e);
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Performs safe, backward-compatible schema migrations for existing databases.
     *
     * @param conn active transaction connection
     * @throws SQLException if a migration operation fails
     */
    private static void migrateDatabase(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {

            // 1. Devices migration: add warranty fields if missing
            Set<String> deviceColumns = getTableColumns(conn, "devices");
            if (!deviceColumns.isEmpty()) {
                if (!deviceColumns.contains("warranty_months")) {
                    stmt.execute("ALTER TABLE devices ADD COLUMN warranty_months INTEGER DEFAULT 0;");
                }
                if (!deviceColumns.contains("warranty_start_date")) {
                    stmt.execute("ALTER TABLE devices ADD COLUMN warranty_start_date TEXT;");
                }
                if (!deviceColumns.contains("warranty_expiry_date")) {
                    stmt.execute("ALTER TABLE devices ADD COLUMN warranty_expiry_date TEXT;");
                }
            }

            // 2. Warranties migration: if legacy warranties table exists, preserve data then remove table
            if (tableExists(conn, "warranties")) {
                LOGGER.info("Migrating legacy warranty records into devices...");
                String copyWarranties = "UPDATE devices SET " +
                        "warranty_start_date = (SELECT w.start_date FROM warranties w JOIN repair_jobs r ON w.job_id = r.job_id WHERE r.device_id = devices.device_id LIMIT 1), " +
                        "warranty_expiry_date = (SELECT w.expiry_date FROM warranties w JOIN repair_jobs r ON w.job_id = r.job_id WHERE r.device_id = devices.device_id LIMIT 1) " +
                        "WHERE EXISTS (SELECT 1 FROM warranties w JOIN repair_jobs r ON w.job_id = r.job_id WHERE r.device_id = devices.device_id)";
                try {
                    stmt.execute(copyWarranties);
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Failed to copy legacy warranties, skipping copy step", e);
                }
                stmt.execute("DROP TABLE IF EXISTS warranties;");
            }

            // 3. Repair Jobs migration: add notes column and map old statuses
            Set<String> repairColumns = getTableColumns(conn, "repair_jobs");
            if (!repairColumns.isEmpty()) {
                if (!repairColumns.contains("notes")) {
                    stmt.execute("ALTER TABLE repair_jobs ADD COLUMN notes TEXT;");
                }

                // Map legacy complex statuses to the simplified 6-status lifecycle
                stmt.execute("UPDATE repair_jobs SET current_status = 'IN_PROGRESS' WHERE current_status IN ('WAITING_FOR_APPROVAL', 'WAITING_FOR_PARTS', 'REPAIRING');");
                stmt.execute("UPDATE repair_jobs SET current_status = 'COMPLETED' WHERE current_status = 'READY_FOR_COLLECTION';");
            }

            // 4. Payments migration: remove UNIQUE(job_id) constraint to support multiple partial payments
            if (hasUniqueJobIdConstraint(conn)) {
                LOGGER.info("Migrating payments table to remove UNIQUE(job_id) constraint for partial payments...");
                stmt.execute("CREATE TABLE IF NOT EXISTS payments_migrated (" +
                        "payment_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "job_id INTEGER NOT NULL, " +
                        "payment_date TEXT NOT NULL, " +
                        "amount_paid REAL NOT NULL CHECK (amount_paid >= 0), " +
                        "payment_method TEXT NOT NULL DEFAULT 'CASH', " +
                        "notes TEXT, " +
                        "created_at TEXT NOT NULL, " +
                        "FOREIGN KEY (job_id) REFERENCES repair_jobs(job_id) ON DELETE RESTRICT" +
                        ");");

                Set<String> payCols = getTableColumns(conn, "payments");
                String notesExpr = payCols.contains("notes") ? "notes" : (payCols.contains("payment_status") ? "payment_status" : "''");

                stmt.execute("INSERT INTO payments_migrated (payment_id, job_id, payment_date, amount_paid, payment_method, notes, created_at) " +
                        "SELECT payment_id, job_id, payment_date, amount_paid, payment_method, " + notesExpr + ", created_at FROM payments;");

                stmt.execute("DROP TABLE payments;");
                stmt.execute("ALTER TABLE payments_migrated RENAME TO payments;");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_payments_job ON payments(job_id);");
            }

            // 5. Remove legacy repair_status_history table if present
            if (tableExists(conn, "repair_status_history")) {
                stmt.execute("DROP TABLE IF EXISTS repair_status_history;");
            }

            // 6. Clean up obsolete roles in users table
            stmt.execute("DELETE FROM users WHERE role NOT IN ('ADMINISTRATOR', 'TECHNICIAN');");
        }
    }

    /**
     * Checks if a specified table exists in the connection.
     */
    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND LOWER(name)=LOWER(?)";
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Retrieves column names for a specified table using PRAGMA table_info.
     */
    private static Set<String> getTableColumns(Connection conn, String tableName) throws SQLException {
        Set<String> columns = new HashSet<>();
        if (!tableExists(conn, tableName)) {
            return columns;
        }
        String sql = "PRAGMA table_info(" + tableName + ");";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                columns.add(rs.getString("name").toLowerCase());
            }
        }
        return columns;
    }

    /**
     * Checks whether payments table enforces a UNIQUE constraint on job_id.
     */
    private static boolean hasUniqueJobIdConstraint(Connection conn) throws SQLException {
        if (!tableExists(conn, "payments")) {
            return false;
        }

        // Check sqlite_master table SQL definition for UNIQUE on job_id
        String sql = "SELECT sql FROM sqlite_master WHERE type='table' AND name='payments'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                String tableSql = rs.getString(1);
                if (tableSql != null) {
                    String upper = tableSql.toUpperCase();
                    if (upper.matches("(?s).*\\bJOB_ID\\b[^,)]*\\bUNIQUE\\b.*") ||
                        upper.matches("(?s).*\\bUNIQUE\\b\\s*\\([^)]*\\bJOB_ID\\b[^)]*\\).*")) {
                        return true;
                    }
                }
            }
        }

        // Also check unique indexes on payments
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA index_list(payments);")) {
            while (rs.next()) {
                int isUnique = rs.getInt("unique");
                String indexName = rs.getString("name");
                if (isUnique == 1 && indexName != null) {
                    try (Statement stmt2 = conn.createStatement();
                         ResultSet rs2 = stmt2.executeQuery("PRAGMA index_info('" + indexName + "');")) {
                        while (rs2.next()) {
                            if ("job_id".equalsIgnoreCase(rs2.getString("name"))) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private static List<String> loadSchemaStatements() {
        List<String> statements = new ArrayList<>();
        InputStream is = DatabaseInitializer.class.getResourceAsStream(SCHEMA_RESOURCE_PATH);

        if (is == null) {
            LOGGER.warning("Schema resource " + SCHEMA_RESOURCE_PATH + " not found on classpath. Using fallback statements.");
            return getFallbackSchemaStatements();
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder currentStatement = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }

                currentStatement.append(line).append("\n");

                if (trimmed.endsWith(";")) {
                    statements.add(currentStatement.toString());
                    currentStatement.setLength(0);
                }
            }

            if (currentStatement.length() > 0 && !currentStatement.toString().trim().isEmpty()) {
                statements.add(currentStatement.toString());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed reading schema resource, falling back.", e);
            return getFallbackSchemaStatements();
        }

        return statements;
    }

    private static List<String> getFallbackSchemaStatements() {
        List<String> list = new ArrayList<>();
        list.add("CREATE TABLE IF NOT EXISTS users (user_id INTEGER PRIMARY KEY AUTOINCREMENT, role TEXT UNIQUE NOT NULL, username TEXT UNIQUE NOT NULL, password_hash TEXT NOT NULL, created_at TEXT NOT NULL, updated_at TEXT);");
        list.add("CREATE TABLE IF NOT EXISTS customers (customer_id INTEGER PRIMARY KEY AUTOINCREMENT, full_name TEXT NOT NULL, phone TEXT NOT NULL, email TEXT, address TEXT, created_at TEXT NOT NULL);");
        list.add("CREATE TABLE IF NOT EXISTS devices (device_id INTEGER PRIMARY KEY AUTOINCREMENT, customer_id INTEGER NOT NULL, category TEXT NOT NULL, brand TEXT NOT NULL, model TEXT NOT NULL, serial_number TEXT, colour TEXT, accessories TEXT, warranty_months INTEGER DEFAULT 0, warranty_start_date TEXT, warranty_expiry_date TEXT, created_at TEXT NOT NULL, FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT);");
        list.add("CREATE TABLE IF NOT EXISTS technicians (technician_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT NOT NULL, specialization TEXT, is_active INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)), created_at TEXT NOT NULL);");
        list.add("CREATE TABLE IF NOT EXISTS repair_jobs (job_id INTEGER PRIMARY KEY AUTOINCREMENT, customer_id INTEGER NOT NULL, device_id INTEGER NOT NULL, technician_id INTEGER, date_received TEXT NOT NULL, initial_problem TEXT NOT NULL, current_status TEXT NOT NULL CHECK (current_status IN ('RECEIVED', 'DIAGNOSING', 'IN_PROGRESS', 'COMPLETED', 'COLLECTED', 'CANCELLED')), diagnosis_findings TEXT, estimated_cost REAL, labour_charges REAL DEFAULT 0.0, estimated_completion_date TEXT, actual_completion_date TEXT, notes TEXT, created_at TEXT NOT NULL, updated_at TEXT NOT NULL, FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT, FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE RESTRICT, FOREIGN KEY (technician_id) REFERENCES technicians(technician_id) ON DELETE RESTRICT);");
        list.add("CREATE TABLE IF NOT EXISTS spare_parts (part_id INTEGER PRIMARY KEY AUTOINCREMENT, part_name TEXT NOT NULL, category TEXT NOT NULL, compatible_device_model TEXT NOT NULL, purchase_cost REAL NOT NULL CHECK (purchase_cost >= 0), selling_price REAL NOT NULL CHECK (selling_price >= 0), available_quantity INTEGER NOT NULL DEFAULT 0 CHECK (available_quantity >= 0), min_stock_level INTEGER NOT NULL DEFAULT 5 CHECK (min_stock_level >= 0), created_at TEXT NOT NULL, updated_at TEXT NOT NULL);");
        list.add("CREATE TABLE IF NOT EXISTS repair_part_usage (usage_id INTEGER PRIMARY KEY AUTOINCREMENT, job_id INTEGER NOT NULL, part_id INTEGER NOT NULL, quantity INTEGER NOT NULL CHECK (quantity > 0), selling_price_at_time REAL NOT NULL CHECK (selling_price_at_time >= 0), date_used TEXT NOT NULL, FOREIGN KEY (job_id) REFERENCES repair_jobs(job_id) ON DELETE RESTRICT, FOREIGN KEY (part_id) REFERENCES spare_parts(part_id) ON DELETE RESTRICT);");
        list.add("CREATE TABLE IF NOT EXISTS payments (payment_id INTEGER PRIMARY KEY AUTOINCREMENT, job_id INTEGER NOT NULL, payment_date TEXT NOT NULL, amount_paid REAL NOT NULL CHECK (amount_paid >= 0), payment_method TEXT NOT NULL DEFAULT 'CASH', notes TEXT, created_at TEXT NOT NULL, FOREIGN KEY (job_id) REFERENCES repair_jobs(job_id) ON DELETE RESTRICT);");
        return list;
    }
}
