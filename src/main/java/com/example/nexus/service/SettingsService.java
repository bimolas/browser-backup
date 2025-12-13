package com.example.nexus.service;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Settings;
import com.example.nexus.repository.SettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service for managing browser settings.
 * Provides a centralized way to access and modify all browser configuration.
 */
public class SettingsService implements ISettingsService {
    private static final Logger logger = LoggerFactory.getLogger(SettingsService.class);

    private final SettingsRepository settingsRepository;
    private Settings currentSettings;
    private final List<Consumer<Settings>> changeListeners = new ArrayList<>();

    public SettingsService(DIContainer container) {
        this.settingsRepository = container.getOrCreate(SettingsRepository.class);
        loadSettings();
    }

    private void loadSettings() {
        // Load settings for default user (ID 1)
        currentSettings = settingsRepository.findByUserId(1);
        if (currentSettings == null) {
            currentSettings = new Settings(1);
            settingsRepository.save(currentSettings);
            logger.info("Created default settings");
        }
        logger.info("Loaded settings: {}", currentSettings);
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
        settingsRepository.update(currentSettings);
        notifyListeners();
    }

    // ==================== CORE ====================

    @Override
    public Settings getSettings() {
        return currentSettings;
    }

    @Override
    public void saveSettings() {
        settingsRepository.update(currentSettings);
        logger.info("Settings saved");
    }

    @Override
    public void resetToDefaults() {
        currentSettings.resetToDefaults();
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
        return currentSettings.getTheme();
    }

    @Override
    public void setTheme(String theme) {
        currentSettings.setTheme(theme);
        saveAndNotify();
        logger.info("Theme changed to: {}", theme);
    }

    @Override
    public String getAccentColor() {
        return currentSettings.getAccentColor();
    }

    @Override
    public void setAccentColor(String accentColor) {
        currentSettings.setAccentColor(accentColor);
        saveAndNotify();
    }

    @Override
    public int getFontSize() {
        return currentSettings.getFontSize();
    }

    @Override
    public void setFontSize(int fontSize) {
        currentSettings.setFontSize(fontSize);
        saveAndNotify();
    }

    @Override
    public double getPageZoom() {
        return currentSettings.getPageZoom();
    }

    @Override
    public void setPageZoom(double pageZoom) {
        currentSettings.setPageZoom(pageZoom);
        saveAndNotify();
    }

    @Override
    public boolean isShowBookmarksBar() {
        return currentSettings.isShowBookmarksBar();
    }

    @Override
    public void setShowBookmarksBar(boolean show) {
        currentSettings.setShowBookmarksBar(show);
        saveAndNotify();
    }

    @Override
    public boolean isShowStatusBar() {
        return currentSettings.isShowStatusBar();
    }

    @Override
    public void setShowStatusBar(boolean show) {
        currentSettings.setShowStatusBar(show);
        saveAndNotify();
    }

    @Override
    public boolean isCompactMode() {
        return currentSettings.isCompactMode();
    }

    @Override
    public void setCompactMode(boolean compact) {
        currentSettings.setCompactMode(compact);
        saveAndNotify();
    }

    // ==================== STARTUP & HOME ====================

    @Override
    public String getHomePage() {
        return currentSettings.getHomePage();
    }

    @Override
    public void setHomePage(String homePage) {
        currentSettings.setHomePage(homePage);
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
        return currentSettings.getSearchEngine();
    }

    @Override
    public void setSearchEngine(String searchEngine) {
        currentSettings.setSearchEngine(searchEngine);
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