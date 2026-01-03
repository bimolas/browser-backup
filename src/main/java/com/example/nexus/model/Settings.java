package com.example.nexus.model;

public class Settings {
    private int id;
    private int userId;

    private String theme;
    private String accentColor;
    private int fontSize;
    private double pageZoom;
    private boolean showBookmarksBar;
    private boolean showStatusBar;
    private boolean compactMode;

    private String homePage;
    private String startupBehavior;
    private boolean restoreSession;
    private String newTabPage;
    private String customNewTabUrl;

    private String searchEngine;
    private String customSearchUrl;
    private boolean showSearchSuggestions;
    private boolean searchInAddressBar;

    private boolean clearHistoryOnExit;
    private boolean clearCookiesOnExit;
    private boolean clearCacheOnExit;
    private boolean blockPopups;
    private boolean doNotTrack;
    private boolean blockThirdPartyCookies;
    private boolean httpsOnlyMode;
    private boolean saveBrowsingHistory;
    private boolean saveFormData;
    private boolean savePasswords;

    private String downloadPath;
    private boolean askDownloadLocation;
    private boolean openPdfInBrowser;
    private boolean showDownloadNotification;

    private boolean hardwareAcceleration;
    private boolean smoothScrolling;
    private boolean preloadPages;
    private boolean lazyLoadImages;
    private int maxTabsInMemory;

    private boolean highContrast;
    private boolean reduceMotion;
    private boolean forceZoom;
    private String defaultEncoding;

    private boolean enableJavaScript;
    private boolean enableImages;
    private boolean enableWebGL;
    private boolean developerMode;
    private String proxyMode;
    private String proxyHost;
    private int proxyPort;
    private String userAgent;

    private boolean enableNotifications;
    private boolean soundEnabled;

    public Settings() {
        setDefaults();
    }

    public Settings(int userId) {
        this.userId = userId;
        setDefaults();
    }

