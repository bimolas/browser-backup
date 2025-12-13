package com.example.nexus.model;

/**
 * Settings model containing all browser configuration options.
 * Organized into logical categories for better maintainability.
 */
public class Settings {
    private int id;
    private int userId;

    // ===== APPEARANCE =====
    private String theme;                    // light, dark, system
    private String accentColor;              // hex color
    private int fontSize;                    // base font size (9-24)
    private double pageZoom;                 // default page zoom (0.25-5.0)
    private boolean showBookmarksBar;        // show bookmarks bar below address bar
    private boolean showStatusBar;           // show status bar at bottom
    private boolean compactMode;             // compact UI mode

    // ===== STARTUP & HOME =====
    private String homePage;                 // home page URL
    private String startupBehavior;          // show_home, restore_session, show_blank, show_new_tab
    private boolean restoreSession;          // restore tabs on startup
    private String newTabPage;               // new_tab, home, blank, custom
    private String customNewTabUrl;          // custom new tab URL

    // ===== SEARCH =====
    private String searchEngine;             // google, bing, duckduckgo, yahoo, custom
    private String customSearchUrl;          // custom search engine URL (use %s for query)
    private boolean showSearchSuggestions;   // show search suggestions
    private boolean searchInAddressBar;      // enable search from address bar

    // ===== PRIVACY & SECURITY =====
    private boolean clearHistoryOnExit;
    private boolean clearCookiesOnExit;
    private boolean clearCacheOnExit;
    private boolean blockPopups;             // block popup windows
    private boolean doNotTrack;              // send DNT header
    private boolean blockThirdPartyCookies;
    private boolean httpsOnlyMode;           // prefer HTTPS
    private boolean saveBrowsingHistory;     // save browsing history
    private boolean saveFormData;            // save form autofill data
    private boolean savePasswords;           // offer to save passwords

    // ===== DOWNLOADS =====
    private String downloadPath;             // default download location
    private boolean askDownloadLocation;     // ask where to save each file
    private boolean openPdfInBrowser;        // open PDF files in browser
    private boolean showDownloadNotification; // show notification when download completes

    // ===== PERFORMANCE =====
    private boolean hardwareAcceleration;    // use hardware acceleration
    private boolean smoothScrolling;         // enable smooth scrolling
    private boolean preloadPages;            // preload pages for faster loading
    private boolean lazyLoadImages;          // lazy load images
    private int maxTabsInMemory;             // max tabs to keep in memory (0 = unlimited)

    // ===== ACCESSIBILITY =====
    private boolean highContrast;            // high contrast mode
    private boolean reduceMotion;            // reduce animations
    private boolean forceZoom;               // allow zoom on all pages
    private String defaultEncoding;          // default text encoding (UTF-8)

    // ===== ADVANCED =====
    private boolean enableJavaScript;        // enable JavaScript
    private boolean enableImages;            // load images
    private boolean enableWebGL;             // enable WebGL
    private boolean developerMode;           // enable developer features
    private String proxyMode;                // none, system, manual
    private String proxyHost;
    private int proxyPort;
    private String userAgent;                // custom user agent (empty = default)

    // ===== NOTIFICATIONS =====
    private boolean enableNotifications;     // allow site notifications
    private boolean soundEnabled;            // enable sound effects

    /**
     * Default constructor with sensible defaults
     */
    public Settings() {
        setDefaults();
    }

    /**
     * Constructor with user ID
     */
    public Settings(int userId) {
        this.userId = userId;
        setDefaults();
    }

