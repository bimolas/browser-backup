package com.example.nexus.controller;

import com.example.nexus.service.SettingsService;
import com.example.nexus.service.TabService;
import com.example.nexus.service.ZoomService;
import com.example.nexus.view.components.BrowserTab;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NavigationController {
    private static final Logger logger = LoggerFactory.getLogger(NavigationController.class);

    private final TabController tabController;
    private final SettingsService settingsService;

    public NavigationController(TabController tabController, SettingsService settingsService, ZoomService zoomService) {
        this.tabController = tabController;
        this.settingsService = settingsService;
    }

    public void goBack() {
        BrowserTab currentTab = tabController.getCurrentBrowserTab();
        if (currentTab != null) {
            logger.debug("Navigating back");
            currentTab.goBack();
        }
    }

    public void goForward() {
        BrowserTab currentTab = tabController.getCurrentBrowserTab();
        if (currentTab != null) {
            logger.debug("Navigating forward");
            currentTab.goForward();
        }
    }

    public void reload() {
        BrowserTab currentTab = tabController.getCurrentBrowserTab();
        if (currentTab != null) {
            logger.debug("Reloading current page");
            currentTab.reload();
        }
    }

    public void goHome() {
        logger.debug("Navigating to home page");
        String homePage = settingsService.getHomePage();
        navigateToUrl(homePage);
    }

    public void navigateToUrl(String url) {
        BrowserTab currentTab = tabController.getCurrentBrowserTab();
        if (currentTab != null) {
            String processedUrl = processUrl(url);
            logger.debug("Navigating to URL: {}", processedUrl);
            currentTab.loadUrl(processedUrl);
        }
    }

    public String processUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return settingsService.getHomePage();
        }

        url = url.trim();

        if (!url.contains("://") && !url.contains(".")) {
            String searchEngine = settingsService.getSearchEngine();
            return getSearchEngineUrl(searchEngine) + url;
        }

        if (!url.contains("://")) {
            return "https://" + url;
        }

        return url;
    }

    public static String getSearchEngineUrl(String searchEngine) {
        return switch (searchEngine.toLowerCase()) {
            case "bing" -> "https://www.bing.com/search?q=";
            case "duckduckgo" -> "https://www.duckduckgo.com/?q=";
            default -> "https://www.google.com/search?q=";
        };
    }

    public void closeCurrentTab() {
        Tab selectedTab = tabController.getTabPane().getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            logger.debug("Closing current tab");
            tabController.handleTabClose(selectedTab);
        }
    }

    public void nextTab() {
        int currentIndex = tabController.getTabPane().getSelectionModel().getSelectedIndex();
        if (currentIndex >= 0 && currentIndex < tabController.getTabPane().getTabs().size() - 1) {
            logger.debug("Switching to next tab");
            tabController.getTabPane().getSelectionModel().select(currentIndex + 1);
        }
    }

    public void previousTab() {
        int currentIndex = tabController.getTabPane().getSelectionModel().getSelectedIndex();
        if (currentIndex > 0) {
            logger.debug("Switching to previous tab");
            tabController.getTabPane().getSelectionModel().select(currentIndex - 1);
        }
    }

    public void duplicateCurrentTab() {
        BrowserTab currentTab = tabController.getCurrentBrowserTab();
        if (currentTab != null) {
            logger.debug("Duplicating current tab with URL: {}", currentTab.getUrl());
            tabController.createNewTab(currentTab.getUrl());
        }
    }

    public void focusAddressBar(TextField addressBar) {
        logger.debug("Focusing address bar");
        addressBar.requestFocus();
    }
}
