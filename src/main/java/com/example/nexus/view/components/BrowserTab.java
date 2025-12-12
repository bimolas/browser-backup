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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final StringProperty titleProperty = new SimpleStringProperty("New Tab");
    private final StringProperty urlProperty = new SimpleStringProperty("");
    private Tab tabModel;
    private boolean disposed = false;

    public BrowserTab(DIContainer container, String url) {
        this.container = container;

        // Create WebView
        this.webView = new WebView();
        this.webEngine = webView.getEngine();

        // Create loading bar
        this.loadingBar = new ProgressBar();
        loadingBar.setMaxWidth(Double.MAX_VALUE);
        loadingBar.setPrefHeight(3);
        loadingBar.setVisible(false);
        loadingBar.setStyle("-fx-accent: #4285f4;");

        // Create content pane
        this.contentPane = new StackPane(webView);
        contentPane.setStyle("-fx-background-color: #ffffff;");

        // Style the root
        this.setStyle("-fx-background-color: #ffffff;");

        // Set up layout
        setTop(loadingBar);
        setCenter(contentPane);

        // Setup WebEngine handlers
        setupWebEngineHandlers();

        // Enable JavaScript
        webEngine.setJavaScriptEnabled(true);

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
            });
        });

        // Loading state
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            Platform.runLater(() -> {
                if (newState == Worker.State.RUNNING) {
                    loadingBar.setVisible(true);
                    loadingBar.setProgress(-1); // Indeterminate
                } else {
                    loadingBar.setVisible(false);
                }
            });
        });

        // Loading progress
        webEngine.getLoadWorker().progressProperty().addListener((obs, oldProgress, newProgress) -> {
            Platform.runLater(() -> {
                if (newProgress.doubleValue() >= 0 && newProgress.doubleValue() < 1.0) {
                    loadingBar.setProgress(newProgress.doubleValue());
                }
            });
        });

        // Error handling
        webEngine.getLoadWorker().exceptionProperty().addListener((obs, oldEx, newEx) -> {
            if (newEx != null) {
                logger.error("WebEngine error: {}", newEx.getMessage());
            }
        });
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
            if (!url.startsWith("http://") && !url.startsWith("https://") &&
                !url.startsWith("file://") && !url.startsWith("data:")) {
                processedUrl = "https://" + url;
            }
        }

        final String finalUrl = processedUrl;
        Platform.runLater(() -> {
            try {
                webEngine.load(finalUrl);
                urlProperty.set(finalUrl);
            } catch (Exception e) {
                logger.error("Error loading URL: {}", finalUrl, e);
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

    public void setZoomLevel(double zoomLevel) {
        if (disposed) return;
        Platform.runLater(() -> {
            webView.setZoom(zoomLevel);
        });
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

