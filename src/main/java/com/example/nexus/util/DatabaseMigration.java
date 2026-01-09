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
            migrateTabsTable(conn);
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
            } catch (SQLException e) {
                logger.info("Database migration result in an error " + e);

            }
        }
    }

    private static void migrateTabsTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            boolean hasUserId = false;
            boolean hasProfileId = false;

            try {
                var rs = stmt.executeQuery("PRAGMA table_info(tabs)");
                while (rs.next()) {
                    String columnName = rs.getString("name");
                    if ("user_id".equals(columnName)) {
                        hasUserId = true;
                    }
                    if ("profile_id".equals(columnName)) {
                        hasProfileId = true;
                    }
                }
                rs.close();
            } catch (SQLException e) {
                logger.error("Error checking tabs table schema", e);
                return;
            }

             if (hasUserId) {
                logger.info("Migrating tabs table from user_id to profile_id schema");
                stmt.execute("ALTER TABLE tabs RENAME TO tabs_old");
                stmt.execute(
                    "CREATE TABLE tabs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "profile_id INTEGER NOT NULL DEFAULT 1, " +
                    "title TEXT NOT NULL, " +
                    "url TEXT NOT NULL, " +
                    "favicon_url TEXT, " +
                    "is_pinned BOOLEAN DEFAULT 0, " +
                    "is_active BOOLEAN DEFAULT 0, " +
                    "position INTEGER DEFAULT 0, " +
                    "session_id TEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (profile_id) REFERENCES profile (id) ON DELETE CASCADE" +
                    ")"
                );

                // Copy data from old table to new table
                // If old table has profile_id, use it; otherwise use user_id as fallback
                String insertSql;
                if (hasProfileId) {
                    // Both columns exist - prefer profile_id
                    insertSql = "INSERT INTO tabs (id, profile_id, title, url, favicon_url, is_pinned, is_active, position, session_id, created_at) " +
                               "SELECT id, profile_id, title, url, favicon_url, is_pinned, is_active, position, session_id, created_at FROM tabs_old";
                } else {
                    // Only user_id exists - map it to profile_id
                    insertSql = "INSERT INTO tabs (id, profile_id, title, url, favicon_url, is_pinned, is_active, position, session_id, created_at) " +
                               "SELECT id, user_id, title, url, favicon_url, is_pinned, is_active, position, session_id, created_at FROM tabs_old";
                }
                stmt.execute(insertSql);

                // Drop old table
                stmt.execute("DROP TABLE tabs_old");

                logger.info("Successfully migrated tabs table to use profile_id (removed user_id)");
            } else if (!hasProfileId) {
                // Table exists but has neither column - add profile_id
                try {
                    stmt.execute("ALTER TABLE tabs ADD COLUMN profile_id INTEGER NOT NULL DEFAULT 1");
                    logger.info("Added profile_id column to tabs table");
                } catch (SQLException e) {
                    logger.debug("Could not add profile_id column: " + e.getMessage());
                }
            } else {
                logger.debug("Tabs table already has correct schema with profile_id");
            }
        }
    }
}
