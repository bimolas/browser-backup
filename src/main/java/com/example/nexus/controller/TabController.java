package com.example.nexus.controller;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Tab;
import com.example.nexus.service.TabService;
import com.example.nexus.view.components.BrowserTab;
import javafx.scene.control.TabPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Controller for tab management operations.
 * Handles tab creation, closing, switching, and state management.
 */
public class TabController {
    private static final Logger logger = LoggerFactory.getLogger(TabController.class);

    private final DIContainer container;
    private final TabService tabService;
    private final Map<javafx.scene.control.Tab, BrowserTab> tabBrowserMap = new HashMap<>();

    private TabPane tabPane;
    private Consumer<BrowserTab> onTabSelected;
    private Consumer<BrowserTab> onTabClosed;

    public TabController(DIContainer container, TabService tabService) {
        this.container = container;
        this.tabService = tabService;
    }

    /**
     * Set the TabPane to manage
     */
    public void setTabPane(TabPane tabPane) {
        this.tabPane = tabPane;
    }

    /**
     * Set callback for tab selection
     */
    public void setOnTabSelected(Consumer<BrowserTab> callback) {
        this.onTabSelected = callback;
    }

    /**
     * Set callback for tab closed
     */
    public void setOnTabClosed(Consumer<BrowserTab> callback) {
        this.onTabClosed = callback;
    }

    /**
     * Create a new tab with the given URL
     */
    public javafx.scene.control.Tab createTab(String url) {
        return createTab(url, null);
    }

    /**
     * Create a new tab with URL and title
     */
    public javafx.scene.control.Tab createTab(String url, String title) {
        try {
            // Create model
            Tab tabModel = new Tab();
            tabModel.setUrl(url);
            tabModel.setTitle(title != null ? title : "New Tab");
            tabService.saveTab(tabModel);

            // Create browser tab
            BrowserTab browserTab = new BrowserTab(container, url);
            browserTab.setTabModel(tabModel);

            // Create JavaFX tab
            javafx.scene.control.Tab fxTab = new javafx.scene.control.Tab();
            fxTab.setText(tabModel.getTitle());

            // Store mapping
            tabBrowserMap.put(fxTab, browserTab);

            // Add to tab pane
            if (tabPane != null) {
                tabPane.getTabs().add(fxTab);
                tabPane.getSelectionModel().select(fxTab);
            }

            logger.info("Created new tab: {}", url);
            return fxTab;

        } catch (Exception e) {
            logger.error("Error creating tab", e);
            return null;
        }
    }

    /**
     * Close a tab
     */
    public void closeTab(javafx.scene.control.Tab tab) {
        if (tab == null) return;

        BrowserTab browserTab = tabBrowserMap.get(tab);
        if (browserTab != null) {
            // Notify callback
            if (onTabClosed != null) {
                onTabClosed.accept(browserTab);
            }

            // Dispose browser tab
            browserTab.dispose();

            // Remove from map
            tabBrowserMap.remove(tab);

            // Delete from database
            Tab tabModel = browserTab.getTabModel();
            if (tabModel != null) {
                tabService.deleteTab(tabModel.getId());
            }
        }

        // Remove from tab pane
        if (tabPane != null) {
            tabPane.getTabs().remove(tab);
        }

        logger.info("Closed tab");
    }

    /**
     * Get the BrowserTab for a JavaFX Tab
     */
    public BrowserTab getBrowserTab(javafx.scene.control.Tab tab) {
        return tabBrowserMap.get(tab);
    }

    /**
     * Get the currently selected BrowserTab
     */
    public BrowserTab getCurrentBrowserTab() {
        if (tabPane == null) return null;
        javafx.scene.control.Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        return selectedTab != null ? tabBrowserMap.get(selectedTab) : null;
    }

    /**
     * Get the currently selected Tab
     */
    public javafx.scene.control.Tab getCurrentTab() {
        return tabPane != null ? tabPane.getSelectionModel().getSelectedItem() : null;
    }

    /**
     * Select a tab
     */
    public void selectTab(javafx.scene.control.Tab tab) {
        if (tabPane != null && tab != null) {
            tabPane.getSelectionModel().select(tab);

            BrowserTab browserTab = tabBrowserMap.get(tab);
            if (browserTab != null && onTabSelected != null) {
                onTabSelected.accept(browserTab);
            }
        }
    }

    /**
     * Get the number of open tabs
     */
    public int getTabCount() {
        return tabPane != null ? tabPane.getTabs().size() : 0;
    }

    /**
     * Close all tabs
     */
    public void closeAllTabs() {
        if (tabPane != null) {
            // Copy list to avoid concurrent modification
            var tabs = new java.util.ArrayList<>(tabPane.getTabs());
            for (javafx.scene.control.Tab tab : tabs) {
                closeTab(tab);
            }
        }
    }

    /**
     * Update tab title
     */
    public void updateTabTitle(javafx.scene.control.Tab tab, String title) {
        if (tab != null) {
            tab.setText(title != null ? title : "New Tab");

            BrowserTab browserTab = tabBrowserMap.get(tab);
            if (browserTab != null && browserTab.getTabModel() != null) {
                browserTab.getTabModel().setTitle(title);
                tabService.updateTab(browserTab.getTabModel());
            }
        }
    }

    /**
     * Get tab browser map (for external access if needed)
     */
    public Map<javafx.scene.control.Tab, BrowserTab> getTabBrowserMap() {
        return tabBrowserMap;
    }
}
