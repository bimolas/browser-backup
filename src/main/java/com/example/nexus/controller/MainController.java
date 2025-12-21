package com.example.nexus.controller;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Tab;
import com.example.nexus.service.*;
import com.example.nexus.util.KeyboardShortcutManager;
import com.example.nexus.view.components.BrowserTab;
import com.example.nexus.view.components.DownloadDropdown;
import com.example.nexus.view.components.DownloadManagerPanel;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
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
    private SettingsController settingsController;
    private KeyboardShortcutManager shortcutManager;

    private final Map<javafx.scene.control.Tab, BrowserTab> tabBrowserMap = new HashMap<>();
    // Cleanup actions per tab (to remove listeners and free references)
    private final Map<javafx.scene.control.Tab, Runnable> tabCleanupMap = new HashMap<>();

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

    private DownloadDropdown downloadDropdown;

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

        // Initialize settings controller for theme management
        settingsController = new SettingsController(settingsService);
        settingsController.setUIComponents(rootPane, addressBar, tabPane, browserContainer, statusBar);

        // Set up icons
        setupIcons();

        // Initialize download dropdown
        try {
            var ds = container.getOrCreate(com.example.nexus.service.DownloadService.class);
            boolean isDarkTheme = "dark".equals(settingsService.getTheme()) || ("system".equals(settingsService.getTheme()) && settingsController.isSystemDark());
            DownloadController dc = container.getOrCreate(DownloadController.class);
            downloadDropdown = new DownloadDropdown(ds, dc, isDarkTheme, () -> {
                try { handleShowDownloads(); } catch (Exception ex) { logger.warn("Failed to open downloads panel", ex); }
            });
            // Auto-show dropdown briefly when a new download is added or updated
            ds.addListener(new com.example.nexus.service.DownloadListener() {
                @Override
                public void downloadAdded(com.example.nexus.model.Download download) {
                    Platform.runLater(() -> {
                        try {
                            updateDownloadsBadge();
                            if (downloadsButton != null && downloadsButton.getScene() != null) {
                                var bounds = downloadsButton.localToScene(downloadsButton.getBoundsInLocal());
                                Window w = downloadsButton.getScene().getWindow();
                                downloadDropdown.showNear(w, bounds);
                            }
                        } catch (Exception ignored) {}
                    });
                }

                @Override
                public void downloadUpdated(com.example.nexus.model.Download download) {
                    // refresh dropdown if visible
                    Platform.runLater(() -> { try { downloadDropdown.refreshContent(); } catch (Exception ignored) {} });
                    Platform.runLater(() -> updateDownloadsBadge());
                }
            });
        } catch (Exception e) {
            logger.warn("Could not create download dropdown", e);
        }

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

        // Apply saved settings on startup (from database)
        Platform.runLater(() -> {
            String savedTheme = settingsService.getTheme();
            settingsController.applyTheme(savedTheme != null ? savedTheme : "light");
            settingsController.applyInterfaceSettings();
        });

        // Listen for settings changes
        settingsService.addSettingsChangeListener(settings -> {
            Platform.runLater(() -> settingsController.applyInterfaceSettings());
        });

        // Load initial tab
        Platform.runLater(() -> {
            String homePage = settingsService.getHomePage();
            createNewTab(homePage);
        });

        System.out.println("MainController initialized successfully");
        logger.info("Main controller initialized");
    }


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
        backButton.setGraphic(safeIcon("mdi2a-arrow-left", 18));
        forwardButton.setGraphic(safeIcon("mdi2a-arrow-right", 18));
        reloadButton.setGraphic(safeIcon("mdi2r-refresh", 18));
        homeButton.setGraphic(safeIcon("mdi2h-home", 18));
        newTabButton.setGraphic(safeIcon("mdi2p-plus", 18));
        menuButton.setGraphic(safeIcon("mdi2m-menu", 18));

        if (bookmarkButton != null) {
            bookmarkButton.setGraphic(safeIcon("mdi2b-bookmark-outline", 16));
        }
        if (downloadsButton != null) {
            downloadsButton.setGraphic(safeIcon("mdi2d-download", 16));
            // create a simple badge label overlay (using tooltip as a lightweight approach)
            downloadsButton.getProperties().put("downloadBadge", new javafx.scene.control.Label(""));
        }
        if (securityIcon != null) {
            securityIcon.setGraphic(safeIcon("mdi2l-lock", 14));
        }
    }

    // Small safe icon helper - returns a FontIcon when possible or a fallback label
    private javafx.scene.Node safeIcon(String literal, int size) {
        try {
            FontIcon fi = new FontIcon(literal);
            fi.setIconSize(size);
            return fi;
        } catch (Throwable t) {
            Label l = new Label("?");
            l.setStyle("-fx-font-size: " + Math.max(12, size-2) + "px; -fx-text-fill: #495057;");
            return l;
        }
    }


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
                icon.setIconColor(javafx.scene.paint.Color.valueOf("#28a745"));
            } else {
                icon.setIconLiteral("mdi2l-lock-open-outline");
                icon.setIconColor(javafx.scene.paint.Color.valueOf("#6c757d"));
            }
        }
    }


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

        // Prepare cleanup for listeners for this tab
        final ChangeListener<String> titleListener = (obs, oldTitle, newTitle) -> Platform.runLater(() -> {
            updateTabTitle(tab, newTitle);
            // update DB in background
            Tab tabModelRef = (Tab) browserTab.getTabModel();
            if (tabModelRef != null) {
                tabModelRef.setTitle(newTitle);
                tabService.updateTab(tabModelRef);
            }
        });

        final ChangeListener<String> urlListener = (obs, oldUrl, newUrl) -> Platform.runLater(() -> {
            // Update address bar if this is the selected tab
            if (tabPane.getSelectionModel().getSelectedItem() == tab) {
                addressBar.setText(newUrl);
                updateSecurityIcon(newUrl);
                updateBookmarkButtonState(newUrl);
            }
            Tab tabModelRef = (Tab) browserTab.getTabModel();
            if (tabModelRef != null) {
                tabModelRef.setUrl(newUrl);
                tabService.updateTab(tabModelRef);
            }
            // Add to history - track every page visit
            if (newUrl != null && !newUrl.isEmpty() && !newUrl.startsWith("about:") && !newUrl.startsWith("data:")) {
                try {
                    String pageTitle = browserTab.getTitle();
                    if (pageTitle == null || pageTitle.isEmpty()) pageTitle = newUrl;
                    historyService.addToHistory(newUrl, pageTitle);
                    logger.info("Added to history: {} - {}", pageTitle, newUrl);
                } catch (Exception e) {
                    logger.error("Failed to add to history: {}", newUrl, e);
                }
            }
        });

        // Attach listeners and store cleanup
        browserTab.titleProperty().addListener(titleListener);
        browserTab.urlProperty().addListener(urlListener);
        tabCleanupMap.put(tab, () -> {
            try {
                browserTab.titleProperty().removeListener(titleListener);
            } catch (Exception ignored) {}
            try {
                browserTab.urlProperty().removeListener(urlListener);
            } catch (Exception ignored) {}
        });

        // Add tab to pane and select it
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

        // Save to database
        Tab tabModel = new Tab(url);
        tabService.saveTab(tabModel);
        browserTab.setTabModel(tabModel);
    }


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
        titleLabel.getStyleClass().add("tab-title");
        titleLabel.setFocusTraversable(false);
        titleLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        titleLabel.setEllipsisString("...");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        // Tooltip to show full title on hover
        Tooltip titleTooltip = new Tooltip("New Tab");
        titleTooltip.setShowDelay(javafx.util.Duration.millis(500));
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
        // closeBtn.setStyle("-fx-background-color: transparent; -fx-padding: 2; -fx-cursor: hand;"); // Let CSS handle hover and cursor; only attach action handler (do not call setStyle here)
        closeBtn.setOnAction(e -> handleTabClose(tab));

        // Hover effect is handled by CSS (.tab-close-btn:hover)

        header.getChildren().addAll(faviconContainer, titleLabel, closeBtn);

        // Store references for updates
        header.setUserData(new Object[]{titleLabel, titleTooltip, faviconContainer, faviconView, defaultIcon});

        // Add tab preview popup on hover
        setupTabPreview(tab, header, browserTab);

        return header;
    }

    private void setupTabPreview(javafx.scene.control.Tab tab, HBox header, BrowserTab browserTab) {
        javafx.stage.Popup previewPopup = new javafx.stage.Popup();
        previewPopup.setAutoHide(true);

        VBox previewContent = new javafx.scene.layout.VBox(5);
        previewContent.setPrefWidth(240);

        // Preview title
        Label previewTitle = new Label("New Tab");
        previewTitle.setMaxWidth(220);
        previewTitle.setWrapText(false);
        previewTitle.setEllipsisString("...");

        // Preview URL
        Label previewUrl = new Label("");
        previewUrl.setMaxWidth(220);
        previewUrl.setWrapText(false);
        previewUrl.setEllipsisString("...");

        // Preview image container
        javafx.scene.image.ImageView previewImage = new javafx.scene.image.ImageView();
        previewImage.setFitWidth(224);
        previewImage.setFitHeight(140);
        previewImage.setPreserveRatio(true);
        previewImage.setSmooth(true);

        StackPane imageContainer = new StackPane(previewImage);
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
                    // Check theme by looking at loaded CSS - most reliable method
                    boolean isDark = isDarkThemeActive();

                    // Theme-aware colors
                    String bgColor = isDark ? "#2d2d2d" : "white";
                    String borderColor = isDark ? "#404040" : "#dee2e6";
                    String titleColor = isDark ? "#e0e0e0" : "#212529";
                    String urlColor = isDark ? "#909090" : "#6c757d";
                    String imageBgColor = isDark ? "#1e1e1e" : "#f8f9fa";
                    String shadowColor = isDark ? "rgba(0,0,0,0.4)" : "rgba(0,0,0,0.2)";

                    // Apply theme-aware CSS variables to preview popup (styles defined in CSS)
                    previewContent.getStyleClass().add("tab-preview");
                    previewContent.setStyle(String.format("--bg-primary: %s; --border-color: %s;", bgColor, borderColor));
                    previewTitle.getStyleClass().add("preview-title");
                    previewTitle.setStyle(String.format("-fx-text-fill: %s;", titleColor));
                    previewUrl.getStyleClass().add("preview-url");
                    previewUrl.setStyle(String.format("-fx-text-fill: %s;", urlColor));
                    imageContainer.getStyleClass().add("preview-image");
                    imageContainer.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 4;", imageBgColor));

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


    private boolean isDarkThemeActive() {
        // First check if dark.css is loaded in the current scene - most reliable
        if (tabPane != null && tabPane.getScene() != null) {
            for (String stylesheet : tabPane.getScene().getStylesheets()) {
                if (stylesheet.contains("dark.css")) {
                    return true;
                }
            }
        }

        // Fallback: check settings
        String theme = settingsService.getTheme();
        if ("dark".equals(theme)) {
            return true;
        }

        // Check system theme if setting is "system"
        if ("system".equals(theme)) {
            try {
                String gtkTheme = System.getenv("GTK_THEME");
                if (gtkTheme != null && gtkTheme.toLowerCase().contains("dark")) {
                    return true;
                }
                // Try gsettings
                try {
                    ProcessBuilder pb = new ProcessBuilder("gsettings", "get", "org.gnome.desktop.interface", "color-scheme");
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    String output = new String(process.getInputStream().readAllBytes()).trim();
                    process.waitFor();
                    if (output.contains("dark")) {
                        return true;
                    }
                } catch (Exception ex) {
                    // Ignore
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        return false;
    }


    private void handleTabClose(javafx.scene.control.Tab tab) {
        BrowserTab browserTab = tabBrowserMap.get(tab);

        // Run cleanup actions (remove listeners etc.)
        Runnable cleanup = tabCleanupMap.remove(tab);
        if (cleanup != null) {
            try { cleanup.run(); } catch (Exception e) { logger.debug("Error running tab cleanup", e); }
        }

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

        // Check if dark theme is active
        boolean isDark = "dark".equals(settingsService.getTheme());

        // Apply styling - in dark mode let CSS handle most of it, just add shadow
        if (isDark) {
            menu.setStyle(
                "-fx-background-color: #2d2d2d;" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 8;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 16, 0, 0, 4);" +
                "-fx-border-color: #404040;" +
                "-fx-border-radius: 10;" +
                "-fx-border-width: 1;"
            );
        } else {
            menu.setStyle(
                "-fx-background-color: #ffffff;" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 8;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 16, 0, 0, 4);" +
                "-fx-border-color: #e5e7eb;" +
                "-fx-border-radius: 10;" +
                "-fx-border-width: 1;"
            );
        }

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

        // === Dark Mode for Web Pages (like Dark Reader) ===
        CheckMenuItem webDarkMode = new CheckMenuItem("Dark Mode for Pages");
        FontIcon darkModeIcon = new FontIcon("mdi2w-weather-night");
        darkModeIcon.setIconSize(16);
        darkModeIcon.setIconColor(javafx.scene.paint.Color.web("#8b5cf6"));
        webDarkMode.setGraphic(darkModeIcon);

        // Check current tab's dark mode state
        BrowserTab currentBrowserTab = getCurrentBrowserTab();
        webDarkMode.setSelected(currentBrowserTab != null && currentBrowserTab.isWebPageDarkModeEnabled());

        webDarkMode.setOnAction(e -> {
            BrowserTab tab = getCurrentBrowserTab();
            if (tab != null) {
                tab.toggleWebPageDarkMode();
                // Also set global preference
                BrowserTab.setGlobalDarkModeEnabled(webDarkMode.isSelected());
            }
        });

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
            webDarkMode,
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


    public void toggleZoomMode() {
        viewportZoomMode = !viewportZoomMode;
        showZoomModeNotification();
    }


    private void showZoomModeNotification() {
        javafx.stage.Popup popup = new javafx.stage.Popup();

        String mode = viewportZoomMode ? "Magnifier Zoom" : "Page Zoom";
        String iconCode = viewportZoomMode ? "mdi2m-magnify-scan" : "mdi2m-magnify";

        HBox content = new HBox(8);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.getStyleClass().add("popup-notification");

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(16);
        icon.setIconColor(javafx.scene.paint.Color.WHITE);

        Label label = new Label("Zoom Mode: " + mode);
        label.getStyleClass().add("popup-label");

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


    public void toggleMouseTracking() {
        mouseTrackingEnabled = !mouseTrackingEnabled;
        showMouseTrackingNotification();
    }

    private void showMouseTrackingNotification() {
        javafx.stage.Popup popup = new javafx.stage.Popup();

        String status = mouseTrackingEnabled ? "Mouse Tracking: ON" : "Mouse Tracking: OFF";
        String iconCode = mouseTrackingEnabled ? "mdi2c-crosshairs-gps" : "mdi2c-crosshairs-off";

        HBox content = new HBox(8);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.getStyleClass().add("popup-notification");

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(16);
        icon.setIconColor(javafx.scene.paint.Color.WHITE);

        Label label = new Label(status);
        label.getStyleClass().add("popup-label");

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


    private void showZoomNotification() {
        double zoom = viewportZoomMode ? viewportZoom : currentZoom;
        int zoomPercent = (int) Math.round(zoom * 100);

        // Create a popup notification
        javafx.stage.Popup popup = new javafx.stage.Popup();

        String modeIndicator = viewportZoomMode ? "🔍 " : "";
        Label zoomLabel = new Label(modeIndicator + zoomPercent + "%");
        zoomLabel.getStyleClass().addAll("popup-notification","popup-label");

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


    private void setupScrollZoom(BrowserTab browserTab) {
        // Track mouse position for navigation when zoomed (viewport zoom only)
        // Use multiple event handlers to track mouse even over scrollbars

        javafx.event.EventHandler<javafx.scene.input.MouseEvent> mouseTracker = event -> {
            if (mouseTrackingEnabled && viewportZoomMode && viewportZoom > 1.0) {
                // Get bounds of the browser tab
                javafx.geometry.Bounds bounds = browserTab.localToScene(browserTab.getBoundsInLocal());

                // Calculate relative position, allowing values outside 0-1 range
                double relX = (event.getSceneX() - bounds.getMinX()) / bounds.getWidth();
                double relY = (event.getSceneY() - bounds.getMinY()) / bounds.getHeight();

                browserTab.scrollViewportSmooth(relX, relY);
            }
        };

        // Track on the browser tab itself
        browserTab.setOnMouseMoved(mouseTracker);
        browserTab.setOnMouseDragged(mouseTracker);

        // Also track on the scroll pane to catch scrollbar hover
        if (browserTab.getScrollPane() != null) {
            browserTab.getScrollPane().setOnMouseMoved(mouseTracker);
            browserTab.getScrollPane().setOnMouseDragged(mouseTracker);
        }

        // Stop scrolling when mouse leaves the entire area
        browserTab.setOnMouseExited(event -> {
            if (mouseTrackingEnabled && viewportZoomMode && viewportZoom > 1.0) {
                // Continue scrolling based on exit direction
                javafx.geometry.Bounds bounds = browserTab.localToScene(browserTab.getBoundsInLocal());
                double relX = (event.getSceneX() - bounds.getMinX()) / bounds.getWidth();
                double relY = (event.getSceneY() - bounds.getMinY()) / bounds.getHeight();
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
        dialog.getDialogPane().getStyleClass().add("custom-dialog");

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
        titleField.getStyleClass().add("dialog-field");

        TextField urlField = new TextField(url);
        urlField.setPromptText("URL");
        urlField.getStyleClass().add("dialog-field");

        // Folder selection
        ComboBox<com.example.nexus.model.BookmarkFolder> folderCombo = new ComboBox<>();
        folderCombo.setPromptText("Select folder (optional)");
        folderCombo.setPrefWidth(300);
        folderCombo.getStyleClass().add("dialog-field");

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
        newFolderBtn.getStyleClass().add("bookmark-new-folder-btn");
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
        dialog.getDialogPane().lookupButton(saveButtonType).getStyleClass().add("dialog-save-button");

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
        try {
            DownloadController downloadController = container.getOrCreate(DownloadController.class);
            boolean isDarkTheme = "dark".equals(settingsService.getTheme()) || ("system".equals(settingsService.getTheme()) && settingsController.isSystemDark());
            DownloadManagerPanel panel = new DownloadManagerPanel(container, downloadController, isDarkTheme);
            panel.show();
        } catch (Exception e) {
            logger.error("Error opening download manager panel", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to open Download Manager");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    public void handleDownloadsButtonClicked() {
        try {
            if (downloadDropdown != null && downloadsButton != null && downloadsButton.getScene() != null) {
                Bounds b = downloadsButton.localToScene(downloadsButton.getBoundsInLocal());
                Window w = downloadsButton.getScene().getWindow();
                // Toggle: hide if already showing
                if (downloadDropdown.isShowing()) {
                    downloadDropdown.hide();
                } else {
                    downloadDropdown.showNear(w, b);
                }
            } else {
                handleShowDownloads();
            }
        } catch (Exception e) {
            logger.warn("Error showing download dropdown", e);
            handleShowDownloads();
        }
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

            // Create a new Stage for settings
            javafx.stage.Stage settingsStage = new javafx.stage.Stage();
            settingsStage.setTitle("Settings");
            settingsStage.initModality(javafx.stage.Modality.NONE);

            // Create scene with size to show all content
            javafx.scene.Scene settingsScene = new javafx.scene.Scene(settingsPanel, 1000, 750);

            // Apply main CSS
            var mainCssResource = getClass().getResource("/com/example/nexus/css/main.css");
            if (mainCssResource != null) {
                settingsScene.getStylesheets().add(mainCssResource.toExternalForm());
            }

            // Apply current theme CSS using settings controller
            String currentTheme = settingsService.getTheme();
            String actualTheme = settingsController.resolveTheme(currentTheme);
            var themeCssResource = getClass().getResource("/com/example/nexus/css/" + actualTheme + ".css");
            if (themeCssResource != null) {
                settingsScene.getStylesheets().add(themeCssResource.toExternalForm());
            }

            settingsStage.setScene(settingsScene);
            settingsStage.setMinWidth(750);
            settingsStage.setMinHeight(600);
            settingsStage.setResizable(true);

            // Register scene with settings controller for theme updates
            settingsController.registerScene(settingsScene);

            // Set theme change callback to use settings controller
            settingsPanel.setOnThemeChange(theme -> settingsController.applyTheme(theme));

            // Cleanup when settings window closes
            settingsStage.setOnHidden(e -> settingsController.unregisterScene(settingsScene));

            // Show the stage
            settingsStage.show();
            settingsStage.toFront();

            logger.info("Settings dialog opened");
        } catch (Exception e) {
            logger.error("Error showing settings", e);
            showErrorAlert("Error", "Failed to open settings: " + e.getMessage());
        }
    }


    private void applyTheme(String theme) {
        settingsController.applyTheme(theme);
    }

    public void applyInterfaceSettings() {
        settingsController.applyInterfaceSettings();
    }


    private String detectSystemTheme() {
        return settingsController.detectSystemTheme();
    }


    private void applyAccentColor(String accentColor) {
        settingsController.applyAccentColor(accentColor);
    }


    private void applyFontSize(int fontSize) {
        settingsController.applyFontSize(fontSize);
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


    public void toggleWebPageDarkMode() {
        BrowserTab currentTab = getCurrentBrowserTab();
        if (currentTab != null) {
            currentTab.toggleWebPageDarkMode();
            // Update global setting
            BrowserTab.setGlobalDarkModeEnabled(currentTab.isWebPageDarkModeEnabled());

            // Show notification
            String status = currentTab.isWebPageDarkModeEnabled() ? "enabled" : "disabled";
            logger.info("Web page dark mode {}", status);
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

    private void updateDownloadsBadge() {
        try {
            var ds = container.getOrCreate(com.example.nexus.service.DownloadService.class);
            long active = ds.getAllDownloads().stream().filter(d -> "downloading".equals(d.getStatus())).count();
            javafx.application.Platform.runLater(() -> {
                if (downloadsButton != null) {
                    if (active > 0) {
                        downloadsButton.setText(String.valueOf(active));
                        downloadsButton.getStyleClass().remove("badge-hidden");
                        downloadsButton.getStyleClass().add("badge-visible");
                    } else {
                        downloadsButton.setText("");
                        downloadsButton.getStyleClass().remove("badge-visible");
                        downloadsButton.getStyleClass().add("badge-hidden");
                    }
                }
            });
        } catch (Exception ignored) {}
    }
}

