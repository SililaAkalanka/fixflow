package com.fixflow.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Manages SQLite database connections and configuration.
 * Directly owns JDBC connection creation and ensures foreign keys are enabled.
 */
public class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static final String APP_DIR_NAME = "FixFlow";
    private static final String DB_FILE_NAME = "fixflow.db";

    private static String customJdbcUrl = null;

    private DatabaseConnection() {
        // Utility class
    }

    /**
     * Resolves the persistent SQLite JDBC connection URL.
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
            baseDir.mkdirs();
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
     * Obtains an active SQLite database connection with foreign keys enabled.
     *
     * @return active {@link Connection}
     * @throws SQLException if connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        String url = getJdbcUrl();
        Connection conn = DriverManager.getConnection(url);

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            if (!url.contains(":memory:")) {
                stmt.execute("PRAGMA journal_mode = WAL;");
            }
        }

        return conn;
    }
}
