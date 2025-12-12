package com.example.nexus.service;


import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Download;
import com.example.nexus.repository.DownloadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

public class DownloadService {
    private static final Logger logger = LoggerFactory.getLogger(DownloadService.class);

    private final DownloadRepository downloadRepository;

    public DownloadService(DIContainer container) {
        this.downloadRepository = container.getOrCreate(DownloadRepository.class);
    }

    public List<Download> getAllDownloads() {
        return downloadRepository.findAll();
    }

    public Download getDownload(int id) {
        return downloadRepository.findById(id);
    }

    public void startDownload(String url, String fileName, String filePath) {
        Download download = new Download(url, fileName, filePath);
        downloadRepository.save(download);

        // In a real implementation, this would start the actual download
        // and update the progress as it goes
        logger.info("Started download: {} to {}", url, filePath);
    }

    public void updateDownloadProgress(int id, long downloadedSize) {
        Download download = downloadRepository.findById(id);
        if (download != null) {
            download.setDownloadedSize(downloadedSize);
            downloadRepository.update(download);
        }
    }

    public void completeDownload(int id) {
        Download download = downloadRepository.findById(id);
        if (download != null) {
            download.setStatus("completed");
            download.setDownloadedSize(download.getFileSize());
            download.setEndTime(LocalDateTime.now());
            downloadRepository.update(download);
        }
    }

    public void failDownload(int id, String error) {
        Download download = downloadRepository.findById(id);
        if (download != null) {
            download.setStatus("failed");
            download.setEndTime(LocalDateTime.now());
            downloadRepository.update(download);
        }
    }

    public void cancelDownload(int id) {
        Download download = downloadRepository.findById(id);
        if (download != null) {
            download.setStatus("cancelled");
            download.setEndTime(LocalDateTime.now());
            downloadRepository.update(download);
        }
    }

    public void deleteDownload(int id) {
        downloadRepository.delete(id);
    }

    public void clearDownloads() {
        downloadRepository.clearAll();
    }
}
