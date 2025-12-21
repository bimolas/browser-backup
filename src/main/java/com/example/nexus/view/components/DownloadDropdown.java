package com.example.nexus.view.components;

import com.example.nexus.model.Download;
import com.example.nexus.service.DownloadListener;
import com.example.nexus.service.DownloadService;
import com.example.nexus.controller.DownloadController;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Small dropdown UI that shows the latest downloads (up to 3) with progress.
 */
public class DownloadDropdown implements DownloadListener {
    private static final Logger logger = LoggerFactory.getLogger(DownloadDropdown.class);
    private final DownloadService downloadService;
    private final Popup popup = new Popup();
    private final VBox content = new VBox(8);
    private BorderPane popupRoot;
    private final boolean dark;
    private final DownloadController downloadController;
    private final Runnable openPanelAction;

    private final String openIconLiteral = "mdi2f-folder-open-outline"; // use materialdesign2 literal
    private final String openIconLiteralAlt = "mdi2f-folder-open";

    // Expose popup visible state for toggling from controller
    public boolean isShowing() {
        try { return popup.isShowing(); } catch (Exception ignored) { return false; }
    }

    public DownloadDropdown(DownloadService downloadService, DownloadController downloadController, boolean dark, Runnable openPanelAction) {
        this.downloadService = downloadService;
        this.downloadController = downloadController;
        this.dark = dark;
        this.openPanelAction = openPanelAction;
        initPopup();
        // register as listener
        this.downloadService.addListener(this);
    }

    private void initPopup() {
        content.setPadding(new Insets(8));
        content.getStyleClass().addAll("download-dropdown","card");
        content.setPrefWidth(420);

        popupRoot = new BorderPane(content);
        popupRoot.getStyleClass().add("download-dropdown-root");
        // Add stylesheet directly to the root so Popup content uses it
        var res = getClass().getResource(this.dark ? "/com/example/nexus/css/dark.css" : "/com/example/nexus/css/main.css");
        if (res != null) {
            try { popupRoot.getStylesheets().add(res.toExternalForm()); } catch (Exception ignored) {}
        }

        // Apply theme class to popup root so .root.dark rules in CSS apply
        if (this.dark) popupRoot.getStyleClass().addAll("root", "dark");

        // ensure transparent wrapper so shadow from inner card shows
        popupRoot.setStyle("-fx-background-color: transparent;");

        popup.getContent().add(popupRoot);
        popup.setAutoHide(true);
        popup.setAutoFix(true);
    }

