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

        String sql = "SELECT * FROM settings WHERE user_id = ? ORDER BY id DESC LIMIT 1";

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

    public void deleteDuplicatesForUser(int userId, int keepId) {
        try {
            String findSql = "SELECT id FROM settings WHERE user_id = ? ORDER BY id DESC";
            try (PreparedStatement findStmt = getConnection().prepareStatement(findSql)) {
                findStmt.setInt(1, userId);
                try (ResultSet rs = findStmt.executeQuery()) {
                    java.util.List<Integer> ids = new java.util.ArrayList<>();
                    while (rs.next()) {
                        ids.add(rs.getInt("id"));
                    }
                    if (ids.size() <= 1) return;

                    int keep = keepId > 0 ? keepId : ids.get(0);

                    StringBuilder sb = new StringBuilder("DELETE FROM settings WHERE user_id = ? AND id IN (");
                    for (int i = 0; i < ids.size(); i++) {
                        if (ids.get(i) == keep) continue;
                        if (sb.charAt(sb.length()-1) != '(') sb.append(',');
                        sb.append('?');
                    }
                    sb.append(')');

                    try (PreparedStatement del = getConnection().prepareStatement(sb.toString())) {
                        del.setInt(1, userId);
                        int param = 2;
                        for (Integer id : ids) {
                            if (id == keep) continue;
                            del.setInt(param++, id);
                        }
                        int removed = del.executeUpdate();
                        logger.info("Deleted {} duplicate settings rows for user {} (kept id={})", removed, userId, keep);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error cleaning duplicate settings for user " + userId, e);
        }
    }

    @Override
    public void save(Settings s) {

        try {
            Settings existing = findByUserId(s.getUserId());
            if (existing != null) {

                s.setId(existing.getId());
                update(s);
                return;
            }
        } catch (Exception e) {
            logger.debug("Could not check existing settings before insert", e);
        }

        String sql = """
            INSERT INTO settings (
                user_id, theme, accent_color, font_size, page_zoom, show_bookmarks_bar, show_status_bar, compact_mode,
                home_page, startup_behavior, restore_session, new_tab_page, custom_new_tab_url,
                search_engine, custom_search_url, show_search_suggestions, search_in_address_bar,
                clear_history_on_exit, clear_cookies_on_exit, clear_cache_on_exit, block_popups, do_not_track,
                block_third_party_cookies, https_only_mode, save_browsing_history, save_form_data, save_passwords,
                download_path, ask_download_location, open_pdf_in_browser, show_download_notification,
                hardware_acceleration, smooth_scrolling, preload_pages, lazy_load_images, max_tabs_in_memory,
                high_contrast, reduce_motion, force_zoom, default_encoding,
                enable_javascript, enable_images, enable_webgl, developer_mode,
                proxy_mode, proxy_host, proxy_port, user_agent,
                enable_notifications, sound_enabled
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = getConnection().prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            stmt.setInt(i++, s.getUserId());

            stmt.setString(i++, s.getTheme());
            stmt.setString(i++, s.getAccentColor());
            stmt.setInt(i++, s.getFontSize());
            stmt.setDouble(i++, s.getPageZoom());
            stmt.setBoolean(i++, s.isShowBookmarksBar());
            stmt.setBoolean(i++, s.isShowStatusBar());
            stmt.setBoolean(i++, s.isCompactMode());

            stmt.setString(i++, s.getHomePage());
            stmt.setString(i++, s.getStartupBehavior());
            stmt.setBoolean(i++, s.isRestoreSession());
            stmt.setString(i++, s.getNewTabPage());
            stmt.setString(i++, s.getCustomNewTabUrl());

            stmt.setString(i++, s.getSearchEngine());
            stmt.setString(i++, s.getCustomSearchUrl());
            stmt.setBoolean(i++, s.isShowSearchSuggestions());
            stmt.setBoolean(i++, s.isSearchInAddressBar());

            stmt.setBoolean(i++, s.isClearHistoryOnExit());
            stmt.setBoolean(i++, s.isClearCookiesOnExit());
            stmt.setBoolean(i++, s.isClearCacheOnExit());
            stmt.setBoolean(i++, s.isBlockPopups());
            stmt.setBoolean(i++, s.isDoNotTrack());
            stmt.setBoolean(i++, s.isBlockThirdPartyCookies());
            stmt.setBoolean(i++, s.isHttpsOnlyMode());
            stmt.setBoolean(i++, s.isSaveBrowsingHistory());
            stmt.setBoolean(i++, s.isSaveFormData());
            stmt.setBoolean(i++, s.isSavePasswords());

            stmt.setString(i++, s.getDownloadPath());
            stmt.setBoolean(i++, s.isAskDownloadLocation());
            stmt.setBoolean(i++, s.isOpenPdfInBrowser());
            stmt.setBoolean(i++, s.isShowDownloadNotification());

            stmt.setBoolean(i++, s.isHardwareAcceleration());
            stmt.setBoolean(i++, s.isSmoothScrolling());
            stmt.setBoolean(i++, s.isPreloadPages());
            stmt.setBoolean(i++, s.isLazyLoadImages());
            stmt.setInt(i++, s.getMaxTabsInMemory());

            stmt.setBoolean(i++, s.isHighContrast());
            stmt.setBoolean(i++, s.isReduceMotion());
            stmt.setBoolean(i++, s.isForceZoom());
            stmt.setString(i++, s.getDefaultEncoding());

            stmt.setBoolean(i++, s.isEnableJavaScript());
            stmt.setBoolean(i++, s.isEnableImages());
            stmt.setBoolean(i++, s.isEnableWebGL());
            stmt.setBoolean(i++, s.isDeveloperMode());
            stmt.setString(i++, s.getProxyMode());
            stmt.setString(i++, s.getProxyHost());
            stmt.setInt(i++, s.getProxyPort());
            stmt.setString(i++, s.getUserAgent());

            stmt.setBoolean(i++, s.isEnableNotifications());
            stmt.setBoolean(i++, s.isSoundEnabled());

            int affectedRows = stmt.executeUpdate();

            logger.debug("SettingsRepository.save() executed, theme={}, home={}, search={}, affectedRows={}", s.getTheme(), s.getHomePage(), s.getSearchEngine(), affectedRows);

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        s.setId(generatedKeys.getInt(1));
                    }
                }
            }
            logger.info("Saved settings with ID: " + s.getId());
        } catch (SQLException e) {
            logger.error("Error saving settings", e);
        }
    }

    @Override
    public void update(Settings s) {
        String sql = """
            UPDATE settings SET
                theme = ?, accent_color = ?, font_size = ?, page_zoom = ?, show_bookmarks_bar = ?, show_status_bar = ?, compact_mode = ?,
                home_page = ?, startup_behavior = ?, restore_session = ?, new_tab_page = ?, custom_new_tab_url = ?,
                search_engine = ?, custom_search_url = ?, show_search_suggestions = ?, search_in_address_bar = ?,
                clear_history_on_exit = ?, clear_cookies_on_exit = ?, clear_cache_on_exit = ?, block_popups = ?, do_not_track = ?,
                block_third_party_cookies = ?, https_only_mode = ?, save_browsing_history = ?, save_form_data = ?, save_passwords = ?,
                download_path = ?, ask_download_location = ?, open_pdf_in_browser = ?, show_download_notification = ?,
                hardware_acceleration = ?, smooth_scrolling = ?, preload_pages = ?, lazy_load_images = ?, max_tabs_in_memory = ?,
                high_contrast = ?, reduce_motion = ?, force_zoom = ?, default_encoding = ?,
                enable_javascript = ?, enable_images = ?, enable_webgl = ?, developer_mode = ?,
                proxy_mode = ?, proxy_host = ?, proxy_port = ?, user_agent = ?,
                enable_notifications = ?, sound_enabled = ?
            WHERE id = ?
            """;

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            int i = 1;

            stmt.setString(i++, s.getTheme());
            stmt.setString(i++, s.getAccentColor());
            stmt.setInt(i++, s.getFontSize());
            stmt.setDouble(i++, s.getPageZoom());
            stmt.setBoolean(i++, s.isShowBookmarksBar());
            stmt.setBoolean(i++, s.isShowStatusBar());
            stmt.setBoolean(i++, s.isCompactMode());

            stmt.setString(i++, s.getHomePage());
            stmt.setString(i++, s.getStartupBehavior());
            stmt.setBoolean(i++, s.isRestoreSession());
            stmt.setString(i++, s.getNewTabPage());
            stmt.setString(i++, s.getCustomNewTabUrl());

            stmt.setString(i++, s.getSearchEngine());
            stmt.setString(i++, s.getCustomSearchUrl());
            stmt.setBoolean(i++, s.isShowSearchSuggestions());
            stmt.setBoolean(i++, s.isSearchInAddressBar());

            stmt.setBoolean(i++, s.isClearHistoryOnExit());
            stmt.setBoolean(i++, s.isClearCookiesOnExit());
            stmt.setBoolean(i++, s.isClearCacheOnExit());
            stmt.setBoolean(i++, s.isBlockPopups());
            stmt.setBoolean(i++, s.isDoNotTrack());
            stmt.setBoolean(i++, s.isBlockThirdPartyCookies());
            stmt.setBoolean(i++, s.isHttpsOnlyMode());
            stmt.setBoolean(i++, s.isSaveBrowsingHistory());
            stmt.setBoolean(i++, s.isSaveFormData());
            stmt.setBoolean(i++, s.isSavePasswords());

            stmt.setString(i++, s.getDownloadPath());
            stmt.setBoolean(i++, s.isAskDownloadLocation());
            stmt.setBoolean(i++, s.isOpenPdfInBrowser());
            stmt.setBoolean(i++, s.isShowDownloadNotification());

            stmt.setBoolean(i++, s.isHardwareAcceleration());
            stmt.setBoolean(i++, s.isSmoothScrolling());
            stmt.setBoolean(i++, s.isPreloadPages());
            stmt.setBoolean(i++, s.isLazyLoadImages());
            stmt.setInt(i++, s.getMaxTabsInMemory());

            stmt.setBoolean(i++, s.isHighContrast());
            stmt.setBoolean(i++, s.isReduceMotion());
            stmt.setBoolean(i++, s.isForceZoom());
            stmt.setString(i++, s.getDefaultEncoding());

            stmt.setBoolean(i++, s.isEnableJavaScript());
            stmt.setBoolean(i++, s.isEnableImages());
            stmt.setBoolean(i++, s.isEnableWebGL());
            stmt.setBoolean(i++, s.isDeveloperMode());
            stmt.setString(i++, s.getProxyMode());
            stmt.setString(i++, s.getProxyHost());
            stmt.setInt(i++, s.getProxyPort());
            stmt.setString(i++, s.getUserAgent());

            stmt.setBoolean(i++, s.isEnableNotifications());
            stmt.setBoolean(i++, s.isSoundEnabled());

            stmt.setInt(i++, s.getId());

            int affected = stmt.executeUpdate();
            logger.debug("SettingsRepository.update() executed, theme={}, home={}, search={}, affectedRows={}", s.getTheme(), s.getHomePage(), s.getSearchEngine(), affected);
            logger.info("Updated settings with ID: {} (affectedRows={})", s.getId(), affected);
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
        Settings s = new Settings();
        s.setId(rs.getInt("id"));
        s.setUserId(rs.getInt("user_id"));

        s.setTheme(getStringOrDefault(rs, "theme", "light"));
        s.setAccentColor(getStringOrDefault(rs, "accent_color", "#3b82f6"));
        s.setFontSize(getIntOrDefault(rs, "font_size", 14));
        s.setPageZoom(getDoubleOrDefault(rs, "page_zoom", 1.0));
        s.setShowBookmarksBar(getBoolOrDefault(rs, "show_bookmarks_bar", true));
        s.setShowStatusBar(getBoolOrDefault(rs, "show_status_bar", true));
        s.setCompactMode(getBoolOrDefault(rs, "compact_mode", false));

        s.setHomePage(getStringOrDefault(rs, "home_page", "https://www.google.com"));
        s.setStartupBehavior(getStringOrDefault(rs, "startup_behavior", "show_home"));
        s.setRestoreSession(getBoolOrDefault(rs, "restore_session", true));
        s.setNewTabPage(getStringOrDefault(rs, "new_tab_page", "new_tab"));
        s.setCustomNewTabUrl(getStringOrDefault(rs, "custom_new_tab_url", ""));

        s.setSearchEngine(getStringOrDefault(rs, "search_engine", "google"));
        s.setCustomSearchUrl(getStringOrDefault(rs, "custom_search_url", ""));
        s.setShowSearchSuggestions(getBoolOrDefault(rs, "show_search_suggestions", true));
        s.setSearchInAddressBar(getBoolOrDefault(rs, "search_in_address_bar", true));

        s.setClearHistoryOnExit(getBoolOrDefault(rs, "clear_history_on_exit", false));
        s.setClearCookiesOnExit(getBoolOrDefault(rs, "clear_cookies_on_exit", false));
        s.setClearCacheOnExit(getBoolOrDefault(rs, "clear_cache_on_exit", false));
        s.setBlockPopups(getBoolOrDefault(rs, "block_popups", true));
        s.setDoNotTrack(getBoolOrDefault(rs, "do_not_track", true));
        s.setBlockThirdPartyCookies(getBoolOrDefault(rs, "block_third_party_cookies", false));
        s.setHttpsOnlyMode(getBoolOrDefault(rs, "https_only_mode", false));
        s.setSaveBrowsingHistory(getBoolOrDefault(rs, "save_browsing_history", true));
        s.setSaveFormData(getBoolOrDefault(rs, "save_form_data", true));
        s.setSavePasswords(getBoolOrDefault(rs, "save_passwords", true));

        s.setDownloadPath(getStringOrDefault(rs, "download_path", System.getProperty("user.home") + "/Downloads"));
        s.setAskDownloadLocation(getBoolOrDefault(rs, "ask_download_location", false));
        s.setOpenPdfInBrowser(getBoolOrDefault(rs, "open_pdf_in_browser", true));
        s.setShowDownloadNotification(getBoolOrDefault(rs, "show_download_notification", true));

        s.setHardwareAcceleration(getBoolOrDefault(rs, "hardware_acceleration", true));
        s.setSmoothScrolling(getBoolOrDefault(rs, "smooth_scrolling", true));
        s.setPreloadPages(getBoolOrDefault(rs, "preload_pages", true));
        s.setLazyLoadImages(getBoolOrDefault(rs, "lazy_load_images", true));
        s.setMaxTabsInMemory(getIntOrDefault(rs, "max_tabs_in_memory", 0));

        s.setHighContrast(getBoolOrDefault(rs, "high_contrast", false));
        s.setReduceMotion(getBoolOrDefault(rs, "reduce_motion", false));
        s.setForceZoom(getBoolOrDefault(rs, "force_zoom", false));
        s.setDefaultEncoding(getStringOrDefault(rs, "default_encoding", "UTF-8"));

        s.setEnableJavaScript(getBoolOrDefault(rs, "enable_javascript", true));
        s.setEnableImages(getBoolOrDefault(rs, "enable_images", true));
        s.setEnableWebGL(getBoolOrDefault(rs, "enable_webgl", true));
        s.setDeveloperMode(getBoolOrDefault(rs, "developer_mode", false));
        s.setProxyMode(getStringOrDefault(rs, "proxy_mode", "system"));
        s.setProxyHost(getStringOrDefault(rs, "proxy_host", ""));
        s.setProxyPort(getIntOrDefault(rs, "proxy_port", 8080));
        s.setUserAgent(getStringOrDefault(rs, "user_agent", ""));

        s.setEnableNotifications(getBoolOrDefault(rs, "enable_notifications", true));
        s.setSoundEnabled(getBoolOrDefault(rs, "sound_enabled", true));

        return s;
    }

    private String getStringOrDefault(ResultSet rs, String column, String defaultValue) {
        try {
            String value = rs.getString(column);
            return value != null ? value : defaultValue;
        } catch (SQLException e) {
            return defaultValue;
        }
    }

    private int getIntOrDefault(ResultSet rs, String column, int defaultValue) {
        try {
            return rs.getInt(column);
        } catch (SQLException e) {
            return defaultValue;
        }
    }

    private double getDoubleOrDefault(ResultSet rs, String column, double defaultValue) {
        try {
            return rs.getDouble(column);
        } catch (SQLException e) {
            return defaultValue;
        }
    }

    private boolean getBoolOrDefault(ResultSet rs, String column, boolean defaultValue) {
        try {
            return rs.getBoolean(column);
        } catch (SQLException e) {
            return defaultValue;
        }
    }
}
