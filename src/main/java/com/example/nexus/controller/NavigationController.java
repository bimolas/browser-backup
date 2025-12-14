package com.example.nexus.controller;

import com.example.nexus.service.HistoryService;
import com.example.nexus.service.SettingsService;
import com.example.nexus.view.components.BrowserTab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Controller for URL navigation operations.
 * Handles URL processing, navigation history, and search queries.
 */
public class NavigationController {
    private static final Logger logger = LoggerFactory.getLogger(NavigationController.class);

    private final SettingsService settingsService;
    private final HistoryService historyService;
    private Consumer<String> onNavigate;

    public NavigationController(SettingsService settingsService, HistoryService historyService) {
        this.settingsService = settingsService;
        this.historyService = historyService;
    }

    /**
     * Set callback for navigation events
     */
    public void setOnNavigate(Consumer<String> callback) {
        this.onNavigate = callback;
    }

    /**
     * Navigate to a URL in the given browser tab
     */
    public void navigateTo(BrowserTab browserTab, String input) {
        if (browserTab == null || input == null) return;

        String processedUrl = processInput(input);
        browserTab.loadUrl(processedUrl);

        // Record in history
        recordNavigation(processedUrl, browserTab.getTitle());

        if (onNavigate != null) {
            onNavigate.accept(processedUrl);
        }

        logger.debug("Navigating to: {}", processedUrl);
    }

    /**
     * Process user input into a valid URL
     */
    public String processInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return settingsService.getHomePage();
        }

        String trimmed = input.trim();

        // Check if it's a search query (no dots or protocol)
        if (!trimmed.contains("://") && !trimmed.contains(".")) {
            return getSearchUrl(trimmed);
        }

        // Add https:// if no protocol specified
        if (!trimmed.contains("://")) {
            return "https://" + trimmed;
        }

        return trimmed;
    }

    /**
     * Get search URL for a query
     */
    public String getSearchUrl(String query) {
        String searchEngine = settingsService.getSearchEngine();
        String baseUrl = getSearchEngineBaseUrl(searchEngine);
        return baseUrl + query.replace(" ", "+");
    }

    /**
     * Get base URL for a search engine
     */
    private String getSearchEngineBaseUrl(String searchEngine) {
        if (searchEngine == null) {
            return "https://www.google.com/search?q=";
        }

        return switch (searchEngine.toLowerCase()) {
            case "bing" -> "https://www.bing.com/search?q=";
            case "duckduckgo" -> "https://www.duckduckgo.com/?q=";
            case "yahoo" -> "https://search.yahoo.com/search?p=";
            case "ecosia" -> "https://www.ecosia.org/search?q=";
            default -> "https://www.google.com/search?q=";
        };
    }

    /**
     * Record a navigation event in history
     */
    private void recordNavigation(String url, String title) {
        if (settingsService.isSaveBrowsingHistory() && historyService != null) {
            try {
                historyService.addToHistory(url, title);
            } catch (Exception e) {
                logger.error("Error recording history", e);
            }
        }
    }

    /**
     * Navigate to home page
     */
    public void goHome(BrowserTab browserTab) {
        if (browserTab != null) {
            String homePage = settingsService.getHomePage();
            browserTab.loadUrl(homePage);
        }
    }

    /**
     * Go back in browser history
     */
    public void goBack(BrowserTab browserTab) {
        if (browserTab != null) {
            browserTab.goBack();
        }
    }

    /**
     * Go forward in browser history
     */
    public void goForward(BrowserTab browserTab) {
        if (browserTab != null) {
            browserTab.goForward();
        }
    }

    /**
     * Reload current page
     */
    public void reload(BrowserTab browserTab) {
        if (browserTab != null) {
            browserTab.reload();
        }
    }

    /**
     * Stop loading current page
     */
    public void stop(BrowserTab browserTab) {
        if (browserTab != null) {
            browserTab.stop();
        }
    }

    /**
     * Check if URL is valid
     */
    public boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        try {
            new java.net.URI(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract domain from URL
     */
    public String extractDomain(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }

        try {
            java.net.URI uri = new java.net.URI(url);
            String host = uri.getHost();
            return host != null ? host : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Check if URL is secure (HTTPS)
     */
    public boolean isSecure(String url) {
        return url != null && url.toLowerCase().startsWith("https://");
    }
}

