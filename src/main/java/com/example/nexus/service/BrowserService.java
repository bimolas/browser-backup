package com.example.nexus.service;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Tab;
import com.example.nexus.view.components.BrowserTab;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class BrowserService {
    private static final Logger logger = LoggerFactory.getLogger(BrowserService.class);

    private final DIContainer container;
    private final TabService tabService;
    private final Map<javafx.scene.control.Tab, BrowserTab> tabBrowserMap = new HashMap<>();
    private final Map<javafx.scene.control.Tab, Runnable> tabCleanupMap = new HashMap<>();
    private TabPane tabPane;
    private StackPane browserContainer;
    private TextField addressBar;

    public BrowserService(DIContainer container, TabService tabService) {
        this.container = container;
        this.tabService = tabService;
    }

    public void setUIComponents(TabPane tabPane, StackPane browserContainer, TextField addressBar) {
        this.tabPane = tabPane;
        this.browserContainer = browserContainer;
        this.addressBar = addressBar;
    }

    public BrowserTab openTab(String url) {

        BrowserTab browserTab = new BrowserTab(container, url);

        javafx.scene.control.Tab tab = new javafx.scene.control.Tab();
        tab.setText("New Tab");
        tab.setClosable(false);

        tabBrowserMap.put(tab, browserTab);

        final ChangeListener<String> titleListener = (obs, oldTitle, newTitle) -> Platform.runLater(() -> {

        });
        final ChangeListener<String> urlListener = (obs, oldUrl, newUrl) -> Platform.runLater(() -> {

        });
        browserTab.titleProperty().addListener(titleListener);
        browserTab.urlProperty().addListener(urlListener);
        tabCleanupMap.put(tab, () -> {
            try { browserTab.titleProperty().removeListener(titleListener); } catch (Exception ignored) {}
            try { browserTab.urlProperty().removeListener(urlListener); } catch (Exception ignored) {}
        });

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

        Tab tabModel = new Tab(url);
        tabService.saveTab(tabModel);
        browserTab.setTabModel(tabModel);
        return browserTab;
    }

    public void closeTab(javafx.scene.control.Tab tab) {
        BrowserTab browserTab = tabBrowserMap.get(tab);
        Runnable cleanup = tabCleanupMap.remove(tab);
        if (cleanup != null) {
            try { cleanup.run(); } catch (Exception e) { logger.debug("Error running tab cleanup", e); }
        }
        tabBrowserMap.remove(tab);
        tabPane.getTabs().remove(tab);
        if (browserTab != null && browserTab.getTabModel() != null) {
            tabService.deleteTab(browserTab.getTabModel().getId());
        }
        if (browserTab != null) {
            browserTab.dispose();
        }
    }

    public BrowserTab getCurrentBrowserTab() {
        javafx.scene.control.Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            return tabBrowserMap.get(selectedTab);
        }
        return null;
    }

    public void navigateToUrl(String url, String homePage, String searchEngine) {
        BrowserTab currentTab = getCurrentBrowserTab();
        if (currentTab != null) {
            String processedUrl = processUrl(url, homePage, searchEngine);
            currentTab.loadUrl(processedUrl);
        }
    }

    public String processUrl(String url, String homePage, String searchEngine) {
        if (url == null || url.trim().isEmpty()) {
            return homePage;
        }
        url = url.trim();
        if (!url.contains("://") && !url.contains(".")) {
            return getSearchEngineUrl(searchEngine) + url;
        }
        if (!url.contains("://")) {
            return "https://" + url;
        }
        return url;
    }

    public String getSearchEngineUrl(String searchEngine) {
        if (searchEngine == null) return "https://www.google.com/search?q=";
        switch (searchEngine.toLowerCase()) {
            case "bing": return "https://www.bing.com/search?q=";
            case "duckduckgo": return "https://www.duckduckgo.com/?q=";
            default: return "https://www.google.com/search?q=";
        }
    }

    public void handleBack() {
        BrowserTab currentTab = getCurrentBrowserTab();
        if (currentTab != null) {
            currentTab.goBack();
        }
    }

    public void handleForward() {
        BrowserTab currentTab = getCurrentBrowserTab();
        if (currentTab != null) {
            currentTab.goForward();
        }
    }

    public void handleReload() {
        BrowserTab currentTab = getCurrentBrowserTab();
        if (currentTab != null) {
            currentTab.reload();
        }
    }

    public void handleHome(String homePage, String searchEngine) {
        navigateToUrl(homePage, homePage, searchEngine);
    }

}
