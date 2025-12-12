package com.example.nexus.repository;


import com.example.nexus.model.Settings;
import com.example.nexus.util.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SettingsRepository extends BaseRepository<Settings> {
    public SettingsRepository(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override
    public java.util.List<Settings> findAll() {
        // This method is not typically used for settings
        // as there's usually only one settings record per user
        return java.util.Collections.emptyList();
    }

    @Override
    public Settings findById(int id) {
        String sql = "SELECT * FROM settings WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSettings(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding settings by ID: " + id, e);
        }

        return null;
    }

    public Settings findByUserId(int userId) {
        String sql = "SELECT * FROM settings WHERE user_id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSettings(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding settings by user ID: " + userId, e);
        }

        return null;
    }

    @Override
    public void save(Settings settings) {
        String sql = "INSERT INTO settings (user_id, theme, accent_color, search_engine, home_page, " +
                "startup_behavior, restore_session, clear_history_on_exit) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, settings.getUserId());
            stmt.setString(2, settings.getTheme());
            stmt.setString(3, settings.getAccentColor());
            stmt.setString(4, settings.getSearchEngine());
            stmt.setString(5, settings.getHomePage());
            stmt.setString(6, settings.getStartupBehavior());
            stmt.setBoolean(7, settings.isRestoreSession());
            stmt.setBoolean(8, settings.isClearHistoryOnExit());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        settings.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving settings", e);
        }
    }

    @Override
    public void update(Settings settings) {
        String sql = "UPDATE settings SET theme = ?, accent_color = ?, search_engine = ?, " +
                "home_page = ?, startup_behavior = ?, restore_session = ?, " +
                "clear_history_on_exit = ? WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, settings.getTheme());
            stmt.setString(2, settings.getAccentColor());
            stmt.setString(3, settings.getSearchEngine());
            stmt.setString(4, settings.getHomePage());
            stmt.setString(5, settings.getStartupBehavior());
            stmt.setBoolean(6, settings.isRestoreSession());
            stmt.setBoolean(7, settings.isClearHistoryOnExit());
            stmt.setInt(8, settings.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating settings", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM settings WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting settings", e);
        }
    }

    private Settings mapResultSetToSettings(ResultSet rs) throws SQLException {
        Settings settings = new Settings();
        settings.setId(rs.getInt("id"));
        settings.setUserId(rs.getInt("user_id"));
        settings.setTheme(rs.getString("theme"));
        settings.setAccentColor(rs.getString("accent_color"));
        settings.setSearchEngine(rs.getString("search_engine"));
        settings.setHomePage(rs.getString("home_page"));
        settings.setStartupBehavior(rs.getString("startup_behavior"));
        settings.setRestoreSession(rs.getBoolean("restore_session"));
        settings.setClearHistoryOnExit(rs.getBoolean("clear_history_on_exit"));
        return settings;
    }
}
