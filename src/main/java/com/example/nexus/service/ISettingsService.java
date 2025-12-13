package com.example.nexus.service;

import com.example.nexus.model.Settings;

import java.util.function.Consumer;

/**
 * Interface for Settings Service operations.
 * Provides methods for managing browser settings.
 */
public interface ISettingsService {

    // ===== CORE =====

    /**
     * Get the current settings object
     */
    Settings getSettings();

    /**
     * Save all current settings
     */
    void saveSettings();

    /**
     * Reset all settings to defaults
     */
    void resetToDefaults();

    /**
     * Add a listener for settings changes
     */
    void addSettingsChangeListener(Consumer<Settings> listener);

    /**
     * Remove a settings change listener
     */
    void removeSettingsChangeListener(Consumer<Settings> listener);

    // ===== APPEARANCE =====

    String getTheme();
    void setTheme(String theme);

    String getAccentColor();
    void setAccentColor(String accentColor);

    int getFontSize();
    void setFontSize(int fontSize);

    double getPageZoom();
    void setPageZoom(double pageZoom);

    boolean isShowBookmarksBar();
    void setShowBookmarksBar(boolean show);

    boolean isShowStatusBar();
    void setShowStatusBar(boolean show);

    boolean isCompactMode();
    void setCompactMode(boolean compact);

    // ===== STARTUP & HOME =====

    String getHomePage();
    void setHomePage(String homePage);

    String getStartupBehavior();
    void setStartupBehavior(String behavior);

    boolean isRestoreSession();
    void setRestoreSession(boolean restore);

    String getNewTabPage();
    void setNewTabPage(String newTabPage);

    // ===== SEARCH =====

    String getSearchEngine();
    void setSearchEngine(String searchEngine);

    String getSearchUrl(String query);

    boolean isShowSearchSuggestions();
    void setShowSearchSuggestions(boolean show);

    // ===== PRIVACY & SECURITY =====

    boolean isClearHistoryOnExit();
    void setClearHistoryOnExit(boolean clear);

    boolean isClearCookiesOnExit();
    void setClearCookiesOnExit(boolean clear);

    boolean isBlockPopups();
    void setBlockPopups(boolean block);

    boolean isDoNotTrack();
    void setDoNotTrack(boolean dnt);

    boolean isSaveBrowsingHistory();
    void setSaveBrowsingHistory(boolean save);

    boolean isSavePasswords();
    void setSavePasswords(boolean save);

    // ===== DOWNLOADS =====

    String getDownloadPath();
    void setDownloadPath(String path);

    boolean isAskDownloadLocation();
    void setAskDownloadLocation(boolean ask);

    // ===== PERFORMANCE =====

    boolean isHardwareAcceleration();
    void setHardwareAcceleration(boolean enabled);

    boolean isSmoothScrolling();
    void setSmoothScrolling(boolean enabled);

    // ===== ACCESSIBILITY =====

    boolean isHighContrast();
    void setHighContrast(boolean enabled);

    boolean isReduceMotion();
    void setReduceMotion(boolean reduce);

    // ===== ADVANCED =====

    boolean isEnableJavaScript();
    void setEnableJavaScript(boolean enabled);

    boolean isDeveloperMode();
    void setDeveloperMode(boolean enabled);

    String getProxyMode();
    void setProxyMode(String mode);
}