    public void showNear(Window owner, Bounds anchorBounds) {
        if (owner == null || anchorBounds == null) return;
        if (popup.isShowing()) popup.hide();
        double x = owner.getX() + anchorBounds.getMinX();
        double y = owner.getY() + anchorBounds.getMaxY() + 6;
        popup.show(owner, x, y);
        refreshContent();
        // Mark as showing so CSS transition rules apply
        try { if (!popupRoot.getStyleClass().contains("showing")) popupRoot.getStyleClass().add("showing"); } catch (Exception ignored) {}
        // animate popup root (fade + translate) to have a smooth entrance
        try {
            if (popupRoot != null) {
                popupRoot.setOpacity(0);
                popupRoot.setTranslateY(-6);
                javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(160), popupRoot);
                ft.setFromValue(0);
                ft.setToValue(1);
                javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(160), popupRoot);
                tt.setFromY(-6);
                tt.setToY(0);
                ft.play(); tt.play();
            }
        } catch (Exception ignored) {}
    }

    public void hide() {
        if (popup.isShowing()) popup.hide();
        try { popupRoot.getStyleClass().remove("showing"); } catch (Exception ignored) {}
    }

    // Ensure when popup is hidden by external clicks we remove the showing class
    {
        popup.setOnHidden(e -> {
            try { popupRoot.getStyleClass().remove("showing"); } catch (Exception ignored) {}
        });
    }

    public void refreshContent() {
        Platform.runLater(() -> {
            content.getChildren().clear();
            List<Download> latest = downloadService.getAllDownloads().stream()
                .sorted(Comparator.comparing(Download::getStartTime).reversed())
                .limit(3)
                .collect(Collectors.toList());

            if (latest.isEmpty()) {
                Label empty = new Label("No recent downloads");
                empty.getStyleClass().add("download-dropdown-empty");
                content.getChildren().add(empty);
            } else {
                for (Download d : latest) {
                    VBox card = new VBox(6);
                    card.getStyleClass().add("download-card-row");
                    card.setPadding(new Insets(8));

                    HBox top = new HBox(8);
                    top.setAlignment(Pos.CENTER_LEFT);
                    // Use a safe icon creator to avoid IllegalArgumentException for invalid literals
                    javafx.scene.Node fileIcon = createSafeIcon("mdi-file-outline", 18, "#6b7280");
                    Label name = new Label(d.getFileName());
                    name.getStyleClass().addAll("download-dropdown-name");
                    name.setMaxWidth(340);
                    name.setTooltip(new javafx.scene.control.Tooltip(d.getFileName()));
                    Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
                    Label status = new Label(d.getStatus()); status.getStyleClass().add("download-dropdown-percent");
                    top.getChildren().addAll(fileIcon, name, sp, status);

                    ProgressBar pb = new ProgressBar();
                    pb.getStyleClass().addAll("download-progress","progress-bar");
                    pb.setPrefWidth(400);
                    pb.setPrefHeight(10);
                    if (d.getFileSize() > 0) animateProgress(pb, d.getProgress()); else pb.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

                    HBox metaRow = new HBox(8);
                    metaRow.setAlignment(Pos.CENTER_LEFT);
                    Label meta = new Label(); meta.getStyleClass().add("download-dropdown-percent");
                    if (d.getFileSize() > 0) meta.setText(String.format("%.0f%% • %s", d.getProgress() * 100, formatShortSize(d))); else meta.setText(formatShortSize(d));
                    // if completed, show timestamp
                    if ("completed".equalsIgnoreCase(d.getStatus()) && d.getEndTime() != null) {
                        meta.setText(meta.getText() + " • " + formatDateTime(d.getEndTime()));
                    }
                    Region metaSpacer = new Region(); HBox.setHgrow(metaSpacer, Priority.ALWAYS);

                    // actions (small text buttons to mirror Settings look)
                    HBox actions = new HBox(6);
                    actions.setAlignment(Pos.CENTER_RIGHT);
                    // Use iconButton helper which silently fails to a fallback if literal invalid
                    Button openBtn = iconButton(openIconLiteral, "Open", "small");
                    openBtn.getStyleClass().add("icon-button");
                    openBtn.setOnAction(e -> { try { downloadController.openDownloadFile(d); } catch (Exception ex) { logger.debug("Failed to open download file {}", d.getId(), ex); } });
                    Button del = iconButton("mdi2d-delete", "Delete", "small");
                    del.setOnAction(e -> { try { downloadController.deleteDownload(d.getId()); refreshContent(); } catch (Exception ex) { logger.debug("Failed to delete download {}", d.getId(), ex); } });
                    actions.getChildren().addAll(openBtn, del);

                    metaRow.getChildren().addAll(meta, metaSpacer, actions);

                    card.getChildren().addAll(top, pb, metaRow);
                    content.getChildren().add(card);
                }
                // footer
                HBox footer = new HBox(8);
                footer.setAlignment(Pos.CENTER_RIGHT);
                Button openPanel = new Button("Open downloads");
                openPanel.getStyleClass().addAll("primary-button");
                openPanel.setOnAction(e -> { if (openPanelAction != null) openPanelAction.run(); popup.hide(); });
                footer.getChildren().add(openPanel);
                content.getChildren().add(footer);
            }
        });
    }

    // Safe icon creation: try to create a FontIcon, fall back to a small label glyph if invalid
    private javafx.scene.Node createSafeIcon(String iconLiteral, int size, String colorHex) {
        try {
            FontIcon fi = new FontIcon(iconLiteral);
            fi.setIconSize(size);
            if (colorHex != null && !colorHex.isBlank()) fi.setIconColor(javafx.scene.paint.Color.web(colorHex));
            return fi;
        } catch (Throwable t) {
            // Fallback: use a small label with a simple emoji/glyph to avoid throwing
            Label fallback = new Label("\uD83D\uDCC4"); // page emoji as generic file icon
            fallback.setStyle("-fx-font-size: " + Math.max(10, size-4) + "px; -fx-text-fill: " + (colorHex != null ? colorHex : "#6b7280") + ";");
            return fallback;
        }
    }

    // Smoothly animate a ProgressBar to a new value on the FX thread
    private void animateProgress(ProgressBar pb, double target) {
        Platform.runLater(() -> {
            try {
                double from = pb.getProgress();
                javafx.animation.Timeline t = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(javafx.util.Duration.ZERO, new javafx.animation.KeyValue(pb.progressProperty(), from)),
                    new javafx.animation.KeyFrame(javafx.util.Duration.millis(360), new javafx.animation.KeyValue(pb.progressProperty(), target, javafx.animation.Interpolator.EASE_BOTH))
                );
                t.play();
            } catch (Exception ignored) {}
        });
    }

    // DownloadListener callbacks
    @Override
    public void downloadAdded(Download download) {
        // Refresh dropdown content when a download is added. Anchoring/showing is handled by caller (MainController).
        Platform.runLater(this::refreshContent);
    }

    @Override
    public void downloadUpdated(Download download) {
        // ensure UI refresh runs on FX thread
        Platform.runLater(this::refreshContent);
    }

    @Override
    public void downloadRemoved(int downloadId) {
        refreshContent();
    }

    public void dispose() {
        downloadService.removeListener(this);
        hide();
    }

    // small helper to create icon-only buttons with consistent style
    private Button iconButton(String iconLiteral, String tooltip, String styleClass) {
        Button b = new Button();
        b.getStyleClass().addAll("download-action-button", styleClass);
        try {
            FontIcon fi = new FontIcon(iconLiteral);
            fi.setIconSize(14);
            // Do not set icon color here; let CSS control .ikonli-font-icon color based on button classes
            b.setGraphic(fi);
        } catch (Exception ignored) {}
        if (tooltip != null) b.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        return b;
    }

    private static String formatShortSize(Download d) {
        if (d.getFileSize() > 0) return formatBytes(d.getDownloadedSize()) + " / " + formatBytes(d.getFileSize());
        if (d.getDownloadedSize() > 0) return formatBytes(d.getDownloadedSize());
        return "-";
    }

    private static String formatBytes(long b) {
        if (b <= 0) return "0 B";
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int u = 0; double val = b;
        while (val >= 1024 && u < units.length -1) { val /= 1024; u++; }
        return String.format("%.1f %s", val, units[u]);
    }

    private static String formatDateTime(java.time.LocalDateTime dt) {
        if (dt == null) return "";
        java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");
        return dt.format(f);
    }
}
