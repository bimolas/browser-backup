package com.example.nexus.service;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Settings;
import com.example.nexus.repository.SettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


public class SettingsService implements ISettingsService {
    private static final Logger logger = LoggerFactory.getLogger(SettingsService.class);

    private final SettingsRepository settingsRepository;
    private volatile Settings currentSettings;
    private final List<Consumer<Settings>> changeListeners = new ArrayList<>();

    public SettingsService(DIContainer container) {
        this.settingsRepository = container.getOrCreate(SettingsRepository.class);
        // Load settings synchronously so startup code can access them immediately
        try {
            loadSettings();
        } catch (Throwable t) {
            logger.error("Failed to load settings during initialization", t);
            // Ensure defaults exist
            currentSettings = new Settings(1);
        }
    }

    private synchronized void loadSettings() {
        // If already loaded by another thread, skip
        if (currentSettings != null) return;

        // Load settings for default user (ID 1)
        try {
            Settings s = settingsRepository.findByUserId(1);
            if (s == null) {
                s = new Settings(1);
                settingsRepository.save(s);
                logger.info("Created default settings");
            }
            currentSettings = s;
            // Clean up duplicates (keep the current one) to avoid multiple settings rows per user
            try {
                settingsRepository.deleteDuplicatesForUser(currentSettings.getUserId(), currentSettings.getId());
            } catch (Exception ex) {
                logger.debug("Failed to cleanup duplicate settings rows", ex);
            }
            logger.info("Loaded settings: {}", currentSettings);
            // Notify listeners that settings are available
            notifyListeners();
        } catch (Exception e) {
            logger.error("Error loading settings from DB", e);
            // Create defaults to keep app functioning
            currentSettings = new Settings(1);
            notifyListeners();
        }
    }

    /**
     * Notify all listeners of settings change
     */
    private void notifyListeners() {
        for (Consumer<Settings> listener : changeListeners) {
            try {
                listener.accept(currentSettings);
            } catch (Exception e) {
                logger.error("Error notifying settings listener", e);
            }
        }
    }

    /**
     * Save settings and notify listeners
     */
    private void saveAndNotify() {
        try {
            // Ensure currentSettings is non-null
            if (currentSettings == null) currentSettings = new Settings(1);

            // Try to find existing settings row for this user
            Settings existing = null;
            try {
                existing = settingsRepository.findByUserId(currentSettings.getUserId());
            } catch (Exception e) {
                logger.debug("Could not query existing settings", e);
            }

            if (existing == null) {
                settingsRepository.save(currentSettings);
                // Ensure ID is set if DB generated one
                if (currentSettings.getId() == 0) {
                    // Try to reload from DB to get ID
                    try {
                        Settings reloaded = settingsRepository.findByUserId(currentSettings.getUserId());
                        if (reloaded != null) currentSettings.setId(reloaded.getId());
                    } catch (Exception e) {
                        logger.debug("Failed to reload settings after insert", e);
                    }
                }
            } else {
                // If existing id differs, ensure model has DB id
                if (currentSettings.getId() == 0) currentSettings.setId(existing.getId());
                settingsRepository.update(currentSettings);
            }
        } catch (Exception e) {
            logger.error("Failed to save settings", e);
        }
        notifyListeners();
    }

    // ==================== CORE ====================

    @Override
    public Settings getSettings() {
        // Return defaults until loaded
        if (currentSettings == null) {
            return new Settings(1);
        }
        return currentSettings;
    }

    @Override
    public void saveSettings() {
        saveAndNotify();
        logger.info("Settings saved");
    }

    @Override
    public void resetToDefaults() {
        currentSettings = new Settings(1);
        saveAndNotify();
        logger.info("Settings reset to defaults");
    }

