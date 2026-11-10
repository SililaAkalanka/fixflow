package com.fixflow.database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Convenient access facade for database connectivity.
 * Specified in Project Specification Section 10.5.
 */
public class DatabaseConnection {

    private DatabaseConnection() {
        // Utility class
    }

    /**
     * Obtains an active database connection with foreign-key support enabled.
     *
     * @return active {@link Connection}
     * @throws SQLException if connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        return DatabaseManager.getConnection();
    }
}

