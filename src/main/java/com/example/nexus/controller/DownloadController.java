package com.example.nexus.controller;

import com.example.nexus.model.Download;
import com.example.nexus.service.DownloadService;
import com.example.nexus.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * Controller for download-related operations.
 * Handles download management, progress tracking, and file operations.
 */
public class DownloadController {
    private static final Logger logger = LoggerFactory.getLogger(DownloadController.class);

    private final DownloadService downloadService;
    private final SettingsService settingsService;

    public DownloadController(DownloadService downloadService, SettingsService settingsService) {
        this.downloadService = downloadService;
        this.settingsService = settingsService;
    }

    /**
     * Start a new download
     */
    public void startDownload(String url, String filename) {
        try {
            String downloadPath = settingsService.getDownloadPath();
            File targetFile = new File(downloadPath, filename);

            downloadService.startDownload(url, filename, targetFile.getAbsolutePath());
            logger.info("Started download: {} -> {}", url, targetFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Error starting download: {}", url, e);
        }
    }

    /**
     * Cancel a download
     */
    public void cancelDownload(int downloadId) {
        try {
            downloadService.cancelDownload(downloadId);
            logger.info("Cancelled download: {}", downloadId);
        } catch (Exception e) {
            logger.error("Error cancelling download: {}", downloadId, e);
        }
    }

    /**
     * Get all downloads
     */
    public List<Download> getAllDownloads() {
        return downloadService.getAllDownloads();
    }

    /**
     * Delete a download record
     */
    public void deleteDownload(int downloadId) {
        try {
            downloadService.deleteDownload(downloadId);
            logger.info("Deleted download: {}", downloadId);
        } catch (Exception e) {
            logger.error("Error deleting download: {}", downloadId, e);
        }
    }

    /**
     * Clear all downloads
     */
    public void clearAllDownloads() {
        try {
            downloadService.clearDownloads();
            logger.info("Cleared all downloads");
        } catch (Exception e) {
            logger.error("Error clearing downloads", e);
        }
    }

    /**
     * Open download location in file manager
     */
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

    /**
     * Get download path setting
     */
    public String getDownloadPath() {
        return settingsService.getDownloadPath();
    }

    /**
     * Check if should ask for download location
     */
    public boolean shouldAskDownloadLocation() {
        return settingsService.isAskDownloadLocation();
    }
}
