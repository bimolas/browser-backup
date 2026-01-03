package com.example.nexus.view.components;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Tab;
import com.example.nexus.controller.DownloadController;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.SnapshotParameters;
import javafx.beans.value.ChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

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
    private final BooleanProperty loadingProperty = new SimpleBooleanProperty(false);
    private final DoubleProperty zoomProperty = new SimpleDoubleProperty(1.0);
    private Tab tabModel;
    private boolean disposed = false;

    private double viewportZoom = 1.0;

    private boolean webPageDarkMode = false;
    private static boolean globalDarkModeEnabled = false;

    private ChangeListener<javafx.geometry.Bounds> viewportBoundsListener;
    private ChangeListener<String> titleChangeListener;
    private ChangeListener<String> locationChangeListener;
    private ChangeListener<Worker.State> stateChangeListener;
    private ChangeListener<Number> progressChangeListener;
    private ChangeListener<Throwable> exceptionChangeListener;

    public BrowserTab(DIContainer container, String url) {
        this.container = container;

        this.webView = new WebView();
        this.webEngine = webView.getEngine();

        webView.setCache(true);
        webView.setContextMenuEnabled(true);

        webEngine.setJavaScriptEnabled(true);

        String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15";
        webEngine.setUserAgent(userAgent);

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            loadingProperty.set(newState == Worker.State.RUNNING || newState == Worker.State.SCHEDULED);
        });

        webView.zoomProperty().addListener((obs, oldZoom, newZoom) -> {
            zoomProperty.set(newZoom.doubleValue());
        });

        this.loadingBar = new ProgressBar();
        loadingBar.setMaxWidth(Double.MAX_VALUE);
        loadingBar.setPrefHeight(3);
        loadingBar.setVisible(false);
        loadingBar.setStyle("-fx-accent: #4285f4;");

        this.zoomGroup = new javafx.scene.Group(webView);

        this.scrollPane = new ScrollPane(zoomGroup);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: #ffffff; -fx-background: #ffffff;");
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        this.contentPane = new StackPane(scrollPane);
        contentPane.setStyle("-fx-background-color: #ffffff;");

        this.setStyle("-fx-background-color: #ffffff;");

        setTop(loadingBar);
        setCenter(contentPane);

        viewportBoundsListener = (obs, oldBounds, newBounds) -> {
            if (viewportZoom == 1.0) {
                webView.setPrefWidth(newBounds.getWidth());
                webView.setPrefHeight(newBounds.getHeight());
            }
        };
        scrollPane.viewportBoundsProperty().addListener(viewportBoundsListener);

        setupWebEngineHandlers();

        webEngine.setCreatePopupHandler(config -> {
            WebView popupView = new WebView();
            popupView.getEngine().setJavaScriptEnabled(true);
            popupView.getEngine().setUserAgent(userAgent);
            return popupView.getEngine();
        });

        webEngine.setOnAlert(event -> logger.info("JavaScript Alert: {}", event.getData()));

        if (url != null && !url.isEmpty()) {
            loadUrl(url);
        }

        logger.info("BrowserTab created with URL: {}", url);
    }

    public BooleanProperty loadingProperty() {
        return loadingProperty;
    }

    public DoubleProperty zoomProperty() {
        return zoomProperty;
    }

    public double getZoom() {
        return zoomProperty.get();
    }
    private void setupWebEngineHandlers() {

        titleChangeListener = (obs, oldTitle, newTitle) -> {
            Platform.runLater(() -> titleProperty.set(newTitle != null && !newTitle.isEmpty() ? newTitle : "New Tab"));
        };
        webEngine.titleProperty().addListener(titleChangeListener);

        locationChangeListener = (obs, oldUrl, newUrl) -> {
            Platform.runLater(() -> {
                try {
                    String urlStr = newUrl != null ? newUrl : "";

                    if (urlStr != null && !urlStr.isEmpty()) {
                        String lower = urlStr.split("\\?")[0].toLowerCase();
                        if (lower.matches(".*\\.(zip|exe|msi|pdf|docx?|xlsx?|rar|7z|png|jpe?g|gif|bmp|webp)$")) {
                            try {
                                String fileName = suggestFileNameFromUrl(urlStr);
                                DownloadController dc = container.getOrCreate(DownloadController.class);
                                dc.requestDownloadFromUI(urlStr, fileName);

                                try { webEngine.getLoadWorker().cancel(); } catch (Exception ignore) {}
                                logger.info("Intercepted navigation and started download: {}", urlStr);
                                return;
                            } catch (Exception ex) {
                                logger.debug("Failed to start download for intercepted URL: {}", urlStr, ex);
                            }
                        } else {

                            java.util.concurrent.CompletableFuture.runAsync(() -> {
                                HttpURLConnection conn = null;
                                try {
                                    java.net.URL u = new java.net.URL(urlStr);
                                    conn = (HttpURLConnection) u.openConnection();
                                    conn.setRequestMethod("HEAD");
                                    conn.setInstanceFollowRedirects(true);
                                    conn.setConnectTimeout(8000);
                                    conn.setReadTimeout(8000);
                                    conn.connect();

                                    String disposition = conn.getHeaderField("Content-Disposition");
                                    String contentType = conn.getContentType();
                                    int code = conn.getResponseCode();

                                    boolean looksLikeAttachment = false;
                                    if (disposition != null && disposition.toLowerCase().contains("attachment")) looksLikeAttachment = true;
                                    if (disposition != null && (disposition.toLowerCase().contains("filename=") || disposition.toLowerCase().contains("filename*="))) looksLikeAttachment = true;
                                    if (contentType != null) {
                                        String ct = contentType.toLowerCase();

                                        if (ct.startsWith("application/") || ct.startsWith("video/") || ct.startsWith("audio/") ) looksLikeAttachment = true;

                                        if (ct.equals("application/octet-stream")) looksLikeAttachment = true;
                                    }

                                    if (looksLikeAttachment || code == HttpURLConnection.HTTP_OK && (contentType == null || !contentType.toLowerCase().contains("text/html"))) {

                                        String suggested = null;
                                        if (disposition != null) {

                                            java.util.regex.Matcher m = java.util.regex.Pattern.compile("filename\\*?=\\s*(?:\\\"?)(?:UTF-8'')?([^;\\\"]+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(disposition);
                                            if (m.find()) {
                                                suggested = m.group(1).trim().replaceAll("[\\\"]", "");
                                                try { suggested = java.net.URLDecoder.decode(suggested, java.nio.charset.StandardCharsets.UTF_8); } catch (Exception ignored) {}
                                            }
                                        }
                                        if (suggested == null || suggested.isEmpty()) suggested = suggestFileNameFromUrl(urlStr);

                                        final String fileName = suggested;

                                        javafx.application.Platform.runLater(() -> {
                                            try {
                                                DownloadController dc = container.getOrCreate(DownloadController.class);
                                                dc.requestDownloadFromUI(urlStr, fileName);
                                                try { webEngine.getLoadWorker().cancel(); } catch (Exception ignore) {}
                                                logger.info("HEAD-detected download and started: {} (suggested={})", urlStr, fileName);
                                            } catch (Exception ex) {
                                                logger.debug("Failed to start HEAD-detected download for {}", urlStr, ex);
                                            }
                                        });
                                    }
                                } catch (Exception e) {

                                    logger.debug("HEAD check failed for {}: {}", urlStr, e.getMessage());
                                } finally {
                                    if (conn != null) conn.disconnect();
                                }
                            });
                        }
                    }

                    urlProperty.set(newUrl != null ? newUrl : "");

                    updateFaviconUrl(newUrl);
                    logger.debug("Location changed to: {}", newUrl);
                } catch (Exception e) {
                    logger.debug("Error processing location change", e);
                }
             });
         };
         webEngine.locationProperty().addListener(locationChangeListener);

        stateChangeListener = (obs, oldState, newState) -> {
            Platform.runLater(() -> {
                logger.debug("WebEngine state changed: {} -> {}", oldState, newState);

                if (newState == null) return;

                switch (newState) {
                    case READY -> loadingBar.setVisible(false);
                    case SCHEDULED -> {
                        loadingBar.setVisible(true);
                        loadingBar.setProgress(0);
                    }
                    case RUNNING -> {
                        loadingBar.setVisible(true);
                        loadingBar.setProgress(-1);
                    }
                    case SUCCEEDED -> {
                        loadingBar.setVisible(false);
                        loadingBar.setProgress(1.0);

                        extractFaviconFromPage();

                        reapplyDarkModeIfEnabled();
                        logger.info("Page loaded successfully: {}", webEngine.getLocation());
                    }
                    case CANCELLED -> {
                        loadingBar.setVisible(false);

                        logger.debug("Page load cancelled");
                    }
                    case FAILED -> {
                        loadingBar.setVisible(false);
                        Throwable ex = webEngine.getLoadWorker().getException();
                        if (ex != null) {
                            logger.error("Page load failed: {}", ex.getMessage());
                            showErrorView("Failed to load page: " + ex.getMessage());
                        }
                    }
                }
            });
        };
        webEngine.getLoadWorker().stateProperty().addListener(stateChangeListener);

        progressChangeListener = (obs, oldProgress, newProgress) -> {
            Platform.runLater(() -> {
                double progress = newProgress.doubleValue();
                if (progress >= 0 && progress < 1.0) {
                    loadingBar.setProgress(progress);
                } else if (progress >= 1.0) {
                    loadingBar.setProgress(1.0);
                }
            });
        };
        webEngine.getLoadWorker().progressProperty().addListener(progressChangeListener);

        exceptionChangeListener = (obs, oldEx, newEx) -> {
            if (newEx != null) {
                logger.error("WebEngine exception: {}", newEx.getMessage(), newEx);
                Platform.runLater(() -> loadingBar.setVisible(false));
            }
        };
        webEngine.getLoadWorker().exceptionProperty().addListener(exceptionChangeListener);

        webEngine.setOnError(event -> logger.error("WebEngine error event: {}", event.getMessage()));
    }

    private void updateFaviconUrl(String pageUrl) {
        if (pageUrl == null || pageUrl.isEmpty()) {
            faviconUrlProperty.set("");
            return;
        }

        try {
            URI uri = new URI(pageUrl);
            String host = uri.getHost();
            if (host != null) {

                String faviconUrl = "https://www.google.com/s2/favicons?domain=" + host + "&sz=32";
                faviconUrlProperty.set(faviconUrl);
            }
        } catch (Exception e) {
            logger.debug("Could not parse URL for favicon: {}", pageUrl);
            faviconUrlProperty.set("");
        }
    }

    private void extractFaviconFromPage() {
        try {

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

            logger.debug("Could not extract favicon from page", e);
        }
    }

    public WritableImage getPreviewSnapshot() {
        if (disposed || webView == null) return null;
        try {
            double w = Math.max(1, webView.getWidth());
            double h = Math.max(1, webView.getHeight());

            final double MAX_W = 320;
            final double MAX_H = 200;

            double scale = Math.min(1.0, Math.min(MAX_W / w, MAX_H / h));

            SnapshotParameters params = new SnapshotParameters();
            params.setTransform(new Scale(scale, scale));

            int outW = Math.max(1, (int) Math.round(w * scale));
            int outH = Math.max(1, (int) Math.round(h * scale));

            WritableImage out = new WritableImage(outW, outH);
            return webView.snapshot(params, out);
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

    public void loadUrl(String url) {
        if (disposed) return;

        String processedUrl = url;

        if (url != null && !url.isEmpty()) {
            String trimmedUrl = url.trim();

            if (!trimmedUrl.contains(".") && !trimmedUrl.startsWith("http") && !trimmedUrl.startsWith("file")) {

                processedUrl = "https://duckduckgo.com/?q=" + trimmedUrl.replace(" ", "+");
            } else if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://") &&
                !trimmedUrl.startsWith("file://") && !trimmedUrl.startsWith("data:")) {
                processedUrl = "https://" + trimmedUrl;
            } else {
                processedUrl = trimmedUrl;
            }
        }

        final String finalUrl = processedUrl;

        Platform.runLater(() -> {
            loadingBar.setVisible(true);
            loadingBar.setProgress(-1);
        });

        Platform.runLater(() -> {
            try {

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

        print();
    }

    public void find(String searchText, boolean forward, boolean matchCase, boolean findNext) {
        if (disposed || searchText == null || searchText.isEmpty()) return;
        Platform.runLater(() -> {
            try {

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
        Platform.runLater(() -> webView.setZoom(webView.getZoom() * 1.1));
    }

    public void zoomOut() {
        if (disposed) return;
        Platform.runLater(() -> webView.setZoom(webView.getZoom() / 1.1));
    }

    public void resetZoom() {
        if (disposed) return;
        Platform.runLater(() -> webView.setZoom(1.0));
    }

    public void setZoomLevel(double zoomLevel) {
        if (disposed) return;
        Platform.runLater(() -> webView.setZoom(zoomLevel));
    }

    public void setViewportZoom(double zoomLevel) {
        if (disposed) return;
        this.viewportZoom = zoomLevel;

        if (zoomLevel <= 1.0) {
            stopSmoothScrollAnimation();
        }

        Platform.runLater(() -> {

            Scale scale = new Scale(zoomLevel, zoomLevel, 0, 0);
            webView.getTransforms().clear();
            webView.getTransforms().add(scale);

            if (zoomLevel > 1.0) {
                scrollPane.setFitToWidth(false);
                scrollPane.setFitToHeight(false);
                scrollPane.setPannable(true);

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

    public double getViewportZoom() {
        return viewportZoom;
    }

    private double scrollSpeedX = 0;
    private double scrollSpeedY = 0;
    private javafx.animation.AnimationTimer scrollAnimator;
    private boolean scrollAnimatorRunning = false;
    private double lastMouseX = 0.5;
    private double lastMouseY = 0.5;

    public void scrollViewportSmooth(double relativeX, double relativeY) {
        if (disposed || viewportZoom <= 1.0) return;

        lastMouseX = relativeX;
        lastMouseY = relativeY;

        double edgeZone = 0.20;

        double baseSpeed = 0.025;
        double zoomBonus = Math.min((viewportZoom - 1.0) * 0.3, 1.0);
        double maxSpeed = baseSpeed * (1.0 + zoomBonus);

        if (relativeX < edgeZone) {

            double intensity = 1.0 - (relativeX / edgeZone);
            scrollSpeedX = -maxSpeed * Math.pow(intensity, 1.3);
        } else if (relativeX > (1.0 - edgeZone)) {

            double intensity = (relativeX - (1.0 - edgeZone)) / edgeZone;
            scrollSpeedX = maxSpeed * Math.pow(intensity, 1.3);
        } else if (relativeX <= 0) {

            scrollSpeedX = -maxSpeed * 1.5;
        } else if (relativeX >= 1.0) {

            scrollSpeedX = maxSpeed * 1.5;
        } else {

            scrollSpeedX = 0;
        }

        if (relativeY < edgeZone) {

            double intensity = 1.0 - (relativeY / edgeZone);
            scrollSpeedY = -maxSpeed * Math.pow(intensity, 1.3);
        } else if (relativeY > (1.0 - edgeZone)) {

            double intensity = (relativeY - (1.0 - edgeZone)) / edgeZone;
            scrollSpeedY = maxSpeed * Math.pow(intensity, 1.3);
        } else if (relativeY <= 0) {

            scrollSpeedY = -maxSpeed * 1.5;
        } else if (relativeY >= 1.0) {

            scrollSpeedY = maxSpeed * 1.5;
        } else {

            scrollSpeedY = 0;
        }

        if ((scrollSpeedX != 0 || scrollSpeedY != 0) && !scrollAnimatorRunning) {
            startSmoothScrollAnimation();
        }
    }

    public void updateScrollFromLastPosition() {
        scrollViewportSmooth(lastMouseX, lastMouseY);
    }

    private void startSmoothScrollAnimation() {
        if (scrollAnimator != null) {
            scrollAnimator.stop();
        }

        scrollAnimatorRunning = true;
        scrollAnimator = new javafx.animation.AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (disposed || viewportZoom <= 1.0) {
                    stopSmoothScrollAnimation();
                    return;
                }

                if (lastUpdate > 0 && (now - lastUpdate) < 16_000_000) {
                    return;
                }
                lastUpdate = now;

                double currentH = scrollPane.getHvalue();
                double currentV = scrollPane.getVvalue();

                double newH = currentH + scrollSpeedX;
                double newV = currentV + scrollSpeedY;

                newH = Math.max(0, Math.min(1, newH));
                newV = Math.max(0, Math.min(1, newV));

                scrollPane.setHvalue(newH);
                scrollPane.setVvalue(newV);

                if (scrollSpeedX == 0 && scrollSpeedY == 0) {
                    stopSmoothScrollAnimation();
                }
            }
        };
        scrollAnimator.start();
    }

    public void stopSmoothScrollAnimation() {
        scrollAnimatorRunning = false;
        scrollSpeedX = 0;
        scrollSpeedY = 0;
        if (scrollAnimator != null) {
            scrollAnimator.stop();
            scrollAnimator = null;
        }
    }

    public void scrollViewportTo(double relativeX, double relativeY) {
        if (disposed || viewportZoom <= 1.0) return;

        Platform.runLater(() -> {
            scrollPane.setHvalue(Math.max(0, Math.min(1, relativeX)));
            scrollPane.setVvalue(Math.max(0, Math.min(1, relativeY)));
        });
    }

    public ScrollPane getScrollPane() {
        return scrollPane;
    }

    public void showDevTools() {

        logger.info("Developer tools not available in WebView mode");
    }

    public void showDevTools(String mode) {

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
        if (disposed) return;
        disposed = true;

        stopSmoothScrollAnimation();

        try {
            if (viewportBoundsListener != null) scrollPane.viewportBoundsProperty().removeListener(viewportBoundsListener);
            if (titleChangeListener != null) webEngine.titleProperty().removeListener(titleChangeListener);
            if (locationChangeListener != null) webEngine.locationProperty().removeListener(locationChangeListener);
            if (stateChangeListener != null) webEngine.getLoadWorker().stateProperty().removeListener(stateChangeListener);
            if (progressChangeListener != null) webEngine.getLoadWorker().progressProperty().removeListener(progressChangeListener);
            if (exceptionChangeListener != null) webEngine.getLoadWorker().exceptionProperty().removeListener(exceptionChangeListener);
        } catch (Exception e) {
            logger.debug("Error removing listeners during dispose", e);
        }

        try {
            this.setOnMouseMoved(null);
            this.setOnMouseDragged(null);
            this.setOnMouseExited(null);
            this.setOnKeyPressed(null);
            if (scrollPane != null) {
                scrollPane.setOnMouseMoved(null);
                scrollPane.setOnMouseDragged(null);
            }

            webEngine.setOnAlert(null);
            webEngine.setOnError(null);
            webEngine.setCreatePopupHandler(null);

            try {
                webEngine.getLoadWorker().cancel();
            } catch (Exception ignore) {}

            Platform.runLater(() -> {
                try {
                    webEngine.load(null);
                    contentPane.getChildren().clear();
                } catch (Exception ignore) {
                }
            });
        } catch (Exception e) {
            logger.debug("Error cleaning up BrowserTab", e);
        }

        logger.info("BrowserTab disposed");
    }

    private static final String DARK_MODE_CSS = """
        html {
            filter: invert(90%) hue-rotate(180deg) !important;
            background-color: #1a1a1a !important;
        }
        img, video, canvas, picture, svg, [style*="background-image"],
        iframe, embed, object {
            filter: invert(100%) hue-rotate(180deg) !important;
        }
        /* Fix specific elements that shouldn't be inverted */
        [data-darkreader-inline-bgcolor],
        [data-darkreader-inline-color] {
            filter: none !important;
        }

""";

    private static final String DARK_MODE_CSS_SOFT = """
        :root {
            color-scheme: dark !important;
        }
        html, body {
            background-color: #1e1e1e !important;
            color: #e0e0e0 !important;
        }
        * {
            background-color: inherit !important;
            color: inherit !important;
            border-color: #404040 !important;
        }
        a {
            color: #6db3f2 !important;
        }
        a:visited {
            color: #b794f4 !important;
        }
        img, video, canvas, picture, svg, iframe {
            opacity: 0.9 !important;
        }
        input, textarea, select, button {
            background-color: #2d2d2d !important;
            color: #e0e0e0 !important;
            border-color: #505050 !important;
        }
        """;

    public void enableWebPageDarkMode() {
        if (disposed) return;
        webPageDarkMode = true;
        injectDarkModeCSS();
    }

    public void disableWebPageDarkMode() {
        if (disposed) return;
        webPageDarkMode = false;
        removeDarkModeCSS();
    }

    public void toggleWebPageDarkMode() {
        if (webPageDarkMode) {
            disableWebPageDarkMode();
        } else {
            enableWebPageDarkMode();
        }
    }

    public boolean isWebPageDarkModeEnabled() {
        return webPageDarkMode;
    }

    public static void setGlobalDarkModeEnabled(boolean enabled) {
        globalDarkModeEnabled = enabled;
    }

    public static boolean isGlobalDarkModeEnabled() {
        return globalDarkModeEnabled;
    }

    private void injectDarkModeCSS() {
        if (disposed) return;

        Platform.runLater(() -> {
            try {
                String escapedCSS = DARK_MODE_CSS
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", " ")
                    .replace("\r", "");

                String script = """
                    (function() {
                        // Remove existing dark mode style if any
                        var existing = document.getElementById('nexus-dark-mode');
                        if (existing) existing.remove();

                        // Create and inject dark mode style
                        var style = document.createElement('style');
                        style.id = 'nexus-dark-mode';
                        style.type = 'text/css';
                        style.innerHTML = '%s';
                        document.head.appendChild(style);
                        return true;
                    })()
                    """.formatted(escapedCSS);

                webEngine.executeScript(script);
                logger.info("Dark mode CSS injected");
            } catch (Exception e) {
                logger.debug("Error injecting dark mode CSS: {}", e.getMessage());
            }
        });
    }

    private void removeDarkModeCSS() {
        if (disposed) return;

        Platform.runLater(() -> {
            try {
                String script = """
                    (function() {
                        var existing = document.getElementById('nexus-dark-mode');
                        if (existing) existing.remove();
                        return true;
                    })()
                    """;

                webEngine.executeScript(script);
                logger.info("Dark mode CSS removed");
            } catch (Exception e) {
                logger.debug("Error removing dark mode CSS: {}", e.getMessage());
            }
        });
    }

    private void reapplyDarkModeIfEnabled() {
        if (webPageDarkMode || globalDarkModeEnabled) {
            webPageDarkMode = true;

            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));
            delay.setOnFinished(e -> injectDarkModeCSS());
            delay.play();
        }
    }

    private String suggestFileNameFromUrl(String urlStr) {
        try {
            URI u = new URI(urlStr);
            String path = u.getPath();
            if (path != null && !path.isEmpty()) {
                String last = path.substring(path.lastIndexOf('/') + 1);
                if (last == null || last.isEmpty()) last = "download";
                return java.net.URLDecoder.decode(last, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {

        }
        return "download";
    }
}
