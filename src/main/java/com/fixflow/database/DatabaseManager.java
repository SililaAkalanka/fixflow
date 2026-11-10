package com.fixflow.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages SQLite database connections, lifecycle, and schema initialization.
 */
public class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    private static final String APP_DIR_NAME = "FixFlow";
    private static final String DB_FILE_NAME = "fixflow.db";
    private static final String SCHEMA_RESOURCE_PATH = "/db/schema.sql";

    private static String customJdbcUrl = null;

    // Expected core tables
    public static final String[] CORE_TABLES = {
            "users",
            "customers",
            "devices",
            "technicians",
            "repair_jobs",
            "spare_parts",
            "repair_part_usage",
            "payments",
            "warranties",
            "repair_status_history"
    };

    /**
     * Resolves the persistent SQLite JDBC connection URL.
     * On Windows: %APPDATA%/FixFlow/fixflow.db
     *
     * @return JDBC connection string
     */
    public static synchronized String getJdbcUrl() {
        if (customJdbcUrl != null && !customJdbcUrl.trim().isEmpty()) {
            return customJdbcUrl;
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        File baseDir;

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.trim().isEmpty()) {
                baseDir = new File(appData, APP_DIR_NAME);
            } else {
                baseDir = new File(System.getProperty("user.home"), "AppData/Roaming/" + APP_DIR_NAME);
            }
        } else if (os.contains("mac")) {
            baseDir = new File(System.getProperty("user.home"), "Library/Application Support/" + APP_DIR_NAME);
        } else {
            baseDir = new File(System.getProperty("user.home"), ".fixflow");
        }

        if (!baseDir.exists()) {
            boolean created = baseDir.mkdirs();
            if (created) {
                LOGGER.info("Created application data directory at: " + baseDir.getAbsolutePath());
            }
        }

        File dbFile = new File(baseDir, DB_FILE_NAME);
        return "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    /**
     * Sets a custom JDBC URL (used primarily for testing, e.g., in-memory SQLite).
     *
     * @param url custom JDBC URL, or null to revert to default
     */
    public static synchronized void setCustomJdbcUrl(String url) {
        customJdbcUrl = url;
    }

    /**
     * Obtains a new database connection with SQLite foreign keys and WAL mode enabled.
     *
     * @return active {@link Connection}
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        String url = getJdbcUrl();
        Connection conn = DriverManager.getConnection(url);

        try (Statement stmt = conn.createStatement()) {
            // Business rule: Enforce relational integrity and foreign-key constraints
            stmt.execute("PRAGMA foreign_keys = ON;");

            // Enable Write-Ahead Logging for better concurrent read performance if not in-memory
            if (!url.contains(":memory:")) {
                stmt.execute("PRAGMA journal_mode = WAL;");
            }
        }

        return conn;
    }

    /**
     * Initializes the database schema, creating all tables and indexes if they do not exist.
     *
     * @throws SQLException if schema creation fails
     */
    public static synchronized void initializeDatabase() throws SQLException {
        LOGGER.info("Initializing FixFlow database schema using URL: " + getJdbcUrl());

        List<String> ddlStatements = loadSchemaStatements();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(false);
            try {
                for (String ddl : ddlStatements) {
                    String trimmed = ddl.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
                conn.commit();
                LOGGER.info("Database schema initialized successfully.");
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to apply database schema, rolling back.", e);
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Checks if a specified table exists in the database.
     *
     * @param tableName name of the table to check
     * @return true if the table exists, false otherwise
     * @throws SQLException if a database metadata lookup fails
     */
    public static boolean tableExists(String tableName) throws SQLException {
        try (Connection conn = getConnection()) {
            return tableExists(conn, tableName);
        }
    }

    /**
     * Checks if a specified table exists in the given connection.
     *
     * @param conn active database connection
     * @param tableName name of the table to check
     * @return true if the table exists, false otherwise
     * @throws SQLException if a database metadata lookup fails
     */
    public static boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
            if (rs.next()) {
                return true;
            }
        }
        // Fallback case-insensitive check for SQLite
        try (ResultSet rs = meta.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = meta.getTables(null, null, tableName.toLowerCase(), new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    /**
     * Verifies that all expected core tables exist in the database.
     *
     * @return true if all core tables are present
     * @throws SQLException if a database check fails
     */
    public static boolean verifySchema() throws SQLException {
        try (Connection conn = getConnection()) {
            for (String table : CORE_TABLES) {
                if (!tableExists(conn, table)) {
                    LOGGER.warning("Missing required table: " + table);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Loads SQL statements from the classpath schema resource.
     *
     * @return list of SQL statements
     */
    private static List<String> loadSchemaStatements() {
        List<String> statements = new ArrayList<>();
        InputStream is = DatabaseManager.class.getResourceAsStream(SCHEMA_RESOURCE_PATH);

        if (is == null) {
            LOGGER.warning("Schema resource " + SCHEMA_RESOURCE_PATH + " not found on classpath. Using embedded fallback.");
            return getFallbackSchemaStatements();
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder currentStatement = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                // Skip empty lines and single-line SQL comments
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
            LOGGER.log(Level.WARNING, "Failed reading schema from resource. Falling back to embedded statements.", e);
            return getFallbackSchemaStatements();
        }

        return statements;
    }

    /**
     * Embedded fallback schema statements to guarantee initialization even if external resource is missing.
     */
    private static List<String> getFallbackSchemaStatements() {
        List<String> list = new ArrayList<>();
        list.add("CREATE TABLE IF NOT EXISTS users (user_id INTEGER PRIMARY KEY AUTOINCREMENT, role TEXT UNIQUE NOT NULL, username TEXT UNIQUE NOT NULL, password_hash TEXT NOT NULL, created_at TEXT NOT NULL, updated_at TEXT);");
        list.add("CREATE TABLE IF NOT EXISTS customers (customer_id INTEGER PRIMARY KEY AUTOINCREMENT, full_name TEXT NOT NULL, phone TEXT NOT NULL, email TEXT, address TEXT, created_at TEXT NOT NULL);");
        list.add("CREATE TABLE IF NOT EXISTS devices (device_id INTEGER PRIMARY KEY AUTOINCREMENT, customer_id INTEGER NOT NULL, category TEXT NOT NULL, brand TEXT NOT NULL, model TEXT NOT NULL, serial_number TEXT, colour TEXT, accessories TEXT, created_at TEXT NOT NULL, FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT);");
        list.add("CREATE TABLE IF NOT EXISTS technicians (technician_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT NOT NULL, specialization TEXT, is_active INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)), created_at TEXT NOT NULL);");
        list.add("CREATE TABLE IF NOT EXISTS repair_jobs (job_id INTEGER PRIMARY KEY AUTOINCREMENT, customer_id INTEGER NOT NULL, device_id INTEGER NOT NULL, technician_id INTEGER, date_received TEXT NOT NULL, initial_problem TEXT NOT NULL, current_status TEXT NOT NULL, diagnosis_findings TEXT, estimated_cost REAL, labour_charges REAL DEFAULT 0.0, estimated_completion_date TEXT, actual_completion_date TEXT, approval_status TEXT NOT NULL DEFAULT 'PENDING' CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED')), approval_date TEXT, cancellation_reason TEXT, original_job_id INTEGER, created_at TEXT NOT NULL, updated_at TEXT NOT NULL, FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT, FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE RESTRICT, FOREIGN KEY (technician_id) REFERENCES technicians(technician_id) ON DELETE RESTRICT, FOREIGN KEY (original_job_id) REFERENCES repair_jobs(job_id) ON DELETE SET NULL);");
        list.add("CREATE TABLE IF NOT EXISTS spare_parts (part_id INTEGER PRIMARY KEY AUTOINCREMENT, part_name TEXT NOT NULL, category TEXT NOT NULL, compatible_device_model TEXT NOT NULL, purchase_cost REAL NOT NULL CHECK (purchase_cost >= 0), selling_price REAL NOT NULL CHECK (selling_price >= 0), available_quantity INTEGER NOT NULL DEFAULT 0 CHECK (available_quantity >= 0), min_stock_level INTEGER NOT NULL DEFAULT 5 CHECK (min_stock_level >= 0), created_at TEXT NOT NULL, updated_at TEXT NOT NULL);");
        list.add("CREATE TABLE IF NOT EXISTS repair_part_usage (usage_id INTEGER PRIMARY KEY AUTOINCREMENT, job_id INTEGER NOT NULL, part_id INTEGER NOT NULL, quantity INTEGER NOT NULL CHECK (quantity > 0), selling_price_at_time REAL NOT NULL CHECK (selling_price_at_time >= 0), date_used TEXT NOT NULL, FOREIGN KEY (job_id) REFERENCES repair_jobs(job_id) ON DELETE RESTRICT, FOREIGN KEY (part_id) REFERENCES spare_parts(part_id) ON DELETE RESTRICT);");
        list.add("CREATE TABLE IF NOT EXISTS payments (payment_id INTEGER PRIMARY KEY AUTOINCREMENT, job_id INTEGER UNIQUE NOT NULL, payment_date TEXT NOT NULL, total_repair_cost REAL NOT NULL CHECK (total_repair_cost >= 0), amount_paid REAL NOT NULL CHECK (amount_paid >= 0), payment_method TEXT NOT NULL DEFAULT 'CASH', payment_status TEXT NOT NULL DEFAULT 'PAID', created_at TEXT NOT NULL, FOREIGN KEY (job_id) REFERENCES repair_jobs(job_id) ON DELETE RESTRICT);");
        list.add("CREATE TABLE IF NOT EXISTS warranties (warranty_id INTEGER PRIMARY KEY AUTOINCREMENT, job_id INTEGER UNIQUE NOT NULL, start_date TEXT NOT NULL, expiry_date TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'EXPIRED', 'CLAIMED')), created_at TEXT NOT NULL, FOREIGN KEY (job_id) REFERENCES repair_jobs(job_id) ON DELETE RESTRICT);");
        list.add("CREATE TABLE IF NOT EXISTS repair_status_history (history_id INTEGER PRIMARY KEY AUTOINCREMENT, job_id INTEGER NOT NULL, from_status TEXT, to_status TEXT NOT NULL, changed_by_role TEXT, change_date TEXT NOT NULL, notes TEXT, FOREIGN KEY (job_id) REFERENCES repair_jobs(job_id) ON DELETE CASCADE);");
        return list;
    }
}