    private void setDefaults() {

        this.theme = "light";
        this.accentColor = "#3b82f6";
        this.fontSize = 14;
        this.pageZoom = 1.0;
        this.showBookmarksBar = true;
        this.showStatusBar = true;
        this.compactMode = false;

        this.homePage = "https://www.google.com";
        this.startupBehavior = "show_home";
        this.restoreSession = true;
        this.newTabPage = "new_tab";
        this.customNewTabUrl = "";

        this.searchEngine = "google";
        this.customSearchUrl = "";
        this.showSearchSuggestions = true;
        this.searchInAddressBar = true;

        this.clearHistoryOnExit = false;
        this.clearCookiesOnExit = false;
        this.clearCacheOnExit = false;
        this.blockPopups = true;
        this.doNotTrack = true;
        this.blockThirdPartyCookies = false;
        this.httpsOnlyMode = false;
        this.saveBrowsingHistory = true;
        this.saveFormData = true;
        this.savePasswords = true;

        this.downloadPath = System.getProperty("user.home") + "/Downloads";
        this.askDownloadLocation = false;
        this.openPdfInBrowser = true;
        this.showDownloadNotification = true;

        this.hardwareAcceleration = true;
        this.smoothScrolling = true;
        this.preloadPages = true;
        this.lazyLoadImages = true;
        this.maxTabsInMemory = 0;

        this.highContrast = false;
        this.reduceMotion = false;
        this.forceZoom = false;
        this.defaultEncoding = "UTF-8";

        this.enableJavaScript = true;
        this.enableImages = true;
        this.enableWebGL = true;
        this.developerMode = false;
        this.proxyMode = "system";
        this.proxyHost = "";
        this.proxyPort = 8080;
        this.userAgent = "";

        this.enableNotifications = true;
        this.soundEnabled = true;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getAccentColor() { return accentColor; }
    public void setAccentColor(String accentColor) { this.accentColor = accentColor; }

    public int getFontSize() { return fontSize; }
    public void setFontSize(int fontSize) { this.fontSize = Math.max(9, Math.min(24, fontSize)); }

    public double getPageZoom() { return pageZoom; }
    public void setPageZoom(double pageZoom) { this.pageZoom = Math.max(0.25, Math.min(5.0, pageZoom)); }

    public boolean isShowBookmarksBar() { return showBookmarksBar; }
    public void setShowBookmarksBar(boolean showBookmarksBar) { this.showBookmarksBar = showBookmarksBar; }

    public boolean isShowStatusBar() { return showStatusBar; }
    public void setShowStatusBar(boolean showStatusBar) { this.showStatusBar = showStatusBar; }

    public boolean isCompactMode() { return compactMode; }
    public void setCompactMode(boolean compactMode) { this.compactMode = compactMode; }

    public String getHomePage() { return homePage; }
    public void setHomePage(String homePage) { this.homePage = homePage; }

    public String getStartupBehavior() { return startupBehavior; }
    public void setStartupBehavior(String startupBehavior) { this.startupBehavior = startupBehavior; }

    public boolean isRestoreSession() { return restoreSession; }
    public void setRestoreSession(boolean restoreSession) { this.restoreSession = restoreSession; }

    public String getNewTabPage() { return newTabPage; }
    public void setNewTabPage(String newTabPage) { this.newTabPage = newTabPage; }

    public String getCustomNewTabUrl() { return customNewTabUrl; }
    public void setCustomNewTabUrl(String customNewTabUrl) { this.customNewTabUrl = customNewTabUrl; }

    public String getSearchEngine() { return searchEngine; }
    public void setSearchEngine(String searchEngine) { this.searchEngine = searchEngine; }

    public String getCustomSearchUrl() { return customSearchUrl; }
    public void setCustomSearchUrl(String customSearchUrl) { this.customSearchUrl = customSearchUrl; }

    public boolean isShowSearchSuggestions() { return showSearchSuggestions; }
    public void setShowSearchSuggestions(boolean showSearchSuggestions) { this.showSearchSuggestions = showSearchSuggestions; }

    public boolean isSearchInAddressBar() { return searchInAddressBar; }
    public void setSearchInAddressBar(boolean searchInAddressBar) { this.searchInAddressBar = searchInAddressBar; }

    public boolean isClearHistoryOnExit() { return clearHistoryOnExit; }
    public void setClearHistoryOnExit(boolean clearHistoryOnExit) { this.clearHistoryOnExit = clearHistoryOnExit; }

    public boolean isClearCookiesOnExit() { return clearCookiesOnExit; }
    public void setClearCookiesOnExit(boolean clearCookiesOnExit) { this.clearCookiesOnExit = clearCookiesOnExit; }

    public boolean isClearCacheOnExit() { return clearCacheOnExit; }
    public void setClearCacheOnExit(boolean clearCacheOnExit) { this.clearCacheOnExit = clearCacheOnExit; }

    public boolean isBlockPopups() { return blockPopups; }
    public void setBlockPopups(boolean blockPopups) { this.blockPopups = blockPopups; }

    public boolean isDoNotTrack() { return doNotTrack; }
    public void setDoNotTrack(boolean doNotTrack) { this.doNotTrack = doNotTrack; }

    public boolean isBlockThirdPartyCookies() { return blockThirdPartyCookies; }
    public void setBlockThirdPartyCookies(boolean blockThirdPartyCookies) { this.blockThirdPartyCookies = blockThirdPartyCookies; }

    public boolean isHttpsOnlyMode() { return httpsOnlyMode; }
    public void setHttpsOnlyMode(boolean httpsOnlyMode) { this.httpsOnlyMode = httpsOnlyMode; }

    public boolean isSaveBrowsingHistory() { return saveBrowsingHistory; }
    public void setSaveBrowsingHistory(boolean saveBrowsingHistory) { this.saveBrowsingHistory = saveBrowsingHistory; }

    public boolean isSaveFormData() { return saveFormData; }
    public void setSaveFormData(boolean saveFormData) { this.saveFormData = saveFormData; }

    public boolean isSavePasswords() { return savePasswords; }
    public void setSavePasswords(boolean savePasswords) { this.savePasswords = savePasswords; }

    public String getDownloadPath() { return downloadPath; }
    public void setDownloadPath(String downloadPath) { this.downloadPath = downloadPath; }

    public boolean isAskDownloadLocation() { return askDownloadLocation; }
    public void setAskDownloadLocation(boolean askDownloadLocation) { this.askDownloadLocation = askDownloadLocation; }

    public boolean isOpenPdfInBrowser() { return openPdfInBrowser; }
    public void setOpenPdfInBrowser(boolean openPdfInBrowser) { this.openPdfInBrowser = openPdfInBrowser; }

    public boolean isShowDownloadNotification() { return showDownloadNotification; }
    public void setShowDownloadNotification(boolean showDownloadNotification) { this.showDownloadNotification = showDownloadNotification; }

    public boolean isHardwareAcceleration() { return hardwareAcceleration; }
    public void setHardwareAcceleration(boolean hardwareAcceleration) { this.hardwareAcceleration = hardwareAcceleration; }

    public boolean isSmoothScrolling() { return smoothScrolling; }
    public void setSmoothScrolling(boolean smoothScrolling) { this.smoothScrolling = smoothScrolling; }

    public boolean isPreloadPages() { return preloadPages; }
    public void setPreloadPages(boolean preloadPages) { this.preloadPages = preloadPages; }

    public boolean isLazyLoadImages() { return lazyLoadImages; }
    public void setLazyLoadImages(boolean lazyLoadImages) { this.lazyLoadImages = lazyLoadImages; }

    public int getMaxTabsInMemory() { return maxTabsInMemory; }
    public void setMaxTabsInMemory(int maxTabsInMemory) { this.maxTabsInMemory = Math.max(0, maxTabsInMemory); }

    public boolean isHighContrast() { return highContrast; }
    public void setHighContrast(boolean highContrast) { this.highContrast = highContrast; }

    public boolean isReduceMotion() { return reduceMotion; }
    public void setReduceMotion(boolean reduceMotion) { this.reduceMotion = reduceMotion; }

    public boolean isForceZoom() { return forceZoom; }
    public void setForceZoom(boolean forceZoom) { this.forceZoom = forceZoom; }

    public String getDefaultEncoding() { return defaultEncoding; }
    public void setDefaultEncoding(String defaultEncoding) { this.defaultEncoding = defaultEncoding; }

    public boolean isEnableJavaScript() { return enableJavaScript; }
    public void setEnableJavaScript(boolean enableJavaScript) { this.enableJavaScript = enableJavaScript; }

    public boolean isEnableImages() { return enableImages; }
    public void setEnableImages(boolean enableImages) { this.enableImages = enableImages; }

    public boolean isEnableWebGL() { return enableWebGL; }
    public void setEnableWebGL(boolean enableWebGL) { this.enableWebGL = enableWebGL; }

    public boolean isDeveloperMode() { return developerMode; }
    public void setDeveloperMode(boolean developerMode) { this.developerMode = developerMode; }

    public String getProxyMode() { return proxyMode; }
    public void setProxyMode(String proxyMode) { this.proxyMode = proxyMode; }

    public String getProxyHost() { return proxyHost; }
    public void setProxyHost(String proxyHost) { this.proxyHost = proxyHost; }

    public int getProxyPort() { return proxyPort; }
    public void setProxyPort(int proxyPort) { this.proxyPort = proxyPort; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public boolean isEnableNotifications() { return enableNotifications; }
    public void setEnableNotifications(boolean enableNotifications) { this.enableNotifications = enableNotifications; }

    public boolean isSoundEnabled() { return soundEnabled; }
    public void setSoundEnabled(boolean soundEnabled) { this.soundEnabled = soundEnabled; }

    public String getSearchUrl(String query) {
        String encodedQuery;
        try {
            encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
        } catch (Exception e) {
            encodedQuery = query.replace(" ", "+");
        }

        return switch (searchEngine.toLowerCase()) {
            case "google" -> "https://www.google.com/search?q=" + encodedQuery;
            case "bing" -> "https://www.bing.com/search?q=" + encodedQuery;
            case "duckduckgo" -> "https://duckduckgo.com/?q=" + encodedQuery;
            case "yahoo" -> "https://search.yahoo.com/search?p=" + encodedQuery;
            case "ecosia" -> "https://www.ecosia.org/search?q=" + encodedQuery;
            case "brave" -> "https://search.brave.com/search?q=" + encodedQuery;
            case "custom" -> customSearchUrl.replace("%s", encodedQuery);
            default -> "https://www.google.com/search?q=" + encodedQuery;
        };
    }

    public void resetToDefaults() {
        int savedId = this.id;
        int savedUserId = this.userId;
        setDefaults();
        this.id = savedId;
        this.userId = savedUserId;
    }

    @Override
    public String toString() {
        return "Settings{" +
                "id=" + id +
                ", userId=" + userId +
                ", theme='" + theme + '\'' +
                ", searchEngine='" + searchEngine + '\'' +
                ", homePage='" + homePage + '\'' +
                '}';
    }
}
