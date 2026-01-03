package com.example.nexus.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseMigration {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigration.class);

    public static void migrate(Connection conn) {
        try {
            migrateProfileTable(conn);
            logger.info("Database migration completed successfully");
        } catch (Exception e) {
            logger.error("Database migration failed", e);
        }
    }

    private static void migrateProfileTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {

            try {
                stmt.execute("ALTER TABLE profile ADD COLUMN password_hash TEXT");
                logger.info("Added password_hash column to profile table");
            } catch (SQLException e) {

                logger.debug("password_hash column already exists");
            }

            try {
                stmt.execute("ALTER TABLE profile ADD COLUMN is_guest BOOLEAN DEFAULT 0");
                logger.info("Added is_guest column to profile table");
            } catch (SQLException e) {

                logger.debug("is_guest column already exists");
            }

            try {
                stmt.execute("ALTER TABLE profile ADD COLUMN logged_in BOOLEAN DEFAULT 1");
                logger.info("Added logged_in column to profile table");
            } catch (SQLException e) {

                logger.debug("logged_in column already exists");
            }
        }
    }
}
