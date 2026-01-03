package com.example.nexus.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.concurrent.*;

public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    private static final long MAX_MIGRATION_FILE_SIZE = 50L * 1024L * 1024L;

    private static final long MIGRATION_TIMEOUT_SECONDS = 30L;

    public static void initialize(DatabaseManager dbManager) {

        try (Connection conn = dbManager.getConnection()) {
            if (conn == null) {
                logger.error("DatabaseInitializer: could not obtain connection");
                return;
            }

            initializeSchema(conn);

            DatabaseMigration.migrate(conn);

            logger.info("DatabaseInitializer: schema initialization completed (fast path)");
        } catch (Exception e) {
            logger.error("DatabaseInitializer failed during schema init", e);
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "nexus-db-migration-scheduler");
            t.setDaemon(true);
            return t;
        });

        scheduler.submit(() -> {

            ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "nexus-db-migration-worker");
                t.setDaemon(true);
                return t;
            });

            Future<?> future = worker.submit(() -> {
                try (Connection conn = dbManager.getConnection()) {
                    if (conn == null) {
                        logger.warn("DatabaseInitializer: migration skipped because connection could not be obtained");
                        return;
                    }
                    migrateFromProjectDbIfNeeded(conn);
                } catch (Exception e) {
                    logger.error("DatabaseInitializer: migration task failed", e);
                }
            });

            try {
                future.get(MIGRATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException te) {
                logger.warn("DatabaseInitializer: migration timed out after {} seconds and will be cancelled", MIGRATION_TIMEOUT_SECONDS);
                future.cancel(true);
            } catch (InterruptedException ie) {
                logger.warn("DatabaseInitializer: migration interrupted", ie);
                Thread.currentThread().interrupt();
            } catch (ExecutionException ee) {
                logger.error("DatabaseInitializer: migration execution failed", ee.getCause());
            } finally {
                worker.shutdownNow();
            }
        });

        scheduler.shutdown();
    }

    private static void migrateFromProjectDbIfNeeded(Connection conn) {
        try {
            String projectPath = "identifier.sqlite";
            java.io.File projectFile = new java.io.File(projectPath);
            if (!projectFile.exists()) {
                logger.debug("No project-local DB found at {}", projectFile.getAbsolutePath());
                return;
            }

            long fileSize = projectFile.length();
            if (fileSize > MAX_MIGRATION_FILE_SIZE) {
                logger.warn("Project DB too large for automatic migration ({} bytes). Skipping migration.", fileSize);
                return;
            }

            String currentJdbc = null;
            try {
                currentJdbc = DatabaseConnection.getInstance().getJdbcUrl();
            } catch (Throwable t) {
                logger.debug("Could not obtain current JDBC URL from DatabaseConnection", t);
            }

            if (currentJdbc != null && currentJdbc.contains(projectFile.getAbsolutePath())) {
                logger.debug("Current DB already uses project-local DB {}", projectFile.getAbsolutePath());
                return;
            }

            try (java.sql.Statement s = conn.createStatement()) {

                try {
                    s.execute("PRAGMA busy_timeout = 1000");
                } catch (Exception ignore) {

                }

                String pathEscaped = projectFile.getAbsolutePath().replace("'", "''");
                String attach = "ATTACH '" + pathEscaped + "' AS olddb";

                logger.info("Attempting to attach project DB for migration: {}", projectFile.getAbsolutePath());
                s.execute(attach);

                java.util.List<String> targetCols = pragmaTableColumns(conn, "settings");
                java.util.List<String> sourceCols = pragmaTableColumns(conn, "olddb.settings");

                if (targetCols == null || targetCols.isEmpty()) {
                    logger.warn("Migration: target settings table has no columns, skipping copy");
                } else if (sourceCols == null || sourceCols.isEmpty()) {
                    logger.warn("Migration: source settings table in attached DB has no columns, skipping copy");
                } else {
                    StringBuilder insertCols = new StringBuilder();
                    StringBuilder selectExpr = new StringBuilder();
                    for (int i = 0; i < targetCols.size(); i++) {
                        String col = targetCols.get(i);
                        if (i > 0) { insertCols.append(", "); selectExpr.append(", "); }
                        insertCols.append(col);

                        if (sourceCols.contains(col)) {
                            selectExpr.append("olddb.settings.").append(col);
                        } else {
                            selectExpr.append("NULL AS ").append(col);
                        }
                    }

                    String copy = "INSERT OR REPLACE INTO settings (" + insertCols.toString() + ") " +
                            "SELECT " + selectExpr.toString() + " FROM olddb.settings WHERE user_id = 1 ORDER BY id DESC LIMIT 1";

                    int copied = s.executeUpdate(copy);
                    logger.info("Migration: copied {} rows from project DB into user DB", copied);
                }

                try {
                    boolean hasTheme = tableHasColumn(conn, "settings", "theme");
                    boolean hasDarkModeFlag = tableHasColumn(conn, "settings", "dark_mode")
                            || tableHasColumn(conn, "settings", "enable_dark_mode")
                            || tableHasColumn(conn, "settings", "use_dark_theme");
                    boolean hasHighContrast = tableHasColumn(conn, "settings", "high_contrast");

                    if (hasTheme) {
                        try (java.sql.Statement norm = conn.createStatement()) {
                            norm.executeUpdate("UPDATE settings SET theme = LOWER(COALESCE(theme, ''))");
                            norm.executeUpdate("UPDATE settings SET theme = CASE WHEN theme LIKE '%dark%' THEN 'dark' WHEN theme LIKE '%light%' OR theme = '' THEN 'main' ELSE theme END");

                            if (hasDarkModeFlag) {
                                if (tableHasColumn(conn, "settings", "dark_mode")) {
                                    norm.executeUpdate("UPDATE settings SET dark_mode = CASE WHEN theme='dark' THEN 1 ELSE 0 END");
                                } else if (tableHasColumn(conn, "settings", "enable_dark_mode")) {
                                    norm.executeUpdate("UPDATE settings SET enable_dark_mode = CASE WHEN theme='dark' THEN 1 ELSE 0 END");
                                } else if (tableHasColumn(conn, "settings", "use_dark_theme")) {
                                    norm.executeUpdate("UPDATE settings SET use_dark_theme = CASE WHEN theme='dark' THEN 1 ELSE 0 END");
                                }
                            }

                            if (hasHighContrast) {
                                norm.executeUpdate("UPDATE settings SET high_contrast = CASE WHEN theme='dark' THEN 1 ELSE high_contrast END");
                            }

                            logger.info("Normalized theme/dark-mode settings after migration");
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Theme normalization skipped or failed", e);
                }

                s.execute("DETACH olddb");
            }
        } catch (Exception e) {
            logger.error("Error during project DB migration", e);
        }
    }

    private static boolean tableHasColumn(Connection conn, String tableName, String columnName) {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, columnName)) {
            return rs != null && rs.next();
        } catch (Exception e) {
            logger.debug("Could not determine if table {} has column {}", tableName, columnName, e);
            return false;
        }
    }

    private static java.util.List<String> pragmaTableColumns(Connection conn, String tableName) {
        java.util.List<String> cols = new java.util.ArrayList<>();
        java.sql.Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            String pragmaSql;

            if (tableName.contains(".")) {
                String[] parts = tableName.split("\\.", 2);
                String schema = parts[0];
                String tbl = parts[1];

                pragmaSql = "PRAGMA " + schema + ".table_info('" + tbl + "')";
            } else {
                pragmaSql = "PRAGMA table_info('" + tableName + "')";
            }
            try {
                rs = stmt.executeQuery(pragmaSql);
            } catch (Exception e) {

                if (!tableName.contains(".")) throw e;
                rs = stmt.executeQuery("PRAGMA table_info('" + tableName + "')");
            }

            while (rs.next()) {
                String name = rs.getString("name");
                if (name != null) cols.add(name);
            }
        } catch (Exception e) {
            logger.debug("Could not read PRAGMA table_info for {}", tableName, e);
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignore) {}
            try { if (stmt != null) stmt.close(); } catch (Exception ignore) {}
        }
        return cols;
    }

    private static void initializeSchema(Connection conn) {
        try (InputStream is = DatabaseInitializer.class.getResourceAsStream("/com/example/nexus/db/init.sql")) {
            if (is == null) {
                logger.error("Database initialization script not found at /com/example/nexus/db/init.sql");
                try (InputStream altIs = DatabaseInitializer.class.getClassLoader().getResourceAsStream("com/example/nexus/db/init.sql")) {
                    if (altIs == null) {
                        logger.error("Database initialization script not found at alternate path either");
                        return;
                    }
                    executeInitScript(conn, altIs);
                    return;
                } catch (Exception e) {
                    logger.error("Failed to run alternate init script", e);
                    return;
                }
            }

            executeInitScript(conn, is);
        } catch (Exception e) {
            logger.error("Failed to initialize database schema", e);
        }
    }

    private static void executeInitScript(Connection conn, InputStream is) throws Exception {
        String sql = new String(is.readAllBytes());

        try (java.sql.Statement stmt = conn.createStatement()) {

            for (String statement : sql.split(";")) {
                if (!statement.trim().isEmpty()) {
                    stmt.execute(statement);
                }
            }

            logger.info("Database schema initialized");
        }
    }
}
