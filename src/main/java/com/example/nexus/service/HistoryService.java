package com.example.nexus.service;

import com.example.nexus.core.DIContainer;
import com.example.nexus.exception.BrowserException;
import com.example.nexus.model.HistoryEntry;
import com.example.nexus.repository.HistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HistoryService {
    private static final Logger logger = LoggerFactory.getLogger(HistoryService.class);

    private final HistoryRepository historyRepository;

    public HistoryService(DIContainer container) {
        this.historyRepository = container.getOrCreate(HistoryRepository.class);
    }

    public List<HistoryEntry> getAllHistory() {
        try {
            return historyRepository.findAll();
        } catch (Exception e) {
            logger.error("Error retrieving all history", e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR,
                "Failed to retrieve history", e);
        }
    }


    public void addToHistory(String url, String title, String faviconUrl) {
        if (url == null || url.trim().isEmpty()) {
            throw new BrowserException(BrowserException.ErrorCode.INVALID_INPUT,
                "URL cannot be null or empty");
        }

        try {

            HistoryEntry existingEntry = historyRepository.findByUrl(url);

            if (existingEntry != null) {

                existingEntry.incrementVisitCount();
                if (title != null && !title.trim().isEmpty()) {
                    existingEntry.setTitle(title);
                }
                if (faviconUrl != null) {
                    existingEntry.setFaviconUrl(faviconUrl);
                }
                historyRepository.update(existingEntry);
                logger.info("Updated history entry for URL: " + url);
            } else {

                HistoryEntry entry = new HistoryEntry(title, url, faviconUrl);
                historyRepository.save(entry);
                logger.info("Added new history entry for URL: " + url);
            }
        } catch (BrowserException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error adding to history: " + url, e);
            throw new BrowserException(BrowserException.ErrorCode.HISTORY_SAVE_ERROR,
                "Failed to add to history", e);
        }
    }

    public void addToHistory(String url, String title) {
        addToHistory(url, title, null);
    }


    public void deleteHistoryEntry(int id) {
        try {
            historyRepository.delete(id);
            logger.info("Deleted history entry with ID: " + id);
        } catch (Exception e) {
            logger.error("Error deleting history entry with ID: " + id, e);
            throw new BrowserException(BrowserException.ErrorCode.HISTORY_DELETE_ERROR,
                "Failed to delete history entry", e);
        }
    }

    public void clearHistory() {
        try {
            historyRepository.clearAll();
            logger.info("Cleared all history");
        } catch (Exception e) {
            logger.error("Error clearing history", e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR,
                "Failed to clear history", e);
        }
    }

    public List<HistoryEntry> searchHistory(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllHistory();
        }

        try {
            return historyRepository.search(query.trim());
        } catch (Exception e) {
            logger.error("Error searching history with query: " + query, e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR,
                "Failed to search history", e);
        }
    }
}