    @Override
    public void addSettingsChangeListener(Consumer<Settings> listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    @Override
    public void removeSettingsChangeListener(Consumer<Settings> listener) {
        changeListeners.remove(listener);
    }

    // ==================== APPEARANCE ====================

    @Override
    public String getTheme() {
        Settings s = currentSettings;
        // Return canonical theme names: "light", "dark", or "system".
        String t = (s != null && s.getTheme() != null) ? s.getTheme().trim() : "light";
        if (t.equalsIgnoreCase("main")) return "light"; // legacy value
        if (t.equalsIgnoreCase("light") || t.equalsIgnoreCase("dark") || t.equalsIgnoreCase("system")) {
            return t.toLowerCase();
        }
        // Unknown values -> default to light
        return "light";
    }

    @Override
    public void setTheme(String theme) {
        if (currentSettings == null) currentSettings = new Settings(1);
        // Normalize incoming values to canonical names
        if (theme == null) theme = "light";
        theme = theme.trim();
        if (theme.equalsIgnoreCase("main")) theme = "light"; // legacy
        if (!(theme.equalsIgnoreCase("light") || theme.equalsIgnoreCase("dark") || theme.equalsIgnoreCase("system"))) {
            // If unknown, default to light
            theme = "light";
        }
         // Only persist if theme actually changed to avoid loops
         String prev = currentSettings.getTheme();
        if (prev == null) prev = "";
        // Normalize prev to compare properly (handle legacy stored 'main')
        if ("main".equalsIgnoreCase(prev)) prev = "light";
        if (prev.equalsIgnoreCase(theme)) {
            logger.debug("setTheme requested but value unchanged: {}", theme);
            return;
        }
        currentSettings.setTheme(theme);
        saveAndNotify();
        logger.info("Theme changed to canonical value: {} (input was {})", theme, theme);
    }

    @Override
    public String getAccentColor() {
        Settings s = currentSettings;
        return (s != null) ? s.getAccentColor() : "#3b82f6";
    }

    @Override
    public void setAccentColor(String accentColor) {
        if (currentSettings == null) currentSettings = new Settings(1);
        String prev = currentSettings.getAccentColor();
        if (prev == null) prev = "";
        if (accentColor == null) accentColor = "";
        if (prev.equals(accentColor)) {
            logger.debug("setAccentColor requested but value unchanged: {}", accentColor);
            return;
        }
        currentSettings.setAccentColor(accentColor);
        saveAndNotify();
    }

    @Override
    public int getFontSize() {
        Settings s = currentSettings;
        return (s != null) ? s.getFontSize() : 14;
    }

    @Override
    public void setFontSize(int fontSize) {
        if (currentSettings == null) currentSettings = new Settings(1);
        int prev = currentSettings.getFontSize();
        if (prev == fontSize) {
            logger.debug("setFontSize requested but value unchanged: {}", fontSize);
            return;
        }
        currentSettings.setFontSize(fontSize);
        saveAndNotify();
    }

    @Override
    public double getPageZoom() {
        Settings s = currentSettings;
        return (s != null) ? s.getPageZoom() : 1.0;
    }

    @Override
    public void setPageZoom(double pageZoom) {
        if (currentSettings == null) currentSettings = new Settings(1);
        currentSettings.setPageZoom(pageZoom);
        saveAndNotify();
    }

    @Override
    public boolean isShowBookmarksBar() {
        Settings s = currentSettings;
        return s != null && s.isShowBookmarksBar();
    }

    @Override
    public void setShowBookmarksBar(boolean show) {
        if (currentSettings == null) currentSettings = new Settings(1);
        boolean prev = currentSettings.isShowBookmarksBar();
        if (prev == show) {
            logger.debug("setShowBookmarksBar requested but value unchanged: {}", show);
            return;
        }
        currentSettings.setShowBookmarksBar(show);
        saveAndNotify();
    }

    @Override
    public boolean isShowStatusBar() {
        Settings s = currentSettings;
        return s != null && s.isShowStatusBar();
    }

    @Override
    public void setShowStatusBar(boolean show) {
        if (currentSettings == null) currentSettings = new Settings(1);
        boolean prev = currentSettings.isShowStatusBar();
        if (prev == show) {
            logger.debug("setShowStatusBar requested but value unchanged: {}", show);
            return;
        }
        currentSettings.setShowStatusBar(show);
        saveAndNotify();
    }

    @Override
    public boolean isCompactMode() {
        Settings s = currentSettings;
        return s != null && s.isCompactMode();
    }

    @Override
    public void setCompactMode(boolean compact) {
        if (currentSettings == null) currentSettings = new Settings(1);
        boolean prev = currentSettings.isCompactMode();
        if (prev == compact) {
            logger.debug("setCompactMode requested but value unchanged: {}", compact);
            return;
        }
        currentSettings.setCompactMode(compact);
        saveAndNotify();
    }

    // ==================== STARTUP & HOME ====================

    @Override
    public String getHomePage() {
        Settings s = currentSettings;
        return (s != null && s.getHomePage() != null) ? s.getHomePage() : "https://www.google.com";
    }

    @Override
    public void setHomePage(String homePage) {
        if (currentSettings == null) currentSettings = new Settings(1);
        currentSettings.setHomePage(homePage);
        // Persist using unified save logic
        saveAndNotify();
    }

    @Override
    public String getStartupBehavior() {
        return currentSettings.getStartupBehavior();
    }

    @Override
    public void setStartupBehavior(String startupBehavior) {
        currentSettings.setStartupBehavior(startupBehavior);
        saveAndNotify();
    }

    @Override
    public boolean isRestoreSession() {
        return currentSettings.isRestoreSession();
    }

    @Override
    public void setRestoreSession(boolean restoreSession) {
        currentSettings.setRestoreSession(restoreSession);
        saveAndNotify();
    }

    @Override
    public String getNewTabPage() {
        return currentSettings.getNewTabPage();
    }

    @Override
    public void setNewTabPage(String newTabPage) {
        currentSettings.setNewTabPage(newTabPage);
        saveAndNotify();
    }

    // ==================== SEARCH ====================

    @Override
    public String getSearchEngine() {
        Settings s = currentSettings;
        return (s != null && s.getSearchEngine() != null) ? s.getSearchEngine() : "google";
    }

    @Override
    public void setSearchEngine(String searchEngine) {
        if (currentSettings == null) currentSettings = new Settings(1);
        currentSettings.setSearchEngine(searchEngine);
        // Persist using unified save logic
        saveAndNotify();
    }

    @Override
    public String getSearchUrl(String query) {
        return currentSettings.getSearchUrl(query);
    }

    @Override
    public boolean isShowSearchSuggestions() {
        return currentSettings.isShowSearchSuggestions();
    }

    @Override
    public void setShowSearchSuggestions(boolean show) {
        currentSettings.setShowSearchSuggestions(show);
        saveAndNotify();
    }

    // ==================== PRIVACY & SECURITY ====================

    @Override
    public boolean isClearHistoryOnExit() {
        return currentSettings.isClearHistoryOnExit();
    }

    @Override
    public void setClearHistoryOnExit(boolean clearHistoryOnExit) {
        currentSettings.setClearHistoryOnExit(clearHistoryOnExit);
        saveAndNotify();
    }

    @Override
    public boolean isClearCookiesOnExit() {
        return currentSettings.isClearCookiesOnExit();
    }

    @Override
    public void setClearCookiesOnExit(boolean clear) {
        currentSettings.setClearCookiesOnExit(clear);
        saveAndNotify();
    }

    @Override
    public boolean isBlockPopups() {
        return currentSettings.isBlockPopups();
    }

    @Override
    public void setBlockPopups(boolean block) {
        currentSettings.setBlockPopups(block);
        saveAndNotify();
    }

    @Override
    public boolean isDoNotTrack() {
        return currentSettings.isDoNotTrack();
    }

    @Override
    public void setDoNotTrack(boolean dnt) {
        currentSettings.setDoNotTrack(dnt);
        saveAndNotify();
    }

    @Override
    public boolean isSaveBrowsingHistory() {
        return currentSettings.isSaveBrowsingHistory();
    }

    @Override
    public void setSaveBrowsingHistory(boolean save) {
        currentSettings.setSaveBrowsingHistory(save);
        saveAndNotify();
    }

    @Override
    public boolean isSavePasswords() {
        return currentSettings.isSavePasswords();
    }

    @Override
    public void setSavePasswords(boolean save) {
        currentSettings.setSavePasswords(save);
        saveAndNotify();
    }

    // ==================== DOWNLOADS ====================

    @Override
    public String getDownloadPath() {
        return currentSettings.getDownloadPath();
    }

    @Override
    public void setDownloadPath(String path) {
        currentSettings.setDownloadPath(path);
        saveAndNotify();
    }

    @Override
    public boolean isAskDownloadLocation() {
        return currentSettings.isAskDownloadLocation();
    }

    @Override
    public void setAskDownloadLocation(boolean ask) {
        currentSettings.setAskDownloadLocation(ask);
        saveAndNotify();
    }

    // ==================== PERFORMANCE ====================

    @Override
    public boolean isHardwareAcceleration() {
        return currentSettings.isHardwareAcceleration();
    }

    @Override
    public void setHardwareAcceleration(boolean enabled) {
        currentSettings.setHardwareAcceleration(enabled);
        saveAndNotify();
    }

    @Override
    public boolean isSmoothScrolling() {
        return currentSettings.isSmoothScrolling();
    }

    @Override
    public void setSmoothScrolling(boolean enabled) {
        currentSettings.setSmoothScrolling(enabled);
        saveAndNotify();
    }

    // ==================== ACCESSIBILITY ====================

    @Override
    public boolean isHighContrast() {
        return currentSettings.isHighContrast();
    }

    @Override
    public void setHighContrast(boolean enabled) {
        currentSettings.setHighContrast(enabled);
        saveAndNotify();
    }

    @Override
    public boolean isReduceMotion() {
        return currentSettings.isReduceMotion();
    }

    @Override
    public void setReduceMotion(boolean reduce) {
        currentSettings.setReduceMotion(reduce);
        saveAndNotify();
    }

    // ==================== ADVANCED ====================

    @Override
    public boolean isEnableJavaScript() {
        return currentSettings.isEnableJavaScript();
    }

    @Override
    public void setEnableJavaScript(boolean enabled) {
        currentSettings.setEnableJavaScript(enabled);
        saveAndNotify();
    }

    @Override
    public boolean isDeveloperMode() {
        return currentSettings.isDeveloperMode();
    }

    @Override
    public void setDeveloperMode(boolean enabled) {
        currentSettings.setDeveloperMode(enabled);
        saveAndNotify();
    }

    @Override
    public String getProxyMode() {
        return currentSettings.getProxyMode();
    }

    @Override
    public void setProxyMode(String mode) {
        currentSettings.setProxyMode(mode);
        saveAndNotify();
    }
}

