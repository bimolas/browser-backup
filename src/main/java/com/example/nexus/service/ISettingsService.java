package com.example.nexus.service;

import com.example.nexus.model.Settings;

import java.util.function.Consumer;

public interface ISettingsService {

    Settings getSettings();

    void saveSettings();

    void resetToDefaults();

    void addSettingsChangeListener(Consumer<Settings> listener);

    void removeSettingsChangeListener(Consumer<Settings> listener);

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

    String getHomePage();
    void setHomePage(String homePage);

    String getStartupBehavior();
    void setStartupBehavior(String behavior);

    boolean isRestoreSession();
    void setRestoreSession(boolean restore);

    String getNewTabPage();
    void setNewTabPage(String newTabPage);

    String getSearchEngine();
    void setSearchEngine(String searchEngine);

    String getSearchUrl(String query);

    boolean isShowSearchSuggestions();
    void setShowSearchSuggestions(boolean show);

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

    String getDownloadPath();
    void setDownloadPath(String path);

    boolean isAskDownloadLocation();
    void setAskDownloadLocation(boolean ask);

    boolean isHardwareAcceleration();
    void setHardwareAcceleration(boolean enabled);

    boolean isSmoothScrolling();
    void setSmoothScrolling(boolean enabled);

    boolean isHighContrast();
    void setHighContrast(boolean enabled);

    boolean isReduceMotion();
    void setReduceMotion(boolean reduce);

    boolean isEnableJavaScript();
    void setEnableJavaScript(boolean enabled);

    boolean isDeveloperMode();
    void setDeveloperMode(boolean enabled);

    String getProxyMode();
    void setProxyMode(String mode);
}
