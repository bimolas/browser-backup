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


    public HistoryEntry addToHistory(String url, String title, String faviconUrl) {
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
                return existingEntry;
            } else {

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

    public HistoryEntry addToHistory(String url, String title) {
        return addToHistory(url, title, null);
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

    public List<HistoryEntry> getTodayHistory() {
        LocalDate today = LocalDate.now();
        return getHistoryByDateRange(
            today.atStartOfDay(),
            today.atTime(LocalTime.MAX)
        );
    }


    public List<HistoryEntry> getThisWeekHistory() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        return getHistoryByDateRange(
            weekStart.atStartOfDay(),
            today.atTime(LocalTime.MAX)
        );
    }

    public List<HistoryEntry> getThisMonthHistory() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        return getHistoryByDateRange(
            monthStart.atStartOfDay(),
            today.atTime(LocalTime.MAX)
        );
    }
}
