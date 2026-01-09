package com.example.nexus.controller;

import com.example.nexus.model.Download;
import com.example.nexus.service.DownloadService;
import com.example.nexus.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import com.example.nexus.view.components.DownloadDropdown;
import javafx.scene.control.Button;
import com.example.nexus.core.DIContainer;
import com.example.nexus.view.components.DownloadManagerPanel;

public class DownloadController {
    private static final Logger logger = LoggerFactory.getLogger(DownloadController.class);

    private final DownloadService downloadService;
    private final SettingsService settingsService;
    private DownloadManagerPanel downloadManagerPanel; // Track open download manager

    public DownloadController(DownloadService downloadService, SettingsService settingsService) {
        this.downloadService = downloadService;
        this.settingsService = settingsService;
    }

    /**
     * Close the download manager panel if it's open
     */
    public void closePanel() {
        if (downloadManagerPanel != null && downloadManagerPanel.isShowing()) {
            downloadManagerPanel.close();
            downloadManagerPanel = null;
        }
    }

    public void startDownload(String url, String filename) {
        try {
            String downloadPath = settingsService.getDownloadPath();
            if (downloadPath == null || downloadPath.trim().isEmpty()) {
                String userHome = System.getProperty("user.home", "");
                downloadPath = userHome + java.io.File.separator + "Downloads";
                logger.debug("Download path empty in settings; falling back to {}", downloadPath);
            }
            File targetDir = new File(downloadPath);
            if (!targetDir.exists()) {
                boolean created = targetDir.mkdirs();
                if (!created) {
                    logger.warn("Could not create download directory: {}", targetDir.getAbsolutePath());
                } else {
                    logger.info("Created download directory: {}", targetDir.getAbsolutePath());
                }
            }

            File targetFile = new File(targetDir, filename);

            File parent = targetFile.getParentFile();
            if (parent != null && !parent.exists()) {
                boolean ok = parent.mkdirs();
                if (!ok) logger.warn("Failed to create parent directory for file: {}", parent.getAbsolutePath());
            }

            downloadService.startDownload(url, filename, targetFile.getAbsolutePath());
            logger.info("Started download: {} -> {}", url, targetFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Error starting download: {}", url, e);
        }
    }

    public void startDownload(String url, String filename, String absolutePath) {
        try {
            downloadService.startDownload(url, filename, absolutePath);
            logger.info("Started download: {} -> {}", url, absolutePath);
        } catch (Exception e) {
            logger.error("Error starting download: {}", url, e);
        }
    }


    public List<Download> getAllDownloads() {
        return downloadService.getAllDownloads();
    }

    public void deleteDownload(int downloadId) {
        try {
            downloadService.deleteDownload(downloadId);
            logger.info("Deleted download: {}", downloadId);
        } catch (Exception e) {
            logger.error("Error deleting download: {}", downloadId, e);
        }
    }

    public void clearAllDownloads() {
        try {
            downloadService.clearDownloads();
            logger.info("Cleared all downloads");
        } catch (Exception e) {
            logger.error("Error clearing downloads", e);
        }
    }

    public void openDownloadLocation(Download download) {
        if (download != null && download.getFilePath() != null) {
            try {
                File file = new File(download.getFilePath());
                if (file.exists()) {
                    ProcessBuilder pb = new ProcessBuilder("xdg-open", file.getParent());
                    pb.start();
                }
            } catch (Exception e) {
                logger.error("Error opening download location", e);
            }
        }
    }

    public void openDownloadFile(Download download) {
        if (download != null && download.getFilePath() != null) {
            try {
                File file = new File(download.getFilePath());
                if (file.exists()) {
                    ProcessBuilder pb = new ProcessBuilder("xdg-open", file.getAbsolutePath());
                    pb.start();
                } else {

                    openDownloadLocation(download);
                }
            } catch (Exception e) {
                logger.error("Error opening download file", e);
            }
        }
    }


    public boolean shouldAskDownloadLocation() {
        return settingsService.isAskDownloadLocation();
    }

    public void requestDownloadFromUI(String url, String suggestedFileName) {
        try {
            if (shouldAskDownloadLocation()) {

                final java.util.concurrent.atomic.AtomicReference<java.io.File> chosen = new java.util.concurrent.atomic.AtomicReference<>();
                if (javafx.application.Platform.isFxApplicationThread()) {
                    javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
                    chooser.setInitialFileName(suggestedFileName);
                    chooser.setTitle("Save As");
                    java.io.File picked = chooser.showSaveDialog(null);
                    chosen.set(picked);
                } else {
                    final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                    javafx.application.Platform.runLater(() -> {
                        try {
                            javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
                            chooser.setInitialFileName(suggestedFileName);
                            chooser.setTitle("Save As");
                            java.io.File picked = chooser.showSaveDialog(null);
                            chosen.set(picked);
                        } finally {
                            latch.countDown();
                        }
                    });
                    try { latch.await(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                }

                java.io.File pick = chosen.get();
                if (pick == null) {
                    logger.info("User cancelled Save As for download: {}", url);
                    return;
                }
                startDownload(url, pick.getName(), pick.getAbsolutePath());
            } else {

                startDownload(url, suggestedFileName);
            }
        } catch (Exception e) {
            logger.error("Error requesting download from UI: {}", url, e);
        }
    }

    public DownloadDropdown createDownloadDropdown(DownloadService ds, boolean isDarkTheme, Button downloadsButton) {
        DownloadDropdown downloadDropdown = new DownloadDropdown(ds, this, isDarkTheme, () -> {
            try { showDownloadManagerPanel(null, null, null); } catch (Exception ex) { logger.warn("Failed to open downloads panel", ex); }
        });
        ds.addListener(new com.example.nexus.service.DownloadListener() {
            @Override
            public void downloadAdded(com.example.nexus.model.Download download) {
                javafx.application.Platform.runLater(() -> {
                    try {
                        updateDownloadsBadge(null, downloadsButton);
                        if (downloadsButton != null && downloadsButton.getScene() != null) {
                            var bounds = downloadsButton.localToScene(downloadsButton.getBoundsInLocal());
                            javafx.stage.Window w = downloadsButton.getScene().getWindow();
                            downloadDropdown.showNear(w, bounds);
                        }
                    } catch (Exception ignored) {}
                });
            }
            @Override
            public void downloadUpdated(com.example.nexus.model.Download download) {
                javafx.application.Platform.runLater(() -> { try { downloadDropdown.refreshContent(); } catch (Exception ignored) {} });
                javafx.application.Platform.runLater(() -> updateDownloadsBadge(null, downloadsButton));
            }
        });
        return downloadDropdown;
    }

    public void showDownloadManagerPanel(DIContainer container, SettingsService settingsService, com.example.nexus.controller.SettingsController settingsController) {
        showDownloadManagerPanel(container, settingsService, settingsController, null);
    }

    public void showDownloadManagerPanel(DIContainer container, SettingsService settingsService, com.example.nexus.controller.SettingsController settingsController, com.example.nexus.util.KeyboardShortcutManager shortcutManager) {
        // If download manager is already open, bring it to front
        if (downloadManagerPanel != null && downloadManagerPanel.isShowing()) {
            downloadManagerPanel.toFront();
            downloadManagerPanel.requestFocus();
            logger.info("Download manager already open, bringing to front");
            return;
        }

        boolean isDarkTheme = false;
        if (settingsService != null && settingsController != null) {
            isDarkTheme = "dark".equals(settingsService.getTheme()) || ("system".equals(settingsService.getTheme()) && settingsController.isSystemDark());
        }
        downloadManagerPanel = new DownloadManagerPanel(container, this, isDarkTheme);

        // Clear reference when closed
        downloadManagerPanel.setOnHidden(e -> downloadManagerPanel = null);

        downloadManagerPanel.show();

        if (shortcutManager != null && downloadManagerPanel.getScene() != null) {
            try {
                shortcutManager.setupForScene(downloadManagerPanel.getScene());
            } catch (Exception ignored) {}
        }
    }

    public void toggleDownloadDropdown(DownloadDropdown downloadDropdown, Button downloadsButton) {
        if (downloadDropdown != null && downloadsButton != null && downloadsButton.getScene() != null) {
            javafx.geometry.Bounds b = downloadsButton.localToScene(downloadsButton.getBoundsInLocal());
            javafx.stage.Window w = downloadsButton.getScene().getWindow();
            if (downloadDropdown.isShowing()) {
                downloadDropdown.hide();
            } else {
                downloadDropdown.showNear(w, b);
            }
        } else {
            showDownloadManagerPanel(null, null, null);
        }
    }

    public void updateDownloadsBadge(DIContainer container, Button downloadsButton) {
        try {
            DownloadService ds = (container != null) ? container.getOrCreate(DownloadService.class) : this.downloadService;
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
