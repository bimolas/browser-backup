package com.example.nexus.service;

import com.example.nexus.core.DIContainer;
import com.example.nexus.exception.BrowserException;
import com.example.nexus.model.HistoryEntry;
import com.example.nexus.repository.HistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public class HistoryService implements IHistoryService {
    private static final Logger logger = LoggerFactory.getLogger(HistoryService.class);

    private final HistoryRepository historyRepository;

    public HistoryService(DIContainer container) {
        this.historyRepository = container.getOrCreate(HistoryRepository.class);
    }

    @Override
    public List<HistoryEntry> getAllHistory() {
        try {
            return historyRepository.findAll();
        } catch (Exception e) {
            logger.error("Error retrieving all history", e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR, 
                "Failed to retrieve history", e);
        }
    }

    @Override
    public Optional<HistoryEntry> getHistoryEntry(int id) {
        try {
            return Optional.ofNullable(historyRepository.findById(id));
        } catch (Exception e) {
            logger.error("Error retrieving history entry with ID: " + id, e);
            throw new BrowserException(BrowserException.ErrorCode.HISTORY_NOT_FOUND, 
                "Failed to retrieve history entry", e);
        }
    }

    @Override
    public HistoryEntry addToHistory(String url, String title, String faviconUrl) {
        if (url == null || url.trim().isEmpty()) {
            throw new BrowserException(BrowserException.ErrorCode.INVALID_INPUT, 
                "URL cannot be null or empty");
        }

        try {
            // Check if the URL already exists in history
            HistoryEntry existingEntry = historyRepository.findByUrl(url);

            if (existingEntry != null) {
                // Update the existing entry
                existingEntry.incrementVisitCount();
                if (title != null && !title.trim().isEmpty()) {
                    existingEntry.setTitle(title);
                }
                if (faviconUrl != null) {
                    existingEntry.setFaviconUrl(faviconUrl);
                }
                historyRepository.update(existingEntry);
                logger.info("Updated history entry for URL: " + url);
                return existingEntry;
            } else {
                // Create a new entry
                HistoryEntry entry = new HistoryEntry(title, url, faviconUrl);
                historyRepository.save(entry);
                logger.info("Added new history entry for URL: " + url);
                return entry;
            }
        } catch (BrowserException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error adding to history: " + url, e);
            throw new BrowserException(BrowserException.ErrorCode.HISTORY_SAVE_ERROR, 
                "Failed to add to history", e);
        }
    }

    @Override
    public HistoryEntry addToHistory(String url, String title) {
        return addToHistory(url, title, null);
    }

    // Keep the old method signature for backward compatibility
    public void addToHistory(String url, String title, boolean ignored) {
        addToHistory(url, title);
    }

    @Override
    public void updateHistoryEntry(HistoryEntry entry) {
        if (entry == null) {
            throw new BrowserException(BrowserException.ErrorCode.INVALID_INPUT, 
                "History entry cannot be null");
        }
        
        try {
            historyRepository.update(entry);
            logger.info("Updated history entry: " + entry.getUrl());
        } catch (Exception e) {
            logger.error("Error updating history entry", e);
            throw new BrowserException(BrowserException.ErrorCode.HISTORY_SAVE_ERROR, 
                "Failed to update history entry", e);
        }
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public List<HistoryEntry> getHistoryByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new BrowserException(BrowserException.ErrorCode.INVALID_INPUT, 
                "Start date and end date cannot be null");
        }
        
        try {
            return historyRepository.findByDateRange(startDate, endDate);
        } catch (Exception e) {
            logger.error("Error retrieving history by date range", e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR, 
                "Failed to retrieve history by date range", e);
        }
    }

    @Override
    public List<HistoryEntry> getTodayHistory() {
        LocalDate today = LocalDate.now();
        return getHistoryByDateRange(
            today.atStartOfDay(),
            today.atTime(LocalTime.MAX)
        );
    }

    @Override
    public List<HistoryEntry> getYesterdayHistory() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return getHistoryByDateRange(
            yesterday.atStartOfDay(),
            yesterday.atTime(LocalTime.MAX)
        );
    }

    @Override
    public List<HistoryEntry> getThisWeekHistory() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        return getHistoryByDateRange(
            weekStart.atStartOfDay(),
            today.atTime(LocalTime.MAX)
        );
    }

    @Override
    public List<HistoryEntry> getThisMonthHistory() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        return getHistoryByDateRange(
            monthStart.atStartOfDay(),
            today.atTime(LocalTime.MAX)
        );
    }

    @Override
    public List<HistoryEntry> getMostVisited(int limit) {
        if (limit <= 0) {
            limit = 10;
        }
        
        try {
            return historyRepository.findMostVisited(limit);
        } catch (Exception e) {
            logger.error("Error retrieving most visited history entries", e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR, 
                "Failed to retrieve most visited sites", e);
        }
    }

    @Override
    public boolean existsInHistory(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        try {
            return historyRepository.existsByUrl(url);
        } catch (Exception e) {
            logger.error("Error checking history existence for URL: " + url, e);
            return false;
        }
    }

    @Override
    public int getHistoryCount() {
        try {
            return historyRepository.count();
        } catch (Exception e) {
            logger.error("Error counting history entries", e);
            return 0;
        }
    }
}