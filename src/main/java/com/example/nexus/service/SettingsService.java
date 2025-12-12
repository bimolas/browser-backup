package com.example.nexus.service;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Settings;
import com.example.nexus.repository.SettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsService {
    private static final Logger logger = LoggerFactory.getLogger(SettingsService.class);

    private final SettingsRepository settingsRepository;
    private Settings currentSettings;

    public SettingsService(DIContainer container) {
        this.settingsRepository = container.getOrCreate(SettingsRepository.class);
        loadSettings();
    }

    private void loadSettings() {
        // For now, we'll just use the default user (ID 1)
        currentSettings = settingsRepository.findByUserId(1);
        if (currentSettings == null) {
            currentSettings = new Settings(1);
            settingsRepository.save(currentSettings);
        }
    }

    public String getTheme() {
        return currentSettings.getTheme();
    }

    public void setTheme(String theme) {
        currentSettings.setTheme(theme);
        settingsRepository.update(currentSettings);
    }

    public String getAccentColor() {
        return currentSettings.getAccentColor();
    }

    public void setAccentColor(String accentColor) {
        currentSettings.setAccentColor(accentColor);
        settingsRepository.update(currentSettings);
    }

    public String getSearchEngine() {
        return currentSettings.getSearchEngine();
    }

    public void setSearchEngine(String searchEngine) {
        currentSettings.setSearchEngine(searchEngine);
        settingsRepository.update(currentSettings);
    }

    public String getHomePage() {
        return currentSettings.getHomePage();
    }

    public void setHomePage(String homePage) {
        currentSettings.setHomePage(homePage);
        settingsRepository.update(currentSettings);
    }

    public String getStartupBehavior() {
        return currentSettings.getStartupBehavior();
    }

    public void setStartupBehavior(String startupBehavior) {
        currentSettings.setStartupBehavior(startupBehavior);
        settingsRepository.update(currentSettings);
    }

    public boolean isRestoreSession() {
        return currentSettings.isRestoreSession();
    }

    public void setRestoreSession(boolean restoreSession) {
        currentSettings.setRestoreSession(restoreSession);
        settingsRepository.update(currentSettings);
    }

    public boolean isClearHistoryOnExit() {
        return currentSettings.isClearHistoryOnExit();
    }

    public void setClearHistoryOnExit(boolean clearHistoryOnExit) {
        currentSettings.setClearHistoryOnExit(clearHistoryOnExit);
        settingsRepository.update(currentSettings);
    }
}