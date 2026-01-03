package com.example.nexus.service;

import com.example.nexus.view.components.BrowserTab;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

public class ZoomService {
    private double currentZoom = 1.0;
    private double viewportZoom = 1.0;
    private static final double ZOOM_STEP = 0.1;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 5.0;
    private static final double VIEWPORT_ZOOM_STEP = 0.2;
    private static final double MIN_VIEWPORT_ZOOM = 1.0;
    private static final double MAX_VIEWPORT_ZOOM = 5.0;
    private boolean mouseTrackingEnabled = false;
    private boolean viewportZoomMode = true;

    public void zoomIn(BrowserTab tab, Runnable notification) {
        if (tab != null) {
            if (viewportZoomMode) {
                viewportZoom = Math.min(MAX_VIEWPORT_ZOOM, viewportZoom + VIEWPORT_ZOOM_STEP);
                tab.setViewportZoom(viewportZoom);
            } else {
                currentZoom = Math.min(MAX_ZOOM, currentZoom + ZOOM_STEP);
                tab.setZoomLevel(currentZoom);
            }
            if (notification != null) notification.run();
        }
    }

    public void zoomOut(BrowserTab tab, Runnable notification) {
        if (tab != null) {
            if (viewportZoomMode) {
                double newZoom = viewportZoom - VIEWPORT_ZOOM_STEP;
                viewportZoom = Math.max(MIN_VIEWPORT_ZOOM, newZoom);
                tab.setViewportZoom(viewportZoom);
            } else {
                currentZoom = Math.max(MIN_ZOOM, currentZoom - ZOOM_STEP);
                tab.setZoomLevel(currentZoom);
            }
            if (notification != null) notification.run();
        }
    }

    public void resetZoom(BrowserTab tab, Runnable notification) {
        if (tab != null) {
            if (viewportZoomMode) {
                viewportZoom = 1.0;
                tab.setViewportZoom(viewportZoom);
            } else {
                currentZoom = 1.0;
                tab.setZoomLevel(currentZoom);
            }
            if (notification != null) notification.run();
        }
    }

    public void toggleZoomMode(Runnable callback) {

        boolean newState = !isViewportZoomMode();
        setViewportZoomMode(newState);

        if (callback != null) {
            callback.run();
        }
    }

    public void toggleMouseTracking(Runnable callback) {

        boolean newState = !isMouseTrackingEnabled();
        setMouseTrackingEnabled(newState);

        if (callback != null) {
            callback.run();
        }
    }

    public void setViewportZoomMode(boolean enabled) {
        this.viewportZoomMode = enabled;
    }
    public void setMouseTrackingEnabled(boolean enabled) {
        this.mouseTrackingEnabled = enabled;
    }

    public boolean isViewportZoomMode() { return viewportZoomMode; }
    public boolean isMouseTrackingEnabled() { return mouseTrackingEnabled; }
    public double getCurrentZoom() { return currentZoom; }
    public double getViewportZoom() { return viewportZoom; }

    public void handleScrollZoom(BrowserTab tab, double delta, Runnable notification) {
        if (tab != null) {
            if (viewportZoomMode) {
                if (delta > 0) {
                    viewportZoom = Math.min(MAX_VIEWPORT_ZOOM, viewportZoom + VIEWPORT_ZOOM_STEP);
                } else if (delta < 0) {
                    viewportZoom = Math.max(MIN_VIEWPORT_ZOOM, viewportZoom - VIEWPORT_ZOOM_STEP);
                }
                tab.setViewportZoom(viewportZoom);
            } else {
                if (delta > 0) {
                    currentZoom = Math.min(MAX_ZOOM, currentZoom + ZOOM_STEP);
                } else if (delta < 0) {
                    currentZoom = Math.max(MIN_ZOOM, currentZoom - ZOOM_STEP);
                }
                tab.setZoomLevel(currentZoom);
            }
            if (notification != null) notification.run();
        }
    }

    public void showZoomNotification(Stage stage) {
        double zoom = viewportZoomMode ? viewportZoom : currentZoom;
        int zoomPercent = (int) Math.round(zoom * 100);
        javafx.stage.Popup popup = new javafx.stage.Popup();
        String modeIndicator = viewportZoomMode ? "🔍 " : "";
        Label zoomLabel = new Label(modeIndicator + zoomPercent + "%");
        zoomLabel.getStyleClass().addAll("popup-notification","popup-label");
        popup.getContent().add(zoomLabel);
        popup.setAutoHide(true);
        Platform.runLater(() -> {
            if (stage.getScene() != null && stage.getScene().getWindow() != null) {
                Window window = stage.getScene().getWindow();
                double centerX = window.getX() + window.getWidth() / 2 - 40;
                double centerY = window.getY() + window.getHeight() / 2 - 20;
                popup.show(window, centerX, centerY);
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1500));
                pause.setOnFinished(e -> popup.hide());
                pause.play();
            }
        });
    }

    public void showZoomModeNotification(Stage stage) {
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
            if (stage.getScene() != null && stage.getScene().getWindow() != null) {
                Window window = stage.getScene().getWindow();
                double centerX = window.getX() + window.getWidth() / 2 - 90;
                double centerY = window.getY() + window.getHeight() / 2 - 20;
                popup.show(window, centerX, centerY);
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1500));
                pause.setOnFinished(e -> popup.hide());
                pause.play();
            }
        });
    }

    public void showMouseTrackingNotification(Stage stage) {
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
            if (stage.getScene() != null && stage.getScene().getWindow() != null) {
                Window window = stage.getScene().getWindow();
                double centerX = window.getX() + window.getWidth() / 2 - 80;
                double centerY = window.getY() + window.getHeight() / 2 - 20;
                popup.show(window, centerX, centerY);
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1500));
                pause.setOnFinished(e -> popup.hide());
                pause.play();
            }
        });
    }
}
