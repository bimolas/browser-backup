package com.example.nexus.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_URL = "jdbc:sqlite:identifier.sqlite";
    private Connection connection;

    public void initialize() {
        try {
            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Create a connection to the database
            connection = DriverManager.getConnection(DB_URL);

            // Initialize the database schema
            initializeSchema();

            logger.info("Database initialized successfully");
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private void initializeSchema() {
        try (InputStream is = getClass().getResourceAsStream("/com/example/nexus/db/init.sql")) {
            if (is == null) {
                logger.error("Database initialization script not found at /com/example/nexus/db/init.sql");
                // Try alternate path
                try (InputStream altIs = getClass().getClassLoader().getResourceAsStream("com/example/nexus/db/init.sql")) {
                    if (altIs == null) {
                        logger.error("Database initialization script not found at alternate path either");
                        return;
                    }
                    executeInitScript(altIs);
                    return;
                }
            }

            executeInitScript(is);
        } catch (Exception e) {
            logger.error("Failed to initialize database schema", e);
        }
    }

    private void executeInitScript(InputStream is) throws Exception {
        String sql = new String(is.readAllBytes());

        try (Statement stmt = connection.createStatement()) {
            // Execute each SQL statement
            for (String statement : sql.split(";")) {
                if (!statement.trim().isEmpty()) {
                    stmt.execute(statement);
                }
            }

            logger.info("Database schema initialized");
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed");
            }
        } catch (SQLException e) {
            logger.error("Failed to close database connection", e);
        }
    }
}