    /**
     * Set all settings to sensible defaults
     */
    private void setDefaults() {
        // Appearance
        this.theme = "light";
        this.accentColor = "#3b82f6";
        this.fontSize = 14;
        this.pageZoom = 1.0;
        this.showBookmarksBar = true;
        this.showStatusBar = true;
        this.compactMode = false;

        // Startup & Home
        this.homePage = "https://www.google.com";
        this.startupBehavior = "show_home";
        this.restoreSession = true;
        this.newTabPage = "new_tab";
        this.customNewTabUrl = "";

        // Search
        this.searchEngine = "google";
        this.customSearchUrl = "";
        this.showSearchSuggestions = true;
        this.searchInAddressBar = true;

        // Privacy & Security
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

        // Downloads
        this.downloadPath = System.getProperty("user.home") + "/Downloads";
        this.askDownloadLocation = false;
        this.openPdfInBrowser = true;
        this.showDownloadNotification = true;

        // Performance
        this.hardwareAcceleration = true;
        this.smoothScrolling = true;
        this.preloadPages = true;
        this.lazyLoadImages = true;
        this.maxTabsInMemory = 0;

        // Accessibility
        this.highContrast = false;
        this.reduceMotion = false;
        this.forceZoom = false;
        this.defaultEncoding = "UTF-8";

        // Advanced
        this.enableJavaScript = true;
        this.enableImages = true;
        this.enableWebGL = true;
        this.developerMode = false;
        this.proxyMode = "system";
        this.proxyHost = "";
        this.proxyPort = 8080;
        this.userAgent = "";

        // Notifications
        this.enableNotifications = true;
        this.soundEnabled = true;
    }

    // ==================== GETTERS AND SETTERS ====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    // Appearance
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

    // Startup & Home
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

    // Search
    public String getSearchEngine() { return searchEngine; }
    public void setSearchEngine(String searchEngine) { this.searchEngine = searchEngine; }

    public String getCustomSearchUrl() { return customSearchUrl; }
    public void setCustomSearchUrl(String customSearchUrl) { this.customSearchUrl = customSearchUrl; }

    public boolean isShowSearchSuggestions() { return showSearchSuggestions; }
    public void setShowSearchSuggestions(boolean showSearchSuggestions) { this.showSearchSuggestions = showSearchSuggestions; }

    public boolean isSearchInAddressBar() { return searchInAddressBar; }
    public void setSearchInAddressBar(boolean searchInAddressBar) { this.searchInAddressBar = searchInAddressBar; }

    // Privacy & Security
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

    // Downloads
    public String getDownloadPath() { return downloadPath; }
    public void setDownloadPath(String downloadPath) { this.downloadPath = downloadPath; }

    public boolean isAskDownloadLocation() { return askDownloadLocation; }
    public void setAskDownloadLocation(boolean askDownloadLocation) { this.askDownloadLocation = askDownloadLocation; }

    public boolean isOpenPdfInBrowser() { return openPdfInBrowser; }
    public void setOpenPdfInBrowser(boolean openPdfInBrowser) { this.openPdfInBrowser = openPdfInBrowser; }

    public boolean isShowDownloadNotification() { return showDownloadNotification; }
    public void setShowDownloadNotification(boolean showDownloadNotification) { this.showDownloadNotification = showDownloadNotification; }

    // Performance
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

    // Accessibility
    public boolean isHighContrast() { return highContrast; }
    public void setHighContrast(boolean highContrast) { this.highContrast = highContrast; }

    public boolean isReduceMotion() { return reduceMotion; }
    public void setReduceMotion(boolean reduceMotion) { this.reduceMotion = reduceMotion; }

    public boolean isForceZoom() { return forceZoom; }
    public void setForceZoom(boolean forceZoom) { this.forceZoom = forceZoom; }

    public String getDefaultEncoding() { return defaultEncoding; }
    public void setDefaultEncoding(String defaultEncoding) { this.defaultEncoding = defaultEncoding; }

    // Advanced
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

    // Notifications
    public boolean isEnableNotifications() { return enableNotifications; }
    public void setEnableNotifications(boolean enableNotifications) { this.enableNotifications = enableNotifications; }

    public boolean isSoundEnabled() { return soundEnabled; }
    public void setSoundEnabled(boolean soundEnabled) { this.soundEnabled = soundEnabled; }

    /**
     * Get search URL for the configured search engine
     */
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

    /**
     * Reset all settings to defaults
     */
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
