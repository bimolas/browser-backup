package com.example.nexus.service;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Profile;
import com.example.nexus.model.Settings;
import com.example.nexus.repository.SettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SettingsService {
    private static final Logger logger = LoggerFactory.getLogger(SettingsService.class);

    private final SettingsRepository settingsRepository;
    private final DIContainer container;
    private volatile Settings currentSettings;
    private final List<Consumer<Settings>> changeListeners = new ArrayList<>();
    private ProfileService profileService;

    public SettingsService(DIContainer container) {
        this.settingsRepository = container.getOrCreate(SettingsRepository.class);
        this.container = container;

        try {
            // Get ProfileService and listen for profile changes
            this.profileService = container.getOrCreate(ProfileService.class);
            this.profileService.addProfileChangeListener(profile -> {
                logger.info("Profile changed to: {}, reloading settings", profile.getUsername());
                reloadSettingsForCurrentProfile();
            });

            loadSettings();
        } catch (Throwable t) {
            logger.error("Failed to load settings during initialization", t);
            currentSettings = new Settings(1);
        }
    }

    private synchronized void loadSettings() {
        if (currentSettings != null) return;

        try {
            // Get current profile
            Profile currentProfile = profileService.getCurrentProfile();
            int userId = (currentProfile != null && currentProfile.getId() > 0)
                ? currentProfile.getId()
                : 1;

            Settings s = settingsRepository.findByUserId(userId);
            if (s == null) {
                s = new Settings(userId);
                settingsRepository.save(s);
                logger.info("Created default settings for user {}", userId);
            }
            currentSettings = s;

            try {
                settingsRepository.deleteDuplicatesForUser(currentSettings.getUserId(), currentSettings.getId());
            } catch (Exception ex) {
                logger.debug("Failed to cleanup duplicate settings rows", ex);
            }
            logger.info("Loaded settings: {}", currentSettings);

            notifyListeners();
        } catch (Exception e) {
            logger.error("Error loading settings from DB", e);
            currentSettings = new Settings(1);
            notifyListeners();
        }
    }

    /**
     * Reload settings when profile changes
     */
    private synchronized void reloadSettingsForCurrentProfile() {
        try {
            Profile currentProfile = profileService.getCurrentProfile();
            int userId = (currentProfile != null && currentProfile.getId() > 0)
                ? currentProfile.getId()
                : 1;

            // Only reload if switching to a different user
            if (currentSettings != null && currentSettings.getUserId() == userId) {
                logger.debug("Settings already loaded for user {}", userId);
                return;
            }

            Settings s = settingsRepository.findByUserId(userId);
            if (s == null) {
                s = new Settings(userId);
                settingsRepository.save(s);
                logger.info("Created default settings for user {}", userId);
            }

            currentSettings = s;
            logger.info("Reloaded settings for user {}: {}", userId, currentSettings);

            notifyListeners();
        } catch (Exception e) {
            logger.error("Error reloading settings for current profile", e);
        }
    }

    private void notifyListeners() {
        for (Consumer<Settings> listener : changeListeners) {
            try {
                listener.accept(currentSettings);
            } catch (Exception e) {
                logger.error("Error notifying settings listener", e);
            }
        }
    }

    private void saveAndNotify() {
        try {

            if (currentSettings == null) currentSettings = new Settings(1);

            Settings existing = null;
            try {
                existing = settingsRepository.findByUserId(currentSettings.getUserId());
            } catch (Exception e) {
                logger.debug("Could not query existing settings", e);
            }

            if (existing == null) {
                settingsRepository.save(currentSettings);

                if (currentSettings.getId() == 0) {

                    try {
                        Settings reloaded = settingsRepository.findByUserId(currentSettings.getUserId());
                        if (reloaded != null) currentSettings.setId(reloaded.getId());
                    } catch (Exception e) {
                        logger.debug("Failed to reload settings after insert", e);
                    }
                }
            } else {

                if (currentSettings.getId() == 0) currentSettings.setId(existing.getId());
                settingsRepository.update(currentSettings);
            }
        } catch (Exception e) {
            logger.error("Failed to save settings", e);
        }
        notifyListeners();
    }

    public Settings getSettings() {

        if (currentSettings == null) {
            return new Settings(1);
        }
        return currentSettings;
    }

    public void saveSettings() {
        saveAndNotify();
        logger.info("Settings saved");
    }

    public void resetToDefaults() {
        currentSettings = new Settings(1);
        saveAndNotify();
        logger.info("Settings reset to defaults");
    }

    public void addSettingsChangeListener(Consumer<Settings> listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeSettingsChangeListener(Consumer<Settings> listener) {
        changeListeners.remove(listener);
    }

    public String getTheme() {
        Settings s = currentSettings;

        String t = (s != null && s.getTheme() != null) ? s.getTheme().trim() : "light";
        if (t.equalsIgnoreCase("main")) return "light";
        if (t.equalsIgnoreCase("light") || t.equalsIgnoreCase("dark") || t.equalsIgnoreCase("system")) {
            return t.toLowerCase();
        }

        return "light";
    }

    public void setTheme(String theme) {
        if (currentSettings == null) currentSettings = new Settings(1);

        if (theme == null) theme = "light";
        theme = theme.trim();
        if (theme.equalsIgnoreCase("main")) theme = "light";
        if (!(theme.equalsIgnoreCase("light") || theme.equalsIgnoreCase("dark") || theme.equalsIgnoreCase("system"))) {

            theme = "light";
        }

         String prev = currentSettings.getTheme();
        if (prev == null) prev = "";

        if ("main".equalsIgnoreCase(prev)) prev = "light";
        if (prev.equalsIgnoreCase(theme)) {
            logger.debug("setTheme requested but value unchanged: {}", theme);
            return;
        }
        currentSettings.setTheme(theme);
        saveAndNotify();
        logger.info("Theme changed to canonical value: {} (input was {})", theme, theme);
    }

    public String getAccentColor() {
        Settings s = currentSettings;
        return (s != null) ? s.getAccentColor() : "#3b82f6";
    }

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

    public int getFontSize() {
        Settings s = currentSettings;
        return (s != null) ? s.getFontSize() : 14;
    }

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

    public double getPageZoom() {
        Settings s = currentSettings;
        return (s != null) ? s.getPageZoom() : 1.0;
    }

    public void setPageZoom(double pageZoom) {
        if (currentSettings == null) currentSettings = new Settings(1);
        currentSettings.setPageZoom(pageZoom);
        saveAndNotify();
    }

    public boolean isShowBookmarksBar() {
        Settings s = currentSettings;
        return s != null && s.isShowBookmarksBar();
    }

    public boolean isShowStatusBar() {
        Settings s = currentSettings;
        return s != null && s.isShowStatusBar();
    }

    public boolean isCompactMode() {
        Settings s = currentSettings;
        return s != null && s.isCompactMode();
    }

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

    public String getHomePage() {
        Settings s = currentSettings;
        return (s != null && s.getHomePage() != null) ? s.getHomePage() : "https://www.google.com";
    }

    public void setHomePage(String homePage) {
        if (currentSettings == null) currentSettings = new Settings(1);
        currentSettings.setHomePage(homePage);

        saveAndNotify();
    }

    public String getStartupBehavior() {
        return currentSettings.getStartupBehavior();
    }

    public void setStartupBehavior(String startupBehavior) {
        currentSettings.setStartupBehavior(startupBehavior);
        saveAndNotify();
    }

    public boolean isRestoreSession() {
        return currentSettings.isRestoreSession();
    }

    public void setRestoreSession(boolean restoreSession) {
        currentSettings.setRestoreSession(restoreSession);
        saveAndNotify();
    }

    public String getNewTabPage() {
        return currentSettings.getNewTabPage();
    }

    public void setNewTabPage(String newTabPage) {
        currentSettings.setNewTabPage(newTabPage);
        saveAndNotify();
    }

    public String getSearchEngine() {
        Settings s = currentSettings;
        return (s != null && s.getSearchEngine() != null) ? s.getSearchEngine() : "google";
    }

    public void setSearchEngine(String searchEngine) {
        if (currentSettings == null) currentSettings = new Settings(1);
        currentSettings.setSearchEngine(searchEngine);

        saveAndNotify();
    }

    public String getSearchUrl(String query) {
        return currentSettings.getSearchUrl(query);
    }

    public boolean isShowSearchSuggestions() {
        return currentSettings.isShowSearchSuggestions();
    }

    public void setShowSearchSuggestions(boolean show) {
        currentSettings.setShowSearchSuggestions(show);
        saveAndNotify();
    }

    public boolean isClearHistoryOnExit() {
        return currentSettings.isClearHistoryOnExit();
    }

    public void setClearHistoryOnExit(boolean clearHistoryOnExit) {
        currentSettings.setClearHistoryOnExit(clearHistoryOnExit);
        saveAndNotify();
    }

    public boolean isClearCookiesOnExit() {
        return currentSettings.isClearCookiesOnExit();
    }

    public void setClearCookiesOnExit(boolean clear) {
        currentSettings.setClearCookiesOnExit(clear);
        saveAndNotify();
    }

    public boolean isBlockPopups() {
        return currentSettings.isBlockPopups();
    }

    public void setBlockPopups(boolean block) {
        currentSettings.setBlockPopups(block);
        saveAndNotify();
    }

    public boolean isDoNotTrack() {
        return currentSettings.isDoNotTrack();
    }

    public void setDoNotTrack(boolean dnt) {
        currentSettings.setDoNotTrack(dnt);
        saveAndNotify();
    }

    public boolean isSaveBrowsingHistory() {
        return currentSettings.isSaveBrowsingHistory();
    }

    public void setSaveBrowsingHistory(boolean save) {
        currentSettings.setSaveBrowsingHistory(save);
        saveAndNotify();
    }

    public boolean isSavePasswords() {
        return currentSettings.isSavePasswords();
    }

    public void setSavePasswords(boolean save) {
        currentSettings.setSavePasswords(save);
        saveAndNotify();
    }

    public String getDownloadPath() {
        return currentSettings.getDownloadPath();
    }

    public void setDownloadPath(String path) {
        currentSettings.setDownloadPath(path);
        saveAndNotify();
    }

    public boolean isAskDownloadLocation() {
        return currentSettings.isAskDownloadLocation();
    }

    public void setAskDownloadLocation(boolean ask) {
        currentSettings.setAskDownloadLocation(ask);
        saveAndNotify();
    }

    public boolean isHardwareAcceleration() {
        return currentSettings.isHardwareAcceleration();
    }

    public void setHardwareAcceleration(boolean enabled) {
        currentSettings.setHardwareAcceleration(enabled);
        saveAndNotify();
    }

    public boolean isSmoothScrolling() {
        return currentSettings.isSmoothScrolling();
    }

    public void setSmoothScrolling(boolean enabled) {
        currentSettings.setSmoothScrolling(enabled);
        saveAndNotify();
    }

    public boolean isHighContrast() {
        return currentSettings.isHighContrast();
    }

    public void setHighContrast(boolean enabled) {
        currentSettings.setHighContrast(enabled);
        saveAndNotify();
    }

    public boolean isReduceMotion() {
        return currentSettings.isReduceMotion();
    }

    public void setReduceMotion(boolean reduce) {
        currentSettings.setReduceMotion(reduce);
        saveAndNotify();
    }

    public boolean isEnableJavaScript() {
        return currentSettings.isEnableJavaScript();
    }

    public void setEnableJavaScript(boolean enabled) {
        currentSettings.setEnableJavaScript(enabled);
        saveAndNotify();
    }

    public boolean isDeveloperMode() {
        return currentSettings.isDeveloperMode();
    }

    public void setDeveloperMode(boolean enabled) {
        currentSettings.setDeveloperMode(enabled);
        saveAndNotify();
    }

    public String getProxyMode() {
        return currentSettings.getProxyMode();
    }

    public void setProxyMode(String mode) {
        currentSettings.setProxyMode(mode);
        saveAndNotify();
    }

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
}
