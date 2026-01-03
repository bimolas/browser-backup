package com.example.nexus.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    public DatabaseManager() {

        logger.debug("DatabaseManager created");
    }

    public void initialize() {
        try {

            boolean ok = DatabaseConnection.getInstance().testConnection();
            if (ok) logger.info("Database connection available");
            else logger.warn("Database connection test returned false");
        } catch (Exception e) {
            logger.error("Failed to initialize DatabaseManager (connection test)", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public void close() {
        DatabaseConnection.getInstance().closePool();
    }

    public String getJdbcUrl() {
        return DatabaseConnection.getInstance().getJdbcUrl();
    }
}
