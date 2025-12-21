package com.example.nexus.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static volatile DatabaseConnection instance;
    private final String jdbcUrl;

    private DatabaseConnection() {
        String configured = System.getProperty("db.url");
        if (configured != null && !configured.isBlank()) {
            jdbcUrl = configured;
        } else {
            // Default to a stable per-user location under ~/.nexus/identifier.sqlite
            String userHome = System.getProperty("user.home");
            File appDir = new File(userHome, ".nexus");
            if (!appDir.exists()) {
                boolean created = appDir.mkdirs();
                if (created) logger.info("Created application directory: {}", appDir.getAbsolutePath());
            }
            File dbFile = new File(appDir, "identifier.sqlite");
            jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        }

        try {
            // Ensure directory and file creation by opening a connection and closing it
            DriverManager.getConnection(jdbcUrl).close();
            logger.info("Database initialized using URL: {}", jdbcUrl);
        } catch (SQLException e) {
            logger.error("Failed to test DriverManager connection", e);
            throw new RuntimeException("Failed to initialize database connection", e);
        }
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) instance = new DatabaseConnection();
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void closePool() {
        logger.info("DriverManager-based DatabaseConnection close called (no-op)");
    }

    public boolean testConnection() {
        try (Connection c = getConnection()) {
            return c != null && !c.isClosed();
        } catch (SQLException e) {
            logger.warn("Database connection test failed", e);
            return false;
        }
    }
}
