package com.example.nexus.controller;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Tab;
import com.example.nexus.service.*;
import com.example.nexus.view.components.BrowserTab;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class TabController {

    private static final Logger logger = LoggerFactory.getLogger(TabController.class);

    private final DIContainer container;
    private final TabService tabService;
    private final SettingsService settingsService;
    private final HistoryController historyController;
    private final ZoomService zoomService;

    private TabPane tabPane;
    private StackPane browserContainer;
    private TextField addressBar;
    private Label securityIcon;

    private final Map<javafx.scene.control.Tab, BrowserTab> tabBrowserMap = new HashMap<>();
    private final Map<javafx.scene.control.Tab, Runnable> tabCleanupMap = new HashMap<>();
    private final Map<javafx.scene.control.Tab, UUID> tabUuidMap = new HashMap<>();

    public TabController(DIContainer container) {
        this.container = container;
        this.tabService = container.getOrCreate(TabService.class);
        this.settingsService = container.getOrCreate(SettingsService.class);
        HistoryService historyService = container.getOrCreate(HistoryService.class);
        this.historyController = new HistoryController(historyService);
        this.zoomService = container.getOrCreate(ZoomService.class);
    }

    public TabPane getTabPane() {
        return tabPane;
    }

    public void initializeUIComponents(TabPane tabPane, StackPane browserContainer,
                                       TextField addressBar, Label securityIcon) {
        this.tabPane = tabPane;
        this.browserContainer = browserContainer;
        this.addressBar = addressBar;
        this.securityIcon = securityIcon;

        setupTabSelectionListener();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setFocusTraversable(false);

        Platform.runLater(this::hideTabDropdownButton);
    }

    private void setupTabSelectionListener() {
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                showBrowserForTab(newTab);
            }
        });
    }

    void hideTabDropdownButton() {

        tabPane.lookupAll(".control-buttons-tab").forEach(node -> {
            node.setVisible(false);
            node.setManaged(false);
        });
        tabPane.lookupAll(".tab-down-button").forEach(node -> {
            node.setVisible(false);
            node.setManaged(false);
        });
    }

    public void createNewTab(String url) {
        logger.info("Creating new tab with URL: {}", url);

        BrowserTab browserTab = new BrowserTab(container, url);

        setupScrollZoom(browserTab);

        javafx.scene.control.Tab tab = new javafx.scene.control.Tab();
        tab.setText("New Tab");
        tab.setClosable(false);

        // Generate UUID for this tab
        UUID tabUuid = UUID.randomUUID();
        tabUuidMap.put(tab, tabUuid);

        HBox tabHeader = createTabHeader(tab, browserTab);
        tab.setGraphic(tabHeader);
        tab.setText(null);


        tabBrowserMap.put(tab, browserTab);

        final ChangeListener<String> titleListener = (obs, oldTitle, newTitle) -> Platform.runLater(() -> {
            updateTabTitle(tab, newTitle);

            Tab tabModelRef = browserTab.getTabModel();
            if (tabModelRef != null) {
                tabModelRef.setTitle(newTitle);
                tabService.updateTab(tabModelRef);
            }
        });

        final ChangeListener<String> urlListener = (obs, oldUrl, newUrl) -> Platform.runLater(() -> {

            if (tabPane.getSelectionModel().getSelectedItem() == tab) {
                addressBar.setText(newUrl);
                updateSecurityIcon(newUrl);
            }
            Tab tabModelRef = browserTab.getTabModel();
            if (tabModelRef != null) {
                tabModelRef.setUrl(newUrl);
                tabService.updateTab(tabModelRef);
            }

            if (newUrl != null && !newUrl.isEmpty() &&
                    !newUrl.startsWith("about:") && !newUrl.startsWith("data:")) {
                try {
                    String pageTitle = browserTab.getTitle();
                    if (pageTitle == null || pageTitle.isEmpty()) pageTitle = newUrl;
                    historyController.recordVisit(newUrl, pageTitle);
                } catch (Exception e) {
                    logger.error("Failed to add to history: {}", newUrl, e);
                }
            }
        });

        browserTab.titleProperty().addListener(titleListener);
        browserTab.urlProperty().addListener(urlListener);
        tabCleanupMap.put(tab, () -> {
            try {
                browserTab.titleProperty().removeListener(titleListener);
            } catch (Exception ignored) {
            }
            try {
                browserTab.urlProperty().removeListener(urlListener);
            } catch (Exception ignored) {
            }
        });

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

        Tab tabModel = new Tab(url);
        tabService.saveTab(tabModel);
        browserTab.setTabModel(tabModel);
    }

    void showBrowserForTab(javafx.scene.control.Tab tab) {
        BrowserTab browserTab = tabBrowserMap.get(tab);
        if (browserTab != null) {

            browserContainer.getChildren().clear();
            browserContainer.getChildren().add(browserTab);

            addressBar.setText(browserTab.getUrl());

            updateSecurityIcon(browserTab.getUrl());
        }
    }

    private void updateSecurityIcon(String url) {
        if (securityIcon != null && securityIcon.getGraphic() instanceof FontIcon icon) {
            if (url != null && url.startsWith("https://")) {
                icon.setIconLiteral("mdi2l-lock");
                icon.setIconColor(Color.valueOf("#28a745"));
            } else {
                icon.setIconLiteral("mdi2l-lock-open-outline");
                icon.setIconColor(Color.valueOf("#6c757d"));
            }
        }
    }

    private HBox createTabHeader(javafx.scene.control.Tab tab, BrowserTab browserTab) {
        HBox header = new HBox(4);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setFocusTraversable(false);

        header.setPrefWidth(140);
        header.setMinWidth(80);
        header.setMaxWidth(180);
        header.setStyle("-fx-padding: 0 4 0 4;");

        ImageView faviconView = new ImageView();
        faviconView.setFitWidth(14);
        faviconView.setFitHeight(14);
        faviconView.setPreserveRatio(true);
        faviconView.setSmooth(true);

        FontIcon defaultIcon = new FontIcon("mdi2w-web");
        defaultIcon.setIconSize(14);
        defaultIcon.setIconColor(Color.valueOf("#6c757d"));

        StackPane faviconContainer = new StackPane();
        faviconContainer.setPrefSize(14, 14);
        faviconContainer.setMinSize(14, 14);
        faviconContainer.setMaxSize(14, 14);
        faviconContainer.getChildren().add(defaultIcon);
        faviconContainer.setFocusTraversable(false);

        Label titleLabel = new Label("New Tab");
        titleLabel.setPrefWidth(90);
        titleLabel.setMinWidth(30);
        titleLabel.setMaxWidth(110);
        titleLabel.getStyleClass().add("tab-title");
        titleLabel.setFocusTraversable(false);
        titleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        titleLabel.setEllipsisString("...");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Tooltip titleTooltip = new Tooltip("New Tab");
        titleTooltip.setShowDelay(Duration.millis(500));
        Tooltip.install(titleLabel, titleTooltip);

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

                        logger.debug("Failed to load favicon: {}", newUrl);
                    }
                }
            });
        });

        Button closeBtn = new Button();
        FontIcon closeIcon = new FontIcon("mdi2c-close");
        closeIcon.setIconSize(10);
        closeIcon.setIconColor(Color.valueOf("#6c757d"));
        closeBtn.setGraphic(closeIcon);
        closeBtn.getStyleClass().add("tab-close-btn");
        closeBtn.setFocusTraversable(false);
        closeBtn.setPrefSize(16, 16);
        closeBtn.setMinSize(16, 16);
        closeBtn.setMaxSize(16, 16);
        closeBtn.setOnAction(e -> handleTabClose(tab));

        header.getChildren().addAll(faviconContainer, titleLabel, closeBtn);

        header.setUserData(new Object[]{titleLabel, titleTooltip, faviconContainer, faviconView, defaultIcon});

        setupTabPreview(tab, header, browserTab);


        return header;
    }

    private void setupTabPreview(javafx.scene.control.Tab tab, HBox header, BrowserTab browserTab) {
        javafx.stage.Popup previewPopup = new javafx.stage.Popup();
        previewPopup.setAutoHide(true);

        VBox previewContent = new VBox(5);
        previewContent.setPrefWidth(240);

        Label previewTitle = new Label("New Tab");
        previewTitle.setMaxWidth(220);
        previewTitle.setWrapText(false);
        previewTitle.setEllipsisString("...");

        Label previewUrl = new Label("");
        previewUrl.setMaxWidth(220);
        previewUrl.setWrapText(false);
        previewUrl.setEllipsisString("...");

        ImageView previewImage = new ImageView();
        previewImage.setFitWidth(224);
        previewImage.setFitHeight(140);
        previewImage.setPreserveRatio(true);
        previewImage.setSmooth(true);

        StackPane imageContainer = new StackPane(previewImage);
        imageContainer.setPrefSize(224, 140);

        previewContent.getChildren().addAll(previewTitle, previewUrl, imageContainer);
        previewPopup.getContent().add(previewContent);

        javafx.animation.PauseTransition hoverDelay = new javafx.animation.PauseTransition(Duration.millis(400));
        final AtomicBoolean isHovering = new AtomicBoolean(false);

        header.setOnMouseEntered(e -> {
            isHovering.set(true);
            hoverDelay.setOnFinished(event -> {
                if (isHovering.get() && tab != tabPane.getSelectionModel().getSelectedItem()) {

                    boolean isDark = isDarkThemeActive();

                    String bgColor = isDark ? "#2d2d2d" : "white";
                    String borderColor = isDark ? "#404040" : "#dee2e6";
                    String titleColor = isDark ? "#e0e0e0" : "#212529";
                    String urlColor = isDark ? "#909090" : "#6c757d";
                    String imageBgColor = isDark ? "#1e1e1e" : "#f8f9fa";

                    previewContent.getStyleClass().add("tab-preview");
                    previewContent.setStyle(String.format("--bg-primary: %s; --border-color: %s;", bgColor, borderColor));
                    previewTitle.getStyleClass().add("preview-title");
                    previewTitle.setStyle(String.format("-fx-text-fill: %s;", titleColor));
                    previewUrl.getStyleClass().add("preview-url");
                    previewUrl.setStyle(String.format("-fx-text-fill: %s;", urlColor));
                    imageContainer.getStyleClass().add("preview-image");
                    imageContainer.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 4;", imageBgColor));

                    previewTitle.setText(browserTab.getTitle());
                    previewUrl.setText(browserTab.getUrl());

                    try {
                        javafx.scene.image.WritableImage snapshot = browserTab.getPreviewSnapshot();
                        if (snapshot != null) {
                            previewImage.setImage(snapshot);
                        }
                    } catch (Exception ex) {
                        logger.debug("Could not get tab preview snapshot");
                    }

                    Bounds bounds = header.localToScreen(header.getBoundsInLocal());
                    if (bounds != null) {
                        previewPopup.show(header, bounds.getMinX(), bounds.getMaxY() + 5);
                    }
                }
            });
            hoverDelay.playFromStart();
        });

        header.setOnMouseExited(e -> {
            isHovering.set(false);
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

                    titleLabel.setText(title);

                    if (titleTooltip != null) {
                        titleTooltip.setText(title);
                    }
                }
            }
        }
    }

    private boolean isDarkThemeActive() {

        if (tabPane != null && tabPane.getScene() != null) {
            for (String stylesheet : tabPane.getScene().getStylesheets()) {
                if (stylesheet.contains("dark.css")) {
                    return true;
                }
            }
        }

        String theme = settingsService.getTheme();
        if ("dark".equals(theme)) {
            return true;
        }

        if ("system".equals(theme)) {
            try {
                String gtkTheme = System.getenv("GTK_THEME");
                if (gtkTheme != null && gtkTheme.toLowerCase().contains("dark")) {
                    return true;
                }

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
                    // Silently ignore gsettings errors
                }
            } catch (Exception e) {
                // Silently ignore theme detection errors
            }
        }

        return false;
    }

    public void handleTabClose(javafx.scene.control.Tab tab) {
        BrowserTab browserTab = tabBrowserMap.get(tab);

        Runnable cleanup = tabCleanupMap.remove(tab);
        if (cleanup != null) {
            try {
                cleanup.run();
            } catch (Exception e) {
                logger.debug("Error running tab cleanup", e);
            }
        }

        // Remove tab from maps
        tabBrowserMap.remove(tab);
        UUID tabUuid = tabUuidMap.remove(tab);


        tabPane.getTabs().remove(tab);

        if (browserTab != null && browserTab.getTabModel() != null) {
            tabService.deleteTab(browserTab.getTabModel().getId());
        }

        if (browserTab != null) {
            browserTab.dispose();
        }

        if (tabPane.getTabs().isEmpty()) {
            createNewTab(settingsService.getHomePage());
        }
    }

    public BrowserTab getCurrentBrowserTab() {
        javafx.scene.control.Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            return tabBrowserMap.get(selectedTab);
        }
        return null;
    }


    private void setupScrollZoom(BrowserTab browserTab) {
        browserTab.setOnScroll(event -> {
            if (event.isControlDown()) {
                event.consume();
                zoomService.handleScrollZoom(browserTab, event.getDeltaY(), () -> {

                });
            }
        });

        javafx.event.EventHandler<javafx.scene.input.MouseEvent> mouseTracker = event -> {
            if (zoomService.isMouseTrackingEnabled() &&
                    zoomService.isViewportZoomMode() &&
                    zoomService.getViewportZoom() > 1.0) {

                Bounds bounds = browserTab.localToScene(browserTab.getBoundsInLocal());

                double relX = (event.getSceneX() - bounds.getMinX()) / bounds.getWidth();
                double relY = (event.getSceneY() - bounds.getMinY()) / bounds.getHeight();

                browserTab.scrollViewportSmooth(relX, relY);
            }
        };

        browserTab.setOnMouseMoved(mouseTracker);
        browserTab.setOnMouseDragged(mouseTracker);

        if (browserTab.getScrollPane() != null) {
            browserTab.getScrollPane().setOnMouseMoved(mouseTracker);
            browserTab.getScrollPane().setOnMouseDragged(mouseTracker);
        }

        browserTab.setOnMouseExited(event -> {
            if (zoomService.isMouseTrackingEnabled() &&
                    zoomService.isViewportZoomMode() &&
                    zoomService.getViewportZoom() > 1.0) {

                Bounds bounds = browserTab.localToScene(browserTab.getBoundsInLocal());
                double relX = (event.getSceneX() - bounds.getMinX()) / bounds.getWidth();
                double relY = (event.getSceneY() - bounds.getMinY()) / bounds.getHeight();
                browserTab.scrollViewportSmooth(relX, relY);
            }
        });

        browserTab.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.DIGIT0) {

                zoomService.resetZoom(browserTab, null);
                event.consume();
            } else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.EQUALS) {

                zoomService.zoomIn(browserTab, null);
                event.consume();
            } else if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.MINUS) {

                zoomService.zoomOut(browserTab, null);
                event.consume();
            }
        });
    }

    public BrowserTab getBrowserTabForFxTab(javafx.scene.control.Tab fxTab) {
        return tabBrowserMap.get(fxTab);
    }
}
