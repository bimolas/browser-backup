package com.example.nexus.view.components;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Tab;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.SnapshotParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * A JavaFX browser tab component using JavaFX WebView.
 * This provides a fully embedded browser experience within the JavaFX application.
 */
public class BrowserTab extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(BrowserTab.class);

    private final DIContainer container;
    private final WebView webView;
    private final WebEngine webEngine;
    private final ProgressBar loadingBar;
    private final StackPane contentPane;
    private final ScrollPane scrollPane;
    private final javafx.scene.Group zoomGroup;

    private final StringProperty titleProperty = new SimpleStringProperty("New Tab");
    private final StringProperty urlProperty = new SimpleStringProperty("");
    private final StringProperty faviconUrlProperty = new SimpleStringProperty("");
    private Tab tabModel;
    private boolean disposed = false;

    // Viewport zoom level (magnifier style)
    private double viewportZoom = 1.0;

    public BrowserTab(DIContainer container, String url) {
        this.container = container;

        // Create WebView with optimized settings
        this.webView = new WebView();
        this.webEngine = webView.getEngine();

        // Enable caching and optimize WebView performance
        webView.setCache(true);
        webView.setContextMenuEnabled(true);

        // Configure WebEngine for better compatibility and speed
        webEngine.setJavaScriptEnabled(true);

        // Set a modern user agent to improve site compatibility
        String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15";
        webEngine.setUserAgent(userAgent);

        // Create loading bar
        this.loadingBar = new ProgressBar();
        loadingBar.setMaxWidth(Double.MAX_VALUE);
        loadingBar.setPrefHeight(3);
        loadingBar.setVisible(false);
        loadingBar.setStyle("-fx-accent: #4285f4;");

        // Create zoom group to hold WebView - this enables viewport zooming
        this.zoomGroup = new javafx.scene.Group(webView);

        // Create scroll pane for panning when zoomed in
        this.scrollPane = new ScrollPane(zoomGroup);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: #ffffff; -fx-background: #ffffff;");
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        // Create content pane
        this.contentPane = new StackPane(scrollPane);
        contentPane.setStyle("-fx-background-color: #ffffff;");

        // Style the root
        this.setStyle("-fx-background-color: #ffffff;");

        // Set up layout
        setTop(loadingBar);
        setCenter(contentPane);

        // Bind WebView size to scroll pane viewport
        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            if (viewportZoom == 1.0) {
                webView.setPrefWidth(newBounds.getWidth());
                webView.setPrefHeight(newBounds.getHeight());
            }
        });

        // Setup WebEngine handlers
        setupWebEngineHandlers();

        // Handle popup windows
        webEngine.setCreatePopupHandler(config -> {
            WebView popupView = new WebView();
            popupView.getEngine().setJavaScriptEnabled(true);
            popupView.getEngine().setUserAgent(userAgent);
            return popupView.getEngine();
        });

        // Handle JavaScript alerts
        webEngine.setOnAlert(event -> logger.info("JavaScript Alert: {}", event.getData()));

        // Load initial URL
        if (url != null && !url.isEmpty()) {
            loadUrl(url);
        }

        logger.info("BrowserTab created with URL: {}", url);
    }

    private void setupWebEngineHandlers() {
        // Title changes
        webEngine.titleProperty().addListener((obs, oldTitle, newTitle) -> {
            Platform.runLater(() -> {
                titleProperty.set(newTitle != null && !newTitle.isEmpty() ? newTitle : "New Tab");
            });
        });

        // URL/Location changes
        webEngine.locationProperty().addListener((obs, oldUrl, newUrl) -> {
            Platform.runLater(() -> {
                urlProperty.set(newUrl != null ? newUrl : "");
                // Update favicon URL when location changes
                updateFaviconUrl(newUrl);
                logger.debug("Location changed to: {}", newUrl);
            });
        });

        // Loading state
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            Platform.runLater(() -> {
                logger.debug("WebEngine state changed: {} -> {}", oldState, newState);

                switch (newState) {
                    case READY:
                        loadingBar.setVisible(false);
                        break;
                    case SCHEDULED:
                        loadingBar.setVisible(true);
                        loadingBar.setProgress(0);
                        break;
                    case RUNNING:
                        loadingBar.setVisible(true);
                        loadingBar.setProgress(-1); // Indeterminate
                        break;
                    case SUCCEEDED:
                        loadingBar.setVisible(false);
                        loadingBar.setProgress(1.0);
                        // Try to extract favicon from the page after load
                        extractFaviconFromPage();
                        logger.info("Page loaded successfully: {}", webEngine.getLocation());
                        break;
                    case CANCELLED:
                        loadingBar.setVisible(false);
                        logger.info("Page load cancelled");
                        break;
                    case FAILED:
                        loadingBar.setVisible(false);
                        Throwable ex = webEngine.getLoadWorker().getException();
                        if (ex != null) {
                            logger.error("Page load failed: {}", ex.getMessage());
                            showErrorView("Failed to load page: " + ex.getMessage());
                        }
                        break;
                }
            });
        });

        // Loading progress
        webEngine.getLoadWorker().progressProperty().addListener((obs, oldProgress, newProgress) -> {
            Platform.runLater(() -> {
                double progress = newProgress.doubleValue();
                if (progress >= 0 && progress < 1.0) {
                    loadingBar.setProgress(progress);
                } else if (progress >= 1.0) {
                    loadingBar.setProgress(1.0);
                }
            });
        });

        // Error handling
        webEngine.getLoadWorker().exceptionProperty().addListener((obs, oldEx, newEx) -> {
            if (newEx != null) {
                logger.error("WebEngine exception: {}", newEx.getMessage(), newEx);
                Platform.runLater(() -> {
                    loadingBar.setVisible(false);
                });
            }
        });

        // Handle document load errors via onError
        webEngine.setOnError(event -> {
            logger.error("WebEngine error event: {}", event.getMessage());
        });
    }

    /**
     * Update the favicon URL based on the current page URL
     */
    private void updateFaviconUrl(String pageUrl) {
        if (pageUrl == null || pageUrl.isEmpty()) {
            faviconUrlProperty.set("");
            return;
        }

        try {
            URI uri = new URI(pageUrl);
            String host = uri.getHost();
            if (host != null) {
                // Use Google's favicon service as a reliable source
                String faviconUrl = "https://www.google.com/s2/favicons?domain=" + host + "&sz=32";
                faviconUrlProperty.set(faviconUrl);
            }
        } catch (Exception e) {
            logger.debug("Could not parse URL for favicon: {}", pageUrl);
            faviconUrlProperty.set("");
        }
    }

    /**
     * Try to extract favicon URL from the page's HTML
     */
    private void extractFaviconFromPage() {
        try {
            // Try to find favicon link in the page
            String script = """
                (function() {
                    var link = document.querySelector("link[rel*='icon']");
                    if (link) return link.href;
                    var shortcut = document.querySelector("link[rel='shortcut icon']");
                    if (shortcut) return shortcut.href;
                    return null;
                })()
                """;
            Object result = webEngine.executeScript(script);
            if (result != null && !result.toString().equals("null")) {
                faviconUrlProperty.set(result.toString());
            }
        } catch (Exception e) {
            // Ignore - we'll use the Google favicon service fallback
            logger.debug("Could not extract favicon from page", e);
        }
    }

    /**
     * Get a snapshot/preview image of the current page
     */
    public WritableImage getPreviewSnapshot() {
        if (disposed || webView == null) return null;
        try {
            SnapshotParameters params = new SnapshotParameters();
            return webView.snapshot(params, null);
        } catch (Exception e) {
            logger.debug("Could not create page snapshot", e);
            return null;
        }
    }

    private void showErrorView(String message) {
        Label iconLabel = new Label("🌐");
        iconLabel.setStyle("-fx-font-size: 64px;");

        Label errorLabel = new Label(message);
        errorLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666; -fx-text-alignment: center;");
        errorLabel.setWrapText(true);

        VBox errorBox = new VBox(20);
        errorBox.setAlignment(Pos.CENTER);
        errorBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 40;");
        errorBox.getChildren().addAll(iconLabel, errorLabel);

        contentPane.getChildren().clear();
        contentPane.getChildren().add(errorBox);
    }

    // Public API methods
    public void loadUrl(String url) {
        if (disposed) return;

        String processedUrl = url;

        // Add protocol if missing
        if (url != null && !url.isEmpty()) {
            String trimmedUrl = url.trim();

            // Check if it's a search query or URL
            if (!trimmedUrl.contains(".") && !trimmedUrl.startsWith("http") && !trimmedUrl.startsWith("file")) {
                // Treat as search query - use DuckDuckGo for privacy
                processedUrl = "https://duckduckgo.com/?q=" + trimmedUrl.replace(" ", "+");
            } else if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://") &&
                !trimmedUrl.startsWith("file://") && !trimmedUrl.startsWith("data:")) {
                processedUrl = "https://" + trimmedUrl;
            } else {
                processedUrl = trimmedUrl;
            }
        }

        final String finalUrl = processedUrl;

        // Show loading immediately for better UX
        Platform.runLater(() -> {
            loadingBar.setVisible(true);
            loadingBar.setProgress(-1);
        });

        // Load URL in background to keep UI responsive
        Platform.runLater(() -> {
            try {
                // Cancel any current loading first
                if (webEngine.getLoadWorker().isRunning()) {
                    webEngine.getLoadWorker().cancel();
                }

                webEngine.load(finalUrl);
                urlProperty.set(finalUrl);
                logger.info("Loading URL: {}", finalUrl);
            } catch (Exception e) {
                logger.error("Error loading URL: {}", finalUrl, e);
                loadingBar.setVisible(false);
                showErrorView("Failed to load: " + finalUrl);
            }
        });
    }

    public void goBack() {
        if (disposed) return;
        Platform.runLater(() -> {
            try {
                if (webEngine.getHistory().getCurrentIndex() > 0) {
                    webEngine.getHistory().go(-1);
                }
            } catch (Exception e) {
                logger.error("Error going back", e);
            }
        });
    }

    public void goForward() {
        if (disposed) return;
        Platform.runLater(() -> {
            try {
                if (webEngine.getHistory().getCurrentIndex() < webEngine.getHistory().getEntries().size() - 1) {
                    webEngine.getHistory().go(1);
                }
            } catch (Exception e) {
                logger.error("Error going forward", e);
            }
        });
    }

    public void reload() {
        if (disposed) return;
        Platform.runLater(() -> {
            try {
                webEngine.reload();
            } catch (Exception e) {
                logger.error("Error reloading", e);
            }
        });
    }

    public void stop() {
        if (disposed) return;
        Platform.runLater(() -> {
            try {
                webEngine.getLoadWorker().cancel();
            } catch (Exception e) {
                logger.error("Error stopping", e);
            }
        });
    }

    public void print() {
        if (disposed) return;
        Platform.runLater(() -> {
            try {
                // JavaFX WebView printing requires PrinterJob
                javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob();
                if (job != null && job.showPrintDialog(webView.getScene().getWindow())) {
                    webEngine.print(job);
                    job.endJob();
                }
            } catch (Exception e) {
                logger.error("Error printing", e);
            }
        });
    }

    public void print(boolean saveAsPdf, boolean printHeaders, boolean printBackground) {
        // For JavaFX WebView, we use the standard print
        print();
    }

    public void find(String searchText, boolean forward, boolean matchCase, boolean findNext) {
        if (disposed || searchText == null || searchText.isEmpty()) return;
        Platform.runLater(() -> {
            try {
                // Use JavaScript to find text
                String script = String.format(
                    "window.find('%s', %b, %b, true, false, true, false);",
                    searchText.replace("'", "\\'"),
                    matchCase,
                    !forward
                );
                webEngine.executeScript(script);
            } catch (Exception e) {
                logger.error("Error finding text", e);
            }
        });
    }

    public void stopFind() {
        if (disposed) return;
        Platform.runLater(() -> {
            try {
                webEngine.executeScript("window.getSelection().removeAllRanges();");
            } catch (Exception e) {
                logger.debug("Error clearing selection", e);
            }
        });
    }

    public void zoomIn() {
        if (disposed) return;
        Platform.runLater(() -> {
            webView.setZoom(webView.getZoom() * 1.1);
        });
    }

    public void zoomOut() {
        if (disposed) return;
        Platform.runLater(() -> {
            webView.setZoom(webView.getZoom() / 1.1);
        });
    }

    public void resetZoom() {
        if (disposed) return;
        Platform.runLater(() -> {
            webView.setZoom(1.0);
        });
    }

    /**
     * Set zoom level using CSS zoom (makes elements bigger)
     */
    public void setZoomLevel(double zoomLevel) {
        if (disposed) return;
        Platform.runLater(() -> {
            webView.setZoom(zoomLevel);
        });
    }

    /**
     * Set viewport zoom level (magnifier-style zoom) - zooms into the view
     * This allows you to see details by zooming into a specific area
     * @param zoomLevel The zoom level (1.0 = 100%, 2.0 = 200%, etc.)
     */
    public void setViewportZoom(double zoomLevel) {
        if (disposed) return;
        this.viewportZoom = zoomLevel;

        Platform.runLater(() -> {
            // Apply scale transform to the WebView
            Scale scale = new Scale(zoomLevel, zoomLevel, 0, 0);
            webView.getTransforms().clear();
            webView.getTransforms().add(scale);

            // Update scroll pane to handle the new size
            if (zoomLevel > 1.0) {
                scrollPane.setFitToWidth(false);
                scrollPane.setFitToHeight(false);
                scrollPane.setPannable(true);
                // Make scrollbars always visible when zoomed
                scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            } else {
                scrollPane.setFitToWidth(true);
                scrollPane.setFitToHeight(true);
                scrollPane.setPannable(false);
                scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            }
        });
    }

    /**
     * Get current viewport zoom level
     */
    public double getViewportZoom() {
        return viewportZoom;
    }

    /**
     * Smooth scroll the viewport based on mouse position
     * This provides fluid navigation when zoomed in
     * @param relativeX Relative X position (0.0 to 1.0)
     * @param relativeY Relative Y position (0.0 to 1.0)
     */
    public void scrollViewportSmooth(double relativeX, double relativeY) {
        if (disposed || viewportZoom <= 1.0) return;

        Platform.runLater(() -> {
            // Calculate distance from center (0.5, 0.5)
            double hDiff = relativeX - 0.5;
            double vDiff = relativeY - 0.5;

            // Speed increases based on how far from center and zoom level
            double speedMultiplier = 0.02 * (viewportZoom - 1.0);

            // Calculate new scroll positions
            double currentH = scrollPane.getHvalue();
            double currentV = scrollPane.getVvalue();

            // Smooth continuous scrolling - always scroll based on mouse position
            // The further from center, the faster it scrolls
            double newH = currentH + (hDiff * speedMultiplier);
            double newV = currentV + (vDiff * speedMultiplier);

            // Clamp values to valid range (0-1) - this prevents stopping at edges
            newH = Math.max(0, Math.min(1, newH));
            newV = Math.max(0, Math.min(1, newV));

            // Apply the scroll
            scrollPane.setHvalue(newH);
            scrollPane.setVvalue(newV);
        });
    }

    /**
     * Scroll viewport to specific position (0-1 range)
     */
    public void scrollViewportTo(double relativeX, double relativeY) {
        if (disposed || viewportZoom <= 1.0) return;

        Platform.runLater(() -> {
            scrollPane.setHvalue(Math.max(0, Math.min(1, relativeX)));
            scrollPane.setVvalue(Math.max(0, Math.min(1, relativeY)));
        });
    }

    /**
     * Get the scroll pane for external zoom control
     */
    public ScrollPane getScrollPane() {
        return scrollPane;
    }

    public void showDevTools() {
        // JavaFX WebView doesn't have built-in dev tools
        // We could show a simple inspector dialog
        logger.info("Developer tools not available in WebView mode");
    }

    public void showDevTools(String mode) {
        // JavaFX WebView doesn't have built-in dev tools
        // mode parameter is ignored since WebView doesn't support docking
        logger.info("Developer tools not available in WebView mode (mode: {})", mode);
    }

    public void viewSource() {
        if (disposed) return;
        String currentUrl = webEngine.getLocation();
        if (currentUrl != null && !currentUrl.startsWith("view-source:")) {
            loadUrl("view-source:" + currentUrl);
        }
    }

    public String getTitle() {
        return titleProperty.get();
    }

    public StringProperty titleProperty() {
        return titleProperty;
    }

    public String getUrl() {
        return urlProperty.get();
    }

    public StringProperty urlProperty() {
        return urlProperty;
    }

    public String getFaviconUrl() {
        return faviconUrlProperty.get();
    }

    public StringProperty faviconUrlProperty() {
        return faviconUrlProperty;
    }

    public Tab getTabModel() {
        return tabModel;
    }

    public void setTabModel(Tab tabModel) {
        this.tabModel = tabModel;
    }

    public boolean isLoading() {
        return webEngine.getLoadWorker().isRunning();
    }

    public WebView getWebView() {
        return webView;
    }

    public WebEngine getWebEngine() {
        return webEngine;
    }

    /**
     * Execute JavaScript in the current page
     * @param script The JavaScript code to execute
     * @return The result of the script execution, or null if failed
     */
    public Object executeScript(String script) {
        if (disposed || script == null || script.isEmpty()) return null;
        try {
            return webEngine.executeScript(script);
        } catch (Exception e) {
            logger.debug("Error executing script: {}", e.getMessage());
            return null;
        }
    }

    public void dispose() {
        disposed = true;
        Platform.runLater(() -> {
            try {
                webEngine.load(null);
            } catch (Exception e) {
                // Ignore
            }
        });
        logger.info("BrowserTab disposed");
    }
}

