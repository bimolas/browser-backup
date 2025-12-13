package com.example.nexus.controller;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Tab;
import com.example.nexus.service.*;
import com.example.nexus.util.KeyboardShortcutManager;
import com.example.nexus.view.components.BrowserTab;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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

        // Disable default close buttons - we use custom ones
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Remove focus traversable to prevent focus ring
        tabPane.setFocusTraversable(false);

        // Hide the tab dropdown arrow button programmatically
        Platform.runLater(() -> hideTabDropdownButton());

        // Apply saved theme on startup
        Platform.runLater(() -> applyTheme(settingsService.getTheme()));

        // Load initial tab
        Platform.runLater(() -> {
            String homePage = settingsService.getHomePage();
            createNewTab(homePage);
        });

        System.out.println("MainController initialized successfully");
        logger.info("Main controller initialized");
    }

    /**
     * Hide the dropdown arrow button that appears in the tab pane header
     */
    private void hideTabDropdownButton() {
        // Find and hide the control buttons (dropdown arrow)
        tabPane.lookupAll(".control-buttons-tab").forEach(node -> {
            node.setVisible(false);
            node.setManaged(false);
        });
        tabPane.lookupAll(".tab-down-button").forEach(node -> {
            node.setVisible(false);
            node.setManaged(false);
        });
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

            // Update bookmark button state
            updateBookmarkButtonState(browserTab.getUrl());
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

        // Add Ctrl+Scroll wheel zoom support
        setupScrollZoom(browserTab);

        // Create a JavaFX tab
        javafx.scene.control.Tab tab = new javafx.scene.control.Tab();
        tab.setText("New Tab");
        tab.setClosable(false); // Disable default close button - we use custom one

        // Create custom tab graphic with close button
        HBox tabHeader = createTabHeader(tab, browserTab);
        tab.setGraphic(tabHeader);
        tab.setText(null); // We use graphic instead

        // Store the mapping
        tabBrowserMap.put(tab, browserTab);

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
                    updateBookmarkButtonState(newUrl);
                }
                if (tabModel != null) {
                    tabModel.setUrl(newUrl);
                    tabService.updateTab(tabModel);
                }
                // Add to history - track every page visit
                if (newUrl != null && !newUrl.isEmpty() &&
                    !newUrl.startsWith("about:") && !newUrl.startsWith("data:")) {
                    try {
                        String pageTitle = browserTab.getTitle();
                        if (pageTitle == null || pageTitle.isEmpty()) {
                            pageTitle = newUrl;
                        }
                        historyService.addToHistory(newUrl, pageTitle);
                        logger.info("Added to history: {} - {}", pageTitle, newUrl);
                    } catch (Exception e) {
                        logger.error("Failed to add to history: {}", newUrl, e);
                    }
                }
            });
        });
    }

    /**
     * Create a custom tab header with icon, title, and close button
     */
    private HBox createTabHeader(javafx.scene.control.Tab tab, BrowserTab browserTab) {
        HBox header = new HBox(4);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setFocusTraversable(false);

        // Fixed width for consistent tab sizing - prevents breaking
        header.setPrefWidth(140);
        header.setMinWidth(80);
        header.setMaxWidth(180);
        header.setStyle("-fx-padding: 0 4 0 4;");

        // Favicon - starts with default icon, will be updated when page loads
        javafx.scene.image.ImageView faviconView = new javafx.scene.image.ImageView();
        faviconView.setFitWidth(14);
        faviconView.setFitHeight(14);
        faviconView.setPreserveRatio(true);
        faviconView.setSmooth(true);

        // Default icon when no favicon is available
        FontIcon defaultIcon = new FontIcon("mdi2w-web");
        defaultIcon.setIconSize(14);
        defaultIcon.setIconColor(javafx.scene.paint.Color.valueOf("#6c757d"));

        StackPane faviconContainer = new StackPane();
        faviconContainer.setPrefSize(14, 14);
        faviconContainer.setMinSize(14, 14);
        faviconContainer.setMaxSize(14, 14);
        faviconContainer.getChildren().add(defaultIcon);
        faviconContainer.setFocusTraversable(false);

        // Title label with ellipsis for long titles - FIXED WIDTH
        Label titleLabel = new Label("New Tab");
        titleLabel.setPrefWidth(90);
        titleLabel.setMinWidth(30);
        titleLabel.setMaxWidth(110);
        titleLabel.setStyle("-fx-font-size: 11px; -fx-text-overrun: ellipsis;");
        titleLabel.setFocusTraversable(false);
        titleLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        titleLabel.setEllipsisString("...");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        // Tooltip to show full title on hover
        Tooltip titleTooltip = new Tooltip("New Tab");
        titleTooltip.setShowDelay(javafx.util.Duration.millis(500));
        titleTooltip.setStyle("-fx-font-size: 12px;");
        Tooltip.install(titleLabel, titleTooltip);

        // Listen for favicon changes
        browserTab.faviconUrlProperty().addListener((obs, oldUrl, newUrl) -> {
            Platform.runLater(() -> {
                if (newUrl != null && !newUrl.isEmpty()) {
                    try {
                        javafx.scene.image.Image favicon = new javafx.scene.image.Image(newUrl, 14, 14, true, true, true);
                        favicon.progressProperty().addListener((o, oldP, newP) -> {
                            if (newP.doubleValue() >= 1.0 && !favicon.isError()) {
                                Platform.runLater(() -> {
                                    faviconView.setImage(favicon);
                                    faviconContainer.getChildren().clear();
                                    faviconContainer.getChildren().add(faviconView);
                                });
                            }
                        });
                    } catch (Exception e) {
                        // Keep default icon
                        logger.debug("Failed to load favicon: {}", newUrl);
                    }
                }
            });
        });

        // Close button with proper styling - gray by default, FIXED SIZE
        Button closeBtn = new Button();
        FontIcon closeIcon = new FontIcon("mdi2c-close");
        closeIcon.setIconSize(10);
        closeIcon.setIconColor(javafx.scene.paint.Color.valueOf("#6c757d"));
        closeBtn.setGraphic(closeIcon);
        closeBtn.getStyleClass().add("tab-close-btn");
        closeBtn.setFocusTraversable(false);
        closeBtn.setPrefSize(16, 16);
        closeBtn.setMinSize(16, 16);
        closeBtn.setMaxSize(16, 16);
        closeBtn.setStyle("-fx-background-color: transparent; -fx-padding: 2; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> {
            e.consume();
            handleTabClose(tab);
        });

        // Hover effect for close button - gray background on hover
        closeBtn.setOnMouseEntered(e ->
            closeBtn.setStyle("-fx-background-color: rgba(108, 117, 125, 0.25); -fx-background-radius: 50%; -fx-padding: 2; -fx-cursor: hand;")
        );
        closeBtn.setOnMouseExited(e ->
            closeBtn.setStyle("-fx-background-color: transparent; -fx-padding: 2; -fx-cursor: hand;")
        );

        header.getChildren().addAll(faviconContainer, titleLabel, closeBtn);

        // Store references for updates
        header.setUserData(new Object[]{titleLabel, titleTooltip, faviconContainer, faviconView, defaultIcon});

        // Add tab preview popup on hover
        setupTabPreview(tab, header, browserTab);

        return header;
    }

    /**
     * Setup tab preview popup that shows on hover (like Chrome)
     */
    private void setupTabPreview(javafx.scene.control.Tab tab, HBox header, BrowserTab browserTab) {
        javafx.stage.Popup previewPopup = new javafx.stage.Popup();
        previewPopup.setAutoHide(true);

        VBox previewContent = new VBox(5);
        previewContent.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);");
        previewContent.setPrefWidth(240);

        // Preview title
        Label previewTitle = new Label("New Tab");
        previewTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #212529;");
        previewTitle.setMaxWidth(220);
        previewTitle.setWrapText(false);
        previewTitle.setEllipsisString("...");

        // Preview URL
        Label previewUrl = new Label("");
        previewUrl.setStyle("-fx-font-size: 10px; -fx-text-fill: #6c757d;");
        previewUrl.setMaxWidth(220);
        previewUrl.setWrapText(false);
        previewUrl.setEllipsisString("...");

        // Preview image container
        javafx.scene.image.ImageView previewImage = new javafx.scene.image.ImageView();
        previewImage.setFitWidth(224);
        previewImage.setFitHeight(140);
        previewImage.setPreserveRatio(true);
        previewImage.setSmooth(true);
        previewImage.setStyle("-fx-background-color: #f8f9fa;");

        StackPane imageContainer = new StackPane(previewImage);
        imageContainer.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 4;");
        imageContainer.setPrefSize(224, 140);

        previewContent.getChildren().addAll(previewTitle, previewUrl, imageContainer);
        previewPopup.getContent().add(previewContent);

        // Timer for delayed popup show
        javafx.animation.PauseTransition hoverDelay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(400));
        final boolean[] isHovering = {false};

        header.setOnMouseEntered(e -> {
            isHovering[0] = true;
            hoverDelay.setOnFinished(event -> {
                if (isHovering[0] && tab != tabPane.getSelectionModel().getSelectedItem()) {
                    // Update preview content
                    previewTitle.setText(browserTab.getTitle());
                    previewUrl.setText(browserTab.getUrl());

                    // Get snapshot of the tab content
                    try {
                        javafx.scene.image.WritableImage snapshot = browserTab.getPreviewSnapshot();
                        if (snapshot != null) {
                            previewImage.setImage(snapshot);
                        }
                    } catch (Exception ex) {
                        logger.debug("Could not get tab preview snapshot");
                    }

                    // Show popup below the tab
                    javafx.geometry.Bounds bounds = header.localToScreen(header.getBoundsInLocal());
                    if (bounds != null) {
                        previewPopup.show(header, bounds.getMinX(), bounds.getMaxY() + 5);
                    }
                }
            });
            hoverDelay.playFromStart();
        });

        header.setOnMouseExited(e -> {
            isHovering[0] = false;
            hoverDelay.stop();
            previewPopup.hide();
        });
    }

    /**
     * Update the title displayed in the tab header
     */
    private void updateTabTitle(javafx.scene.control.Tab tab, String title) {
        if (tab.getGraphic() instanceof HBox header) {
            Object userData = header.getUserData();
            if (userData instanceof Object[] data && data.length >= 2) {
                Label titleLabel = (Label) data[0];
                Tooltip titleTooltip = (Tooltip) data[1];

                if (titleLabel != null && title != null) {
                    // Set the label text (will auto-truncate with ellipsis due to maxWidth)
                    titleLabel.setText(title);

                    // Update tooltip to show full title
                    if (titleTooltip != null) {
                        titleTooltip.setText(title);
                    }
                }
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

        // Apply clean styling to the menu
        menu.setStyle(
            "-fx-background-color: #ffffff;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 8;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 16, 0, 0, 4);" +
            "-fx-border-color: #e5e7eb;" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1;"
        );

        // === Navigation Section ===
        MenuItem newTab = createMenuItem("New Tab", "mdi2p-plus", "#3b82f6", "Ctrl+T");
        newTab.setOnAction(e -> handleNewTab());

        MenuItem newWindow = createMenuItem("New Window", "mdi2o-open-in-new", "#3b82f6", "Ctrl+N");
        newWindow.setOnAction(e -> handleNewWindow());

        // === Zoom Submenu ===
        Menu zoomMenu = new Menu("Zoom");
        FontIcon zoomIcon = new FontIcon("mdi2m-magnify");
        zoomIcon.setIconSize(16);
        zoomIcon.setIconColor(javafx.scene.paint.Color.web("#8b5cf6"));
        zoomMenu.setGraphic(zoomIcon);

        // Zoom items
        MenuItem zoomIn = createMenuItem("Zoom In", "mdi2p-plus", "#22c55e", "Ctrl++");
        zoomIn.setOnAction(e -> handleZoomIn());

        MenuItem zoomOut = createMenuItem("Zoom Out", "mdi2m-minus", "#ef4444", "Ctrl+-");
        zoomOut.setOnAction(e -> handleZoomOut());

        MenuItem zoomReset = createMenuItem("Reset (100%)", "mdi2b-backup-restore", "#64748b", "Ctrl+0");
        zoomReset.setOnAction(e -> handleZoomReset());

        // Zoom toggles
        CheckMenuItem magnifierMode = new CheckMenuItem("Magnifier Mode");
        magnifierMode.setSelected(viewportZoomMode);
        magnifierMode.setOnAction(e -> {
            viewportZoomMode = magnifierMode.isSelected();
            showZoomModeNotification();
        });

        CheckMenuItem mouseNav = new CheckMenuItem("Mouse Navigation");
        mouseNav.setSelected(mouseTrackingEnabled);
        mouseNav.setOnAction(e -> {
            mouseTrackingEnabled = mouseNav.isSelected();
            showMouseTrackingNotification();
        });

        zoomMenu.getItems().addAll(
            zoomIn, zoomOut,
            new SeparatorMenuItem(),
            zoomReset,
            new SeparatorMenuItem(),
            magnifierMode, mouseNav
        );

        // === Tools Section ===
        MenuItem history = createMenuItem("History", "mdi2h-history", "#f59e0b", "Ctrl+H");
        history.setOnAction(e -> handleShowHistory());

        MenuItem downloads = createMenuItem("Downloads", "mdi2d-download", "#10b981", "Ctrl+J");
        downloads.setOnAction(e -> handleShowDownloads());

        MenuItem bookmarks = createMenuItem("Bookmarks", "mdi2b-bookmark-outline", "#ec4899", "Ctrl+B");
        bookmarks.setOnAction(e -> handleShowBookmarks());

        // === Settings Section ===
        MenuItem settings = createMenuItem("Settings", "mdi2c-cog-outline", "#64748b", null);
        settings.setOnAction(e -> handleShowSettings());

        MenuItem about = createMenuItem("About", "mdi2i-information-outline", "#64748b", null);
        about.setOnAction(e -> handleAbout());

        // === Exit ===
        MenuItem exit = createMenuItem("Exit", "mdi2l-logout", "#ef4444", null);
        exit.setOnAction(e -> handleExit());

        // Build menu with separators
        menu.getItems().addAll(
            newTab, newWindow,
            new SeparatorMenuItem(),
            zoomMenu,
            new SeparatorMenuItem(),
            history, downloads, bookmarks,
            new SeparatorMenuItem(),
            settings, about,
            new SeparatorMenuItem(),
            exit
        );

        // Show menu below the menu button
        menu.show(menuButton, javafx.geometry.Side.BOTTOM, -160, 5);
    }

    /**
     * Create a menu item with icon and optional shortcut
     */
    private MenuItem createMenuItem(String text, String iconCode, String iconColor, String shortcut) {
        MenuItem item = new MenuItem(text);

        // Create colored icon
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(16);
        icon.setIconColor(javafx.scene.paint.Color.web(iconColor));
        item.setGraphic(icon);

        // Set accelerator if provided
        if (shortcut != null) {
            try {
                item.setAccelerator(javafx.scene.input.KeyCombination.keyCombination(
                    shortcut.replace("++", "+PLUS").replace("+-", "+MINUS")
                ));
            } catch (Exception e) {
                // Ignore invalid accelerator
            }
        }

        return item;
    }


    // Zoom level tracking
    private double currentZoom = 1.0;          // CSS zoom (makes elements bigger)
    private double viewportZoom = 1.0;         // Viewport zoom (magnifier style)
    private static final double ZOOM_STEP = 0.1;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 5.0;
    private static final double VIEWPORT_ZOOM_STEP = 0.2;
    private static final double MIN_VIEWPORT_ZOOM = 1.0;  // Can't go below 100% in magnifier mode
    private static final double MAX_VIEWPORT_ZOOM = 5.0;

    // Mouse tracking zoom feature - toggle with Alt+M key
    private boolean mouseTrackingEnabled = false;

    // Which zoom mode is active: false = CSS zoom, true = viewport zoom (magnifier)
    private boolean viewportZoomMode = true;

    /**
     * Handle zoom in
     */
    public void handleZoomIn() {
        BrowserTab currentTab = getCurrentBrowserTab();
        if (currentTab != null) {
            if (viewportZoomMode) {
                // Viewport zoom (magnifier style) - starts at 100%, zooms in only
                viewportZoom = Math.min(MAX_VIEWPORT_ZOOM, viewportZoom + VIEWPORT_ZOOM_STEP);
                currentTab.setViewportZoom(viewportZoom);
            } else {
                // CSS zoom (makes elements bigger)
                currentZoom = Math.min(MAX_ZOOM, currentZoom + ZOOM_STEP);
                currentTab.setZoomLevel(currentZoom);
            }
            showZoomNotification();
        }
    }

    /**
     * Handle zoom out
     */
    public void handleZoomOut() {
        BrowserTab currentTab = getCurrentBrowserTab();
        if (currentTab != null) {
            if (viewportZoomMode) {
                // Viewport zoom - cannot go below 100% (breaks view)
                double newZoom = viewportZoom - VIEWPORT_ZOOM_STEP;
                if (newZoom < MIN_VIEWPORT_ZOOM) {
                    // Just reset to 100% instead of going below
                    viewportZoom = MIN_VIEWPORT_ZOOM;
                } else {
                    viewportZoom = newZoom;
                }
                currentTab.setViewportZoom(viewportZoom);
            } else {
                // CSS zoom (can go below 100%)
                currentZoom = Math.max(MIN_ZOOM, currentZoom - ZOOM_STEP);
                currentTab.setZoomLevel(currentZoom);
            }
            showZoomNotification();
        }
    }

    /**
     * Handle zoom reset to 100%
     */
    public void handleZoomReset() {
        BrowserTab currentTab = getCurrentBrowserTab();
        if (currentTab != null) {
            // Reset both zoom modes
            if (viewportZoomMode) {
                viewportZoom = 1.0;
                currentTab.setViewportZoom(viewportZoom);
            } else {
                currentZoom = 1.0;
                currentTab.setZoomLevel(currentZoom);
            }
            showZoomNotification();
        }
    }

    /**
     * Toggle between CSS zoom and viewport zoom modes
     */
    public void toggleZoomMode() {
        viewportZoomMode = !viewportZoomMode;
        showZoomModeNotification();
    }

    /**
     * Show zoom mode notification
     */
    private void showZoomModeNotification() {
        javafx.stage.Popup popup = new javafx.stage.Popup();

        String mode = viewportZoomMode ? "Magnifier Zoom" : "Page Zoom";
        String iconCode = viewportZoomMode ? "mdi2m-magnify-scan" : "mdi2m-magnify";

        HBox content = new HBox(8);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.setStyle("-fx-background-color: rgba(33, 33, 33, 0.9); -fx-padding: 10 16; -fx-background-radius: 6;");

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(16);
        icon.setIconColor(javafx.scene.paint.Color.WHITE);

        Label label = new Label("Zoom Mode: " + mode);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");

        content.getChildren().addAll(icon, label);
        popup.getContent().add(content);
        popup.setAutoHide(true);

        Platform.runLater(() -> {
            if (rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
                javafx.stage.Window window = rootPane.getScene().getWindow();
                double centerX = window.getX() + window.getWidth() / 2 - 90;
                double centerY = window.getY() + window.getHeight() / 2 - 20;
                popup.show(window, centerX, centerY);

                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1500));
                pause.setOnFinished(e -> popup.hide());
                pause.play();
            }
        });
    }

    /**
     * Toggle mouse tracking for zoom navigation
     */
    public void toggleMouseTracking() {
        mouseTrackingEnabled = !mouseTrackingEnabled;
        showMouseTrackingNotification();
    }

    /**
     * Show mouse tracking status notification
     */
    private void showMouseTrackingNotification() {
        javafx.stage.Popup popup = new javafx.stage.Popup();

        String status = mouseTrackingEnabled ? "Mouse Tracking: ON" : "Mouse Tracking: OFF";
        String iconCode = mouseTrackingEnabled ? "mdi2c-crosshairs-gps" : "mdi2c-crosshairs-off";

        HBox content = new HBox(8);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.setStyle("-fx-background-color: rgba(33, 33, 33, 0.9); -fx-padding: 10 16; -fx-background-radius: 6;");

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(16);
        icon.setIconColor(javafx.scene.paint.Color.WHITE);

        Label label = new Label(status);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");

        content.getChildren().addAll(icon, label);
        popup.getContent().add(content);
        popup.setAutoHide(true);

        Platform.runLater(() -> {
            if (rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
                javafx.stage.Window window = rootPane.getScene().getWindow();
                double centerX = window.getX() + window.getWidth() / 2 - 80;
                double centerY = window.getY() + window.getHeight() / 2 - 20;
                popup.show(window, centerX, centerY);

                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1500));
                pause.setOnFinished(e -> popup.hide());
                pause.play();
            }
        });
    }

    /**
     * Show a brief notification of current zoom level
     */
    private void showZoomNotification() {
        double zoom = viewportZoomMode ? viewportZoom : currentZoom;
        int zoomPercent = (int) Math.round(zoom * 100);

        // Create a popup notification
        javafx.stage.Popup popup = new javafx.stage.Popup();

        String modeIndicator = viewportZoomMode ? "🔍 " : "";
        Label zoomLabel = new Label(modeIndicator + zoomPercent + "%");
        zoomLabel.setStyle("-fx-background-color: rgba(33, 33, 33, 0.9); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 6;");

        popup.getContent().add(zoomLabel);
        popup.setAutoHide(true);

        // Position in center of browser
        Platform.runLater(() -> {
            if (rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
                javafx.stage.Window window = rootPane.getScene().getWindow();
                double centerX = window.getX() + window.getWidth() / 2 - 40;
                double centerY = window.getY() + window.getHeight() / 2 - 20;
                popup.show(window, centerX, centerY);

                // Auto-hide after 1.5 seconds
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1500));
                pause.setOnFinished(e -> popup.hide());
                pause.play();
            }
        });
    }

    /**
     * Setup Ctrl+Scroll wheel zoom for a browser tab
     * Also sets up mouse tracking for smooth navigation when zoomed
     */
    private void setupScrollZoom(BrowserTab browserTab) {
        // Track mouse position for navigation when zoomed (viewport zoom only)
        browserTab.setOnMouseMoved(event -> {
            // If mouse tracking is enabled and viewport zoomed in, scroll to follow mouse
            if (mouseTrackingEnabled && viewportZoomMode && viewportZoom > 1.0) {
                double relX = event.getX() / browserTab.getWidth();
                double relY = event.getY() / browserTab.getHeight();
                browserTab.scrollViewportSmooth(relX, relY);
            }
        });

        // Keyboard shortcuts: Alt+M for mouse tracking, Alt+Z for zoom mode, Ctrl+0 for reset
        browserTab.setOnKeyPressed(event -> {
            if (event.isAltDown() && event.getCode() == javafx.scene.input.KeyCode.M) {
                toggleMouseTracking();
                event.consume();
            } else if (event.isAltDown() && event.getCode() == javafx.scene.input.KeyCode.Z) {
                toggleZoomMode();
                event.consume();
            } else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.DIGIT0) {
                // Ctrl+0 to reset zoom
                handleZoomReset();
                event.consume();
            } else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.NUMPAD0) {
                // Ctrl+Numpad0 to reset zoom
                handleZoomReset();
                event.consume();
            } else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.EQUALS) {
                // Ctrl+= (Ctrl+Plus on US keyboard) to zoom in
                handleZoomIn();
                event.consume();
            } else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.MINUS) {
                // Ctrl+- to zoom out
                handleZoomOut();
                event.consume();
            } else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.ADD) {
                // Ctrl+Numpad+ to zoom in
                handleZoomIn();
                event.consume();
            } else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.SUBTRACT) {
                // Ctrl+Numpad- to zoom out
                handleZoomOut();
                event.consume();
            }
        });

        // Ctrl+Scroll for zoom
        browserTab.setOnScroll(event -> {
            // Only zoom if Ctrl is held
            if (event.isControlDown()) {
                event.consume();

                double delta = event.getDeltaY();

                if (viewportZoomMode) {
                    // Viewport zoom (magnifier style) - can only zoom in, not below 100%
                    if (delta > 0) {
                        // Zoom in
                        viewportZoom = Math.min(MAX_VIEWPORT_ZOOM, viewportZoom + VIEWPORT_ZOOM_STEP);
                        browserTab.setViewportZoom(viewportZoom);
                        showZoomNotification();
                    } else if (delta < 0 && viewportZoom > MIN_VIEWPORT_ZOOM) {
                        // Zoom out - but not below 100%
                        viewportZoom = Math.max(MIN_VIEWPORT_ZOOM, viewportZoom - VIEWPORT_ZOOM_STEP);
                        browserTab.setViewportZoom(viewportZoom);
                        showZoomNotification();
                    }
                } else {
                    // CSS zoom (makes elements bigger) - can go below 100%
                    if (delta > 0) {
                        currentZoom = Math.min(MAX_ZOOM, currentZoom + ZOOM_STEP);
                    } else if (delta < 0) {
                        currentZoom = Math.max(MIN_ZOOM, currentZoom - ZOOM_STEP);
                    }
                    browserTab.setZoomLevel(currentZoom);
                    showZoomNotification();
                }
            }
        });
    }

    /**
     * Scroll the page to center on the mouse position when zoomed
     * This is now handled by BrowserTab.scrollViewportSmooth()
     */
    private void scrollToMousePosition(BrowserTab browserTab, double mouseX, double mouseY) {
        // Delegate to browser tab's viewport scroll
        double relX = mouseX / browserTab.getWidth();
        double relY = mouseY / browserTab.getHeight();
        browserTab.scrollViewportSmooth(relX, relY);
    }

    @FXML
    public void handleBookmarkCurrentPage() {
        BrowserTab currentTab = getCurrentBrowserTab();
        if (currentTab != null) {
            String url = currentTab.getUrl();
            String title = currentTab.getTitle();

            // Check if already bookmarked
            if (bookmarkService.isBookmarked(url)) {
                // Show option to edit or remove
                showBookmarkEditDialog(url, title);
            } else {
                // Show add bookmark dialog
                showAddBookmarkDialog(url, title);
            }
        }
    }

    private void showAddBookmarkDialog(String url, String title) {
        Dialog<com.example.nexus.model.Bookmark> dialog = new Dialog<>();
        dialog.setTitle("Add Bookmark");
        dialog.setHeaderText("Save this page to your bookmarks");

        // Style the dialog
        dialog.getDialogPane().setStyle("-fx-background-color: #ffffff;");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new javafx.geometry.Insets(20));

        TextField titleField = new TextField(title);
        titleField.setPromptText("Bookmark name");
        titleField.setPrefWidth(300);
        titleField.setStyle("-fx-background-radius: 6; -fx-padding: 8 12;");

        TextField urlField = new TextField(url);
        urlField.setPromptText("URL");
        urlField.setStyle("-fx-background-radius: 6; -fx-padding: 8 12;");

        // Folder selection
        ComboBox<com.example.nexus.model.BookmarkFolder> folderCombo = new ComboBox<>();
        folderCombo.setPromptText("Select folder (optional)");
        folderCombo.setPrefWidth(300);
        folderCombo.setStyle("-fx-background-radius: 6;");

        // Add "No folder" option first
        folderCombo.getItems().add(null);
        try {
            folderCombo.getItems().addAll(bookmarkService.getAllFolders());
        } catch (Exception e) {
            logger.error("Error loading folders", e);
        }

        folderCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(com.example.nexus.model.BookmarkFolder folder) {
                return folder == null ? "No Folder (Bookmarks Bar)" : folder.getName();
            }
            @Override
            public com.example.nexus.model.BookmarkFolder fromString(String string) { return null; }
        });

        CheckBox favoriteCheck = new CheckBox("Add to favorites");
        favoriteCheck.setStyle("-fx-font-size: 13px;");

        // Add new folder button
        Button newFolderBtn = new Button("New Folder");
        newFolderBtn.setGraphic(new FontIcon("mdi2f-folder-plus"));
        newFolderBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #0d6efd; -fx-cursor: hand;");
        newFolderBtn.setOnAction(e -> {
            TextInputDialog folderDialog = new TextInputDialog();
            folderDialog.setTitle("New Folder");
            folderDialog.setHeaderText("Create a new bookmark folder");
            folderDialog.setContentText("Folder name:");
            folderDialog.showAndWait().ifPresent(name -> {
                if (!name.trim().isEmpty()) {
                    try {
                        com.example.nexus.model.BookmarkFolder newFolder = bookmarkService.createFolder(name.trim());
                        folderCombo.getItems().add(newFolder);
                        folderCombo.setValue(newFolder);
                    } catch (Exception ex) {
                        logger.error("Error creating folder", ex);
                    }
                }
            });
        });

        grid.add(new Label("Name:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("URL:"), 0, 1);
        grid.add(urlField, 1, 1);
        grid.add(new Label("Folder:"), 0, 2);

        HBox folderRow = new HBox(10);
        folderRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        folderRow.getChildren().addAll(folderCombo, newFolderBtn);
        grid.add(folderRow, 1, 2);

        grid.add(favoriteCheck, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Style the save button
        dialog.getDialogPane().lookupButton(saveButtonType).setStyle(
            "-fx-background-color: #0d6efd; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 20;");

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                com.example.nexus.model.Bookmark bookmark = new com.example.nexus.model.Bookmark();
                bookmark.setTitle(titleField.getText().trim());
                bookmark.setUrl(urlField.getText().trim());
                com.example.nexus.model.BookmarkFolder selectedFolder = folderCombo.getValue();
                if (selectedFolder != null) {
                    bookmark.setFolderId(selectedFolder.getId());
                }
                bookmark.setFavorite(favoriteCheck.isSelected());
                return bookmark;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(bookmark -> {
            try {
                bookmarkService.saveBookmark(bookmark);

                // Update button visual
                if (bookmarkButton != null && bookmarkButton.getGraphic() instanceof FontIcon) {
                    ((FontIcon) bookmarkButton.getGraphic()).setIconLiteral("mdi2b-bookmark");
                    ((FontIcon) bookmarkButton.getGraphic()).setStyle("-fx-fill: #ffc107;");
                }

                // Show success message
                logger.info("Bookmark saved: {}", bookmark.getTitle());
            } catch (Exception e) {
                logger.error("Error saving bookmark", e);
                showErrorAlert("Error", "Failed to save bookmark: " + e.getMessage());
            }
        });
    }

    private void showBookmarkEditDialog(String url, String title) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Edit Bookmark");
        alert.setHeaderText("This page is already bookmarked");
        alert.setContentText("Would you like to remove it from bookmarks?");

        ButtonType removeButton = new ButtonType("Remove Bookmark");
        ButtonType editButton = new ButtonType("Edit");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(removeButton, editButton, cancelButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == removeButton) {
                try {
                    bookmarkService.getBookmarkByUrl(url).ifPresent(bookmark -> {
                        bookmarkService.deleteBookmark(bookmark.getId());
                        if (bookmarkButton != null && bookmarkButton.getGraphic() instanceof FontIcon) {
                            ((FontIcon) bookmarkButton.getGraphic()).setIconLiteral("mdi2b-bookmark-outline");
                            ((FontIcon) bookmarkButton.getGraphic()).setStyle("-fx-fill: #495057;");
                        }
                    });
                } catch (Exception e) {
                    logger.error("Error removing bookmark", e);
                }
            } else if (response == editButton) {
                handleShowBookmarks();
            }
        });
    }


    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateBookmarkButtonState(String url) {
        if (bookmarkButton != null && bookmarkButton.getGraphic() instanceof FontIcon) {
            FontIcon icon = (FontIcon) bookmarkButton.getGraphic();
            if (url != null && bookmarkService.isBookmarked(url)) {
                icon.setIconLiteral("mdi2b-bookmark");
                icon.setStyle("-fx-fill: #ffc107;");
            } else {
                icon.setIconLiteral("mdi2b-bookmark-outline");
                icon.setStyle("-fx-fill: #495057;");
            }
        }
    }

    @FXML
    public void handleShowDownloads() {
        showNotImplementedAlert("Downloads");
    }

    @FXML
    public void handleShowHistory() {
        try {
            com.example.nexus.view.dialogs.HistoryPanel historyPanel =
                new com.example.nexus.view.dialogs.HistoryPanel(container);
            historyPanel.setOnOpenUrl(url -> {
                createNewTab(url);
                historyPanel.close();
            });
            historyPanel.show();
        } catch (Exception e) {
            logger.error("Error opening history panel", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open history");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void handleShowBookmarks() {
        try {
            com.example.nexus.view.dialogs.BookmarkPanel bookmarkPanel =
                new com.example.nexus.view.dialogs.BookmarkPanel(container);
            bookmarkPanel.setOnOpenUrl(url -> {
                createNewTab(url);
            });
            bookmarkPanel.show();
        } catch (Exception e) {
            logger.error("Error opening bookmark panel", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open bookmarks");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void handleShowSettings() {
        try {
            // Create settings panel
            com.example.nexus.view.dialogs.SettingsPanel settingsPanel =
                new com.example.nexus.view.dialogs.SettingsPanel(settingsService);

            // Create a new Stage for settings - don't set owner to allow maximize on Linux
            javafx.stage.Stage settingsStage = new javafx.stage.Stage();
            settingsStage.setTitle("Settings");
            settingsStage.initModality(javafx.stage.Modality.NONE); // Allow independent window behavior

            // Create scene with size to show all content
            javafx.scene.Scene settingsScene = new javafx.scene.Scene(settingsPanel, 1000, 750);

            // Apply CSS safely
            var cssResource = getClass().getResource("/com/example/nexus/css/main.css");
            if (cssResource != null) {
                settingsScene.getStylesheets().add(cssResource.toExternalForm());
            }

            settingsStage.setScene(settingsScene);
            settingsStage.setMinWidth(750);
            settingsStage.setMinHeight(600);
            settingsStage.setResizable(true);

            // Set theme change callback
            settingsPanel.setOnThemeChange(theme -> applyTheme(theme));

            // Show the stage (use show() instead of showAndWait() to avoid blocking issues)
            settingsStage.show();
            settingsStage.toFront();

            logger.info("Settings dialog opened");
        } catch (Exception e) {
            logger.error("Error showing settings", e);
            showErrorAlert("Error", "Failed to open settings: " + e.getMessage());
        }
    }

    /**
     * Apply theme to the browser
     */
    private void applyTheme(String theme) {
        Platform.runLater(() -> {
            try {
                if (rootPane.getScene() != null) {
                    // Remove existing theme classes
                    rootPane.getStyleClass().removeAll("light", "dark");

                    String actualTheme = theme;
                    if ("system".equals(theme)) {
                        // Detect system theme (simplified - defaults to light)
                        actualTheme = "light";
                    }

                    // Add new theme class
                    rootPane.getStyleClass().add(actualTheme);

                    // Apply theme-specific styles
                    if ("dark".equals(actualTheme)) {
                        rootPane.setStyle("-fx-background-color: #1e1e1e;");
                    } else {
                        rootPane.setStyle("-fx-background-color: #ffffff;");
                    }

                    logger.info("Applied theme: " + theme);
                }
            } catch (Exception e) {
                logger.error("Error applying theme", e);
            }
        });
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

