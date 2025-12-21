package com.example.nexus.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Lightweight connection manager. This class only exposes connection utilities and
 * delegates schema/migration/initialization logic to DatabaseInitializer.
 *
 * Goal: keep all SQL and database schema logic out of this class so it's a simple
 * utility that other parts can use safely.
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    public DatabaseManager() {
        // No heavy initialization here — keep constructor cheap. The DatabaseConnection
        // singleton will lazily prepare the JDBC file if needed.
        logger.debug("DatabaseManager created");
    }

    /**
     * Ensure the database file/URL is reachable. This does not perform any SQL migration.
     */
    public void initialize() {
        try {
            // Trigger creation/test of the underlying connection
            boolean ok = DatabaseConnection.getInstance().testConnection();
            if (ok) logger.info("Database connection available");
            else logger.warn("Database connection test returned false");
        } catch (Exception e) {
            logger.error("Failed to initialize DatabaseManager (connection test)", e);
        }
    }

    /** Returns a new connection. Caller is responsible for closing it. */
    public Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    /** Close any pooled resources (no-op for DriverManager-backed connection). */
    public void close() {
        DatabaseConnection.getInstance().closePool();
    }

    /** Convenience: return JDBC URL used by the connection. */
    public String getJdbcUrl() {
        return DatabaseConnection.getInstance().getJdbcUrl();
    }
}