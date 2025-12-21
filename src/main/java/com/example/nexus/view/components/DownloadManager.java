package com.example.nexus.view.components;
import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Download;
import com.example.nexus.service.DownloadService;
import io.github.palexdev.materialfx.controls.MFXProgressBar;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;

public class DownloadManager extends BorderPane {
    private final DIContainer container;
    private static final Logger logger = LoggerFactory.getLogger(DownloadManager.class);
    private final DownloadService downloadService;
    private final ObservableList<Download> downloadList;
    private final ListView<Download> downloadListView;

    public DownloadManager(DIContainer container) {
        this.container = container;
        this.downloadService = container.getOrCreate(DownloadService.class);
        this.downloadList = FXCollections.observableArrayList();
        this.downloadListView = new ListView<>(downloadList);

        initializeUI();
        loadDownloads();
    }

    private void initializeUI() {
        // Set up the header
        Label titleLabel = new Label("Downloads");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        Button clearButton = new Button("Clear All");
        clearButton.setGraphic(new FontIcon("mdi-delete"));
        clearButton.setOnAction(e -> clearDownloads());

        HBox headerBox = new HBox(10, titleLabel, clearButton);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(10));

        // Set up the download list
        downloadListView.setCellFactory(param -> new DownloadCell());

        // Set up the layout
        setTop(headerBox);
        setCenter(new MFXScrollPane(downloadListView));

        // Set up the bottom
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> close());

        HBox bottomBox = new HBox(closeButton);
        bottomBox.setAlignment(Pos.CENTER_RIGHT);
        bottomBox.setPadding(new Insets(10));

        setBottom(bottomBox);
    }

    private void loadDownloads() {
        downloadList.clear();
        downloadList.addAll(downloadService.getAllDownloads());
    }

    private void clearDownloads() {
        downloadService.clearDownloads();
        downloadList.clear();
    }

    private void close() {
        Stage stage = (Stage) getScene().getWindow();
        stage.close();
    }

    public void show() {
        Stage stage = new Stage();
        stage.setTitle("Downloads");
        stage.setScene(new javafx.scene.Scene(this, 800, 600));
        stage.show();
    }

    private class DownloadCell extends ListCell<Download> {
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        @Override
        protected void updateItem(Download item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                VBox vbox = new VBox(5);

                HBox titleBox = new HBox(10);
                titleBox.setAlignment(Pos.CENTER_LEFT);

                Label titleLabel = new Label(item.getFileName());
                titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                HBox.setHgrow(titleLabel, Priority.ALWAYS);

                Label statusLabel = new Label(item.getStatus());
                statusLabel.setStyle("-fx-text-fill: #666;");

                titleBox.getChildren().addAll(titleLabel, statusLabel);

                HBox progressBox = new HBox(10);
                progressBox.setAlignment(Pos.CENTER_LEFT);

                MFXProgressBar progressBar = new MFXProgressBar();
                progressBar.setProgress(item.getProgress());
                HBox.setHgrow(progressBar, Priority.ALWAYS);

                Label progressLabel = new Label(String.format("%.1f%%", item.getProgress() * 100));

                progressBox.getChildren().addAll(progressBar, progressLabel);

                Label dateLabel = new Label(item.getStartTime().format(formatter));
                dateLabel.setStyle("-fx-text-fill: #999;");

                // Action buttons
                HBox buttonBox = new HBox(5);

                Button openButton = new Button("Open");
                openButton.setOnAction(e -> {
                    try {
                        java.io.File f = new java.io.File(item.getFilePath());
                        if (f.exists()) {
                            new ProcessBuilder("xdg-open", f.getAbsolutePath()).start();
                        } else {
                            // open folder if file missing
                            new ProcessBuilder("xdg-open", new java.io.File(item.getFilePath()).getParent()).start();
                        }
                    } catch (Exception ex) {
                        logger.warn("Failed to open download file", ex);
                    }
                });

                Button pauseButton = new Button("Pause");
                pauseButton.setOnAction(e -> pauseDownload(item));

                Button cancelButton = new Button("Cancel");
                cancelButton.setOnAction(e -> cancelDownload(item));

                buttonBox.getChildren().addAll(pauseButton, cancelButton, openButton);

                vbox.getChildren().addAll(titleBox, progressBox, dateLabel, buttonBox);

                setGraphic(vbox);
                setText(null);
            }
        }

        private void pauseDownload(Download download) {
            try {
                downloadService.pauseDownload(download.getId());
                loadDownloads();
            } catch (Exception e) {
                logger.warn("Failed to pause download {}", download.getId(), e);
            }
        }

        private void cancelDownload(Download download) {
            downloadService.cancelDownload(download.getId());
            loadDownloads();
        }

        private void openDownload(Download download) {
            try {
                java.io.File f = new java.io.File(download.getFilePath());
                if (f.exists()) {
                    new ProcessBuilder("xdg-open", f.getAbsolutePath()).start();
                } else if (f.getParentFile() != null) {
                    new ProcessBuilder("xdg-open", f.getParent()).start();
                }
            } catch (Exception e) {
                logger.warn("Failed to open download {}", download.getId(), e);
            }
        }
    }
}
