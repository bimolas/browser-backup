package com.example.nexus.controller;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Tab;
import com.example.nexus.service.*;
import com.example.nexus.util.KeyboardShortcutManager;
import com.example.nexus.view.components.BrowserTab;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private DIContainer container;
    private TabService tabService;
    private BookmarkService bookmarkService;
    private HistoryService historyService;
    private SettingsService settingsService;
    private KeyboardShortcutManager shortcutManager;

    // Map to store browser tabs associated with JavaFX tabs
    private final Map<javafx.scene.control.Tab, BrowserTab> tabBrowserMap = new HashMap<>();

    @FXML private BorderPane rootPane;
    @FXML private TabPane tabPane;
    @FXML private StackPane browserContainer;
    @FXML private TextField addressBar;
    @FXML private Button backButton;
    @FXML private Button forwardButton;
    @FXML private Button reloadButton;
    @FXML private Button homeButton;
    @FXML private Button newTabButton;
    @FXML private Button bookmarkButton;
    @FXML private Button downloadsButton;
    @FXML private Button menuButton;
    @FXML private Label securityIcon;
    @FXML private Label statusLabel;
    @FXML private HBox statusBar;

    public void setContainer(DIContainer container) {
        this.container = container;
    }

    @FXML
    public void initialize() {
        System.out.println("MainController.initialize() called");

        // Initialize services
        tabService = container.getOrCreate(TabService.class);
        bookmarkService = container.getOrCreate(BookmarkService.class);
        historyService = container.getOrCreate(HistoryService.class);
        settingsService = container.getOrCreate(SettingsService.class);
        shortcutManager = new KeyboardShortcutManager(this);

        // Set up icons
        setupIcons();

        // Set up address bar
        addressBar.setOnAction(e -> navigateToUrl(addressBar.getText()));

        // Set up tab selection listener - THIS IS THE KEY FIX
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                showBrowserForTab(newTab);
            }
        });

        // Handle tab close requests
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);

        // Load initial tab
        Platform.runLater(() -> {
            String homePage = settingsService.getHomePage();
            createNewTab(homePage);
        });

        System.out.println("MainController initialized successfully");
        logger.info("Main controller initialized");
    }

    private void setupIcons() {
        // Navigation buttons
        backButton.setGraphic(new FontIcon("mdi2a-arrow-left"));
        forwardButton.setGraphic(new FontIcon("mdi2a-arrow-right"));
        reloadButton.setGraphic(new FontIcon("mdi2r-refresh"));
        homeButton.setGraphic(new FontIcon("mdi2h-home"));
        newTabButton.setGraphic(new FontIcon("mdi2p-plus"));
        menuButton.setGraphic(new FontIcon("mdi2m-menu"));

        if (bookmarkButton != null) {
            bookmarkButton.setGraphic(new FontIcon("mdi2b-bookmark-outline"));
        }
        if (downloadsButton != null) {
            downloadsButton.setGraphic(new FontIcon("mdi2d-download"));
        }
        if (securityIcon != null) {
            securityIcon.setGraphic(new FontIcon("mdi2l-lock"));
        }
    }

    /**
     * Show the browser view for the selected tab
     */
    private void showBrowserForTab(javafx.scene.control.Tab tab) {
        BrowserTab browserTab = tabBrowserMap.get(tab);
        if (browserTab != null) {
            // Clear the container and add the browser view
            browserContainer.getChildren().clear();
            browserContainer.getChildren().add(browserTab);

            // Update address bar
            addressBar.setText(browserTab.getUrl());

            // Update security icon based on URL
            updateSecurityIcon(browserTab.getUrl());
        }
    }

    private void updateSecurityIcon(String url) {
        if (securityIcon != null && securityIcon.getGraphic() instanceof FontIcon) {
            FontIcon icon = (FontIcon) securityIcon.getGraphic();
            if (url != null && url.startsWith("https://")) {
                icon.setIconLiteral("mdi2l-lock");
                icon.setStyle("-fx-fill: green;");
            } else {
                icon.setIconLiteral("mdi2l-lock-open-outline");
                icon.setStyle("-fx-fill: gray;");
            }
        }
    }

    /**
     * Create a new tab with the given URL
     */
    private void createNewTab(String url) {
        System.out.println("Creating new tab with URL: " + url);

        // Create the browser tab component
        BrowserTab browserTab = new BrowserTab(container, url);

        // Create a JavaFX tab
        javafx.scene.control.Tab tab = new javafx.scene.control.Tab();
        tab.setText("New Tab");
        tab.setClosable(true);

        // Create custom tab graphic with close button
        HBox tabHeader = createTabHeader(tab, browserTab);
        tab.setGraphic(tabHeader);
        tab.setText(null); // We use graphic instead

        // Store the mapping
        tabBrowserMap.put(tab, browserTab);

        // Handle tab close
        tab.setOnCloseRequest(event -> {
            handleTabClose(tab);
        });

        // Add tab to pane and select it
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

        // Save to database
        Tab tabModel = new Tab(url);
        tabService.saveTab(tabModel);
        browserTab.setTabModel(tabModel);

        // Set up listeners for title and URL changes
        browserTab.titleProperty().addListener((obs, oldTitle, newTitle) -> {
            Platform.runLater(() -> {
                updateTabTitle(tab, newTitle);
                if (tabModel != null) {
                    tabModel.setTitle(newTitle);
                    tabService.updateTab(tabModel);
                }
            });
        });

        browserTab.urlProperty().addListener((obs, oldUrl, newUrl) -> {
            Platform.runLater(() -> {
                // Update address bar if this is the selected tab
                if (tabPane.getSelectionModel().getSelectedItem() == tab) {
                    addressBar.setText(newUrl);
                    updateSecurityIcon(newUrl);
                }
                if (tabModel != null) {
                    tabModel.setUrl(newUrl);
                    tabService.updateTab(tabModel);
                }
                // Add to history
                if (newUrl != null && !newUrl.isEmpty()) {
                    historyService.addToHistory(newUrl, browserTab.getTitle());
                }
            });
        });
    }

    /**
     * Create a custom tab header with icon, title, and close button
     */
    private HBox createTabHeader(javafx.scene.control.Tab tab, BrowserTab browserTab) {
        HBox header = new HBox(5);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setStyle("-fx-padding: 2 0 2 0;");

        // Favicon
        Label favicon = new Label();
        favicon.setGraphic(new FontIcon("mdi2w-web"));
        favicon.getGraphic().setStyle("-fx-font-size: 14px;");

        // Title label
        Label titleLabel = new Label("New Tab");
        titleLabel.setMaxWidth(120);
        titleLabel.setStyle("-fx-font-size: 12px;");

        // Close button
        Button closeBtn = new Button();
        closeBtn.setGraphic(new FontIcon("mdi2c-close"));
        closeBtn.getGraphic().setStyle("-fx-font-size: 12px;");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-padding: 2; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> {
            e.consume();
            handleTabClose(tab);
        });

        // Hover effect for close button
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #e0e0e0; -fx-padding: 2; -fx-cursor: hand; -fx-background-radius: 50%;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-padding: 2; -fx-cursor: hand;"));

        header.getChildren().addAll(favicon, titleLabel, closeBtn);

        // Store reference for title updates
        header.setUserData(titleLabel);

        return header;
    }

    /**
     * Update the title displayed in the tab header
     */
    private void updateTabTitle(javafx.scene.control.Tab tab, String title) {
        if (tab.getGraphic() instanceof HBox) {
            HBox header = (HBox) tab.getGraphic();
            Label titleLabel = (Label) header.getUserData();
            if (titleLabel != null && title != null) {
                String displayTitle = title.length() > 20 ? title.substring(0, 17) + "..." : title;
                titleLabel.setText(displayTitle);
                titleLabel.setTooltip(new Tooltip(title));
            }
        }
    }

    /**
     * Handle tab close request
     */
    private void handleTabClose(javafx.scene.control.Tab tab) {
        BrowserTab browserTab = tabBrowserMap.get(tab);

        // Remove from map
        tabBrowserMap.remove(tab);

        // Remove from TabPane
        tabPane.getTabs().remove(tab);

        // Delete from database
        if (browserTab != null && browserTab.getTabModel() != null) {
            tabService.deleteTab(browserTab.getTabModel().getId());
        }

        // Dispose browser resources
        if (browserTab != null) {
            browserTab.dispose();
        }

        // If no tabs left, create a new one
        if (tabPane.getTabs().isEmpty()) {
            createNewTab(settingsService.getHomePage());
        }
    }

    private BrowserTab getCurrentBrowserTab() {
        javafx.scene.control.Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            return tabBrowserMap.get(selectedTab);
        }
        return null;
    }

    private void navigateToUrl(String url) {
        BrowserTab currentTab = getCurrentBrowserTab();
        if (currentTab != null) {
            String processedUrl = processUrl(url);
            currentTab.loadUrl(processedUrl);
        }
    }

    private String processUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return settingsService.getHomePage();
        }

        url = url.trim();

        // Check if it's a search query (no dots or protocol)
        if (!url.contains("://") && !url.contains(".")) {
            String searchEngine = settingsService.getSearchEngine();
            return getSearchEngineUrl(searchEngine) + url;
        }

        // Add https:// if no protocol specified
        if (!url.contains("://")) {
            return "https://" + url;
        }

        return url;
    }

    private String getSearchEngineUrl(String searchEngine) {
        return switch (searchEngine.toLowerCase()) {
            case "bing" -> "https://www.bing.com/search?q=";
            case "duckduckgo" -> "https://www.duckduckgo.com/?q=";
            default -> "https://www.google.com/search?q=";
        };
    }

    // Navigation handlers
    @FXML
    private void handleBack() {
        BrowserTab currentTab = getCurrentBrowserTab();
        if (currentTab != null) {
            currentTab.goBack();
        }
    }

    @FXML
    private void handleForward() {
        BrowserTab currentTab = getCurrentBrowserTab();
        if (currentTab != null) {
            currentTab.goForward();
        }
    }

    @FXML
    public void handleReload() {
        BrowserTab currentTab = getCurrentBrowserTab();
        if (currentTab != null) {
            currentTab.reload();
        }
    }

    @FXML
    private void handleHome() {
        navigateToUrl(settingsService.getHomePage());
    }

    @FXML
    public void handleNewTab() {
        createNewTab(settingsService.getHomePage());
    }

    @FXML
    private void handleMenu() {
        showMainMenu();
    }

    private void showMainMenu() {
        ContextMenu menu = new ContextMenu();

        MenuItem newTab = new MenuItem("New Tab");
        newTab.setGraphic(new FontIcon("mdi2t-tab-plus"));
        newTab.setOnAction(e -> handleNewTab());

        MenuItem newWindow = new MenuItem("New Window");
        newWindow.setGraphic(new FontIcon("mdi2w-window-maximize"));
        newWindow.setOnAction(e -> handleNewWindow());

        MenuItem history = new MenuItem("History");
        history.setGraphic(new FontIcon("mdi2h-history"));
        history.setOnAction(e -> handleShowHistory());

        MenuItem downloads = new MenuItem("Downloads");
        downloads.setGraphic(new FontIcon("mdi2d-download"));
        downloads.setOnAction(e -> handleShowDownloads());

        MenuItem bookmarks = new MenuItem("Bookmarks");
        bookmarks.setGraphic(new FontIcon("mdi2b-bookmark-multiple"));
        bookmarks.setOnAction(e -> handleShowBookmarks());

        MenuItem settings = new MenuItem("Settings");
        settings.setGraphic(new FontIcon("mdi2c-cog"));
        settings.setOnAction(e -> handleShowSettings());

        MenuItem about = new MenuItem("About");
        about.setGraphic(new FontIcon("mdi2i-information"));
        about.setOnAction(e -> handleAbout());

        MenuItem exit = new MenuItem("Exit");
        exit.setGraphic(new FontIcon("mdi2e-exit-to-app"));
        exit.setOnAction(e -> handleExit());

        menu.getItems().addAll(
            newTab, newWindow,
            new SeparatorMenuItem(),
            history, downloads, bookmarks,
            new SeparatorMenuItem(),
            settings, about,
            new SeparatorMenuItem(),
            exit
        );

        // Show menu below the menu button
        menu.show(menuButton, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    @FXML
    public void handleBookmarkCurrentPage() {
        BrowserTab currentTab = getCurrentBrowserTab();
        if (currentTab != null) {
            String url = currentTab.getUrl();
            String title = currentTab.getTitle();

            // Create and save bookmark
            com.example.nexus.model.Bookmark bookmark = new com.example.nexus.model.Bookmark();
            bookmark.setUrl(url);
            bookmark.setTitle(title);
            bookmarkService.saveBookmark(bookmark);

            // Visual feedback
            if (bookmarkButton != null) {
                ((FontIcon) bookmarkButton.getGraphic()).setIconLiteral("mdi2b-bookmark");
            }
        }
    }

    @FXML
    public void handleShowDownloads() {
        showNotImplementedAlert("Downloads");
    }

    @FXML
    public void handleShowHistory() {
        showNotImplementedAlert("History");
    }

    @FXML
    public void handleShowBookmarks() {
        showNotImplementedAlert("Bookmarks");
    }

    @FXML
    public void handleShowSettings() {
        showNotImplementedAlert("Settings");
    }

    @FXML
    public void handleNewWindow() {
        showNotImplementedAlert("New Window");
    }

    @FXML
    public void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Nexus Browser");
        alert.setHeaderText("Nexus Browser v1.0.0");
        alert.setContentText("A modern web browser built with JavaFX and JCEF.\n\n© 2024");
        alert.showAndWait();
    }

    @FXML
    public void handleExit() {
        Platform.exit();
    }

    @FXML
    public void handleFind() {
        showNotImplementedAlert("Find");
    }

    @FXML
    public void handlePrint() {
        BrowserTab currentTab = getCurrentBrowserTab();
        if (currentTab != null) {
            currentTab.print();
        }
    }

    @FXML
    public void handleZoomIn() {
        BrowserTab currentTab = getCurrentBrowserTab();
        if (currentTab != null) {
            currentTab.zoomIn();
        }
    }

    @FXML
    public void handleZoomOut() {
        BrowserTab currentTab = getCurrentBrowserTab();
        if (currentTab != null) {
            currentTab.zoomOut();
        }
    }

    @FXML
    public void handleFullScreen() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setFullScreen(!stage.isFullScreen());
    }

    @FXML
    public void handleShowDeveloperTools() {
        BrowserTab currentTab = getCurrentBrowserTab();
        if (currentTab != null) {
            currentTab.showDevTools();
        }
    }

    @FXML
    public void handleDuplicateTab() {
        BrowserTab currentTab = getCurrentBrowserTab();
        if (currentTab != null) {
            createNewTab(currentTab.getUrl());
        }
    }

    @FXML
    public void handlePinTab() {
        showNotImplementedAlert("Pin Tab");
    }

    @FXML
    public void handleGroupTab() {
        showNotImplementedAlert("Group Tab");
    }

    @FXML
    public void handleCloseTab() {
        javafx.scene.control.Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            handleTabClose(selectedTab);
        }
    }

    @FXML
    public void handleNewPrivateWindow() {
        showNotImplementedAlert("Private Window");
    }

    private void showNotImplementedAlert(String feature) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText(feature);
        alert.setContentText("This feature is not yet implemented.");
        alert.showAndWait();
    }

    public void setupKeyboardShortcuts(Scene scene) {
        if (shortcutManager != null) {
            shortcutManager.setupForScene(scene);
        }
    }

    public void handleResetZoom() {
        BrowserTab currentTab = getCurrentBrowserTab();
        if (currentTab != null) {
            currentTab.resetZoom();
        }
    }

    public void saveCurrentSession() {
        // Save all open tabs
        tabService.deleteTabsBySessionId("previous_session");

        int position = 0;
        for (javafx.scene.control.Tab tab : tabPane.getTabs()) {
            BrowserTab browserTab = tabBrowserMap.get(tab);
            if (browserTab != null) {
                Tab sessionTab = new Tab();
                sessionTab.setUrl(browserTab.getUrl());
                sessionTab.setTitle(browserTab.getTitle());
                sessionTab.setPosition(position++);
                sessionTab.setSessionId("previous_session");
                tabService.saveTab(sessionTab);
            }
        }
    }
}

