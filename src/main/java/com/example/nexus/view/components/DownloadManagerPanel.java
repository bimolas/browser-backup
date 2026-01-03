package com.example.nexus.view.components;

import com.example.nexus.controller.DownloadController;
import com.example.nexus.model.Download;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DownloadManagerPanel extends Stage {
    private final DownloadController downloadController;
    private final boolean isDarkTheme;
    private final ListView<Download> downloadListView;
    private final ScheduledExecutorService refresher = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });

    public DownloadManagerPanel(com.example.nexus.core.DIContainer container, DownloadController downloadController, boolean isDarkTheme) {

        this.downloadController = downloadController;
        this.isDarkTheme = isDarkTheme;
        this.downloadListView = new ListView<>();
        setTitle("Downloads");
        initModality(Modality.NONE);
        initStyle(StageStyle.DECORATED);
        setMinWidth(700);
        setMinHeight(400);
        setWidth(800);
        setHeight(500);
        initializeUI();
        loadDownloads();

        try {
            var downloadService = container.getOrCreate(com.example.nexus.service.DownloadService.class);
            downloadService.addListener(new com.example.nexus.service.DownloadListener() {
                @Override
                public void downloadAdded(com.example.nexus.model.Download download) { Platform.runLater(() -> loadDownloads()); }

                @Override
                public void downloadUpdated(com.example.nexus.model.Download download) { Platform.runLater(() -> loadDownloads()); }

                @Override
                public void downloadRemoved(int downloadId) { Platform.runLater(() -> loadDownloads()); }
            });
        } catch (Exception ignored) {

            refresher.scheduleAtFixedRate(this::loadDownloads, 1, 1, TimeUnit.SECONDS);
        }
    }

    private void initializeUI() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("download-panel");

        HBox header = new HBox(10);
        Label title = new Label("Downloads");
        title.getStyleClass().addAll("download-title","modern-title");

        TextField searchField = new TextField();
        searchField.setPromptText("Search downloads...");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((obs, oldV, newV) -> loadDownloads(newV));

        Label searchIcon = new Label("\uD83D\uDD0D");
        searchIcon.getStyleClass().add("search-icon");
        HBox searchBox = new HBox(6, searchIcon, searchField);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        Button clearBtn = new Button("Clear All");
        clearBtn.getStyleClass().addAll("action-button","secondary-button");
        clearBtn.setOnAction(e -> { downloadController.clearAllDownloads(); loadDownloads(); });
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, searchBox, spacer, clearBtn);
        header.setPadding(new Insets(16));
        root.setTop(header);

        downloadListView.setCellFactory(lv -> new DownloadCell());
        root.setCenter(downloadListView);

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().addAll("action-button","secondary-button");
        closeBtn.setOnAction(e -> { refresher.shutdownNow(); close(); });
        HBox footer = new HBox(closeBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12));
        root.setBottom(footer);
        Scene scene = new Scene(root);
        String cssPath = isDarkTheme ? "/com/example/nexus/css/dark.css" : "/com/example/nexus/css/main.css";
        var res = getClass().getResource(cssPath);
        if (res != null) scene.getStylesheets().add(res.toExternalForm());
        setScene(scene);
    }

    private void loadDownloads() { loadDownloads(null); }

    private void loadDownloads(String filter) {
        final String f = (filter == null) ? "" : filter.trim().toLowerCase();
        Platform.runLater(() -> {
            List<Download> downloads = downloadController.getAllDownloads();
            if (!f.isEmpty()) {
                downloads = downloads.stream()
                    .filter(d -> (d.getFileName() != null && d.getFileName().toLowerCase().contains(f)) ||
                                 (d.getUrl() != null && d.getUrl().toLowerCase().contains(f)))
                    .toList();
            }
            int sel = downloadListView.getSelectionModel().getSelectedIndex();
            downloadListView.getItems().setAll(downloads);
            if (sel >= 0 && sel < downloadListView.getItems().size()) downloadListView.getSelectionModel().select(sel);
        });
    }

    private class DownloadCell extends ListCell<Download> {
        @Override
        protected void updateItem(Download item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null); setGraphic(null);
            } else {
                VBox vbox = new VBox(6);
                vbox.setPadding(new Insets(8));
                HBox topRow = new HBox(8);
                topRow.setAlignment(Pos.CENTER_LEFT);

                Label fileTypeIcon = new Label(getFileTypeIcon(item.getFileName()));
                fileTypeIcon.getStyleClass().add("file-type-icon");

                Label name = new Label(item.getFileName());
                name.getStyleClass().add("download-item-title");
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                Label status = new Label(item.getStatus());
                status.getStyleClass().add("status");
                topRow.getChildren().addAll(fileTypeIcon, name, spacer, status);

                ProgressBar pb = new ProgressBar();
                pb.getStyleClass().addAll("download-progress","progress-bar");
                pb.setPrefWidth(480);
                pb.setPrefHeight(10);
                if (item.getFileSize() > 0) {
                    pb.setProgress(item.getProgress());
                } else {
                    pb.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                }
                Label percent = new Label(item.getFileSize() > 0 ? String.format("%.0f%%", item.getProgress() * 100) : "");
                percent.getStyleClass().add("download-dropdown-percent");

                String sizeText = formatBytes(item.getDownloadedSize());
                if (item.getFileSize() > 0) sizeText += " / " + formatBytes(item.getFileSize());
                Label sizeLabel = new Label(sizeText);
                sizeLabel.getStyleClass().add("download-dropdown-percent");

                Label eta = new Label(estimateTimeLeft(item));
                eta.getStyleClass().add("download-dropdown-percent");

                if ("completed".equalsIgnoreCase(item.getStatus()) && item.getEndTime() != null) {
                    eta.setText(formatDateTime(item.getEndTime()));
                }

                Button openBtn = new Button();
                try {
                    FontIcon fi = new FontIcon("mdi2f-folder-open-outline");
                    fi.setIconSize(16);

                    openBtn.setGraphic(fi);
                } catch (Throwable t) {
                    Label openIcon = new Label("📁"); openIcon.getStyleClass().add("action-icon"); openBtn.setGraphic(openIcon);
                }
                openBtn.getStyleClass().addAll("icon-button", "open");
                openBtn.setTooltip(new Tooltip("Open file"));
                openBtn.setOnAction(e -> downloadController.openDownloadFile(item));

                Button deleteBtn = new Button();
                try {
                    FontIcon di = new FontIcon("mdi2d-delete");
                    di.setIconSize(14);

                    deleteBtn.setGraphic(di);
                } catch (Throwable t) {
                    Label deleteIcon = new Label("✖"); deleteIcon.getStyleClass().add("action-icon"); deleteBtn.setGraphic(deleteIcon);
                }
                deleteBtn.getStyleClass().addAll("icon-button","delete","danger-button");
                deleteBtn.setTooltip(new Tooltip("Delete download"));
                deleteBtn.setOnAction(e -> { downloadController.deleteDownload(item.getId()); loadDownloads(); });

                HBox rightActions = new HBox(8, openBtn, deleteBtn);
                rightActions.setAlignment(Pos.CENTER_RIGHT);

                HBox progressRow = new HBox(8);
                progressRow.setAlignment(Pos.CENTER_LEFT);
                Region midSpacer = new Region(); HBox.setHgrow(midSpacer, Priority.ALWAYS);
                VBox infoBox = new VBox(2, percent, sizeLabel);
                infoBox.setAlignment(Pos.CENTER_LEFT);

                progressRow.getChildren().addAll(pb, infoBox, midSpacer, rightActions);

                vbox.getChildren().addAll(topRow, progressRow);
                setGraphic(vbox); setText(null);
            }
        }

        private String getFileTypeIcon(String fileName) {
            if (fileName == null) return "📦";
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".webp")) return "🖼️";
            if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.endsWith(".mov")) return "🎬";
            if (lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".7z") || lower.endsWith(".tar.gz") || lower.endsWith(".tar.xz")) return "🗜️";
            if (lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".txt") || lower.endsWith(".rtf")) return "📄";
            return "📦";
        }
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

    private static String estimateTimeLeft(Download item) {
        if (item.getFileSize() <= 0) return "";
        long downloaded = item.getDownloadedSize();
        long total = item.getFileSize();
        if (downloaded <= 0) return "";
        java.time.LocalDateTime start = item.getStartTime();
        if (start == null) return "";
        long elapsedSecs = java.time.Duration.between(start, java.time.LocalDateTime.now()).getSeconds();
        if (elapsedSecs <= 0) return "";
        double speed = (double) downloaded / elapsedSecs;
        if (speed <= 0) return "";
        long remaining = total - downloaded;
        if (remaining <= 0) return "";
        long secs = (long) Math.ceil(remaining / speed);
        if (secs < 60) return secs + "s";
        long mins = secs / 60;
        if (mins < 60) return mins + "m";
        long hrs = mins / 60;
        return hrs + "h";
    }


    @Override
    public void close() {
        try { refresher.shutdownNow(); } catch (Exception ignored) {}
        super.close();
    }
}
