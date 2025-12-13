package com.example.nexus.service;

import com.example.nexus.model.HistoryEntry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Interface for history management operations.
 */
public interface IHistoryService {

    /**
     * Get all history entries sorted by last visit date descending.
     */
    List<HistoryEntry> getAllHistory();

    /**
     * Get a history entry by its ID.
     */
    Optional<HistoryEntry> getHistoryEntry(int id);

    /**
     * Add a URL to history or update if it exists.
     */
    HistoryEntry addToHistory(String url, String title, String faviconUrl);

    /**
     * Add a URL to history with default favicon.
     */
    HistoryEntry addToHistory(String url, String title);

    /**
     * Update an existing history entry.
     */
    void updateHistoryEntry(HistoryEntry entry);

    /**
     * Delete a history entry by ID.
     */
    void deleteHistoryEntry(int id);

    /**
     * Clear all history entries.
     */
    void clearHistory();

    /**
     * Search history by title or URL.
     */
    List<HistoryEntry> searchHistory(String query);

    /**
     * Get history entries within a date range.
     */
    List<HistoryEntry> getHistoryByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get today's history.
     */
    List<HistoryEntry> getTodayHistory();

    /**
     * Get yesterday's history.
     */
    List<HistoryEntry> getYesterdayHistory();

    /**
     * Get this week's history.
     */
    List<HistoryEntry> getThisWeekHistory();

    /**
     * Get this month's history.
     */
    List<HistoryEntry> getThisMonthHistory();

    /**
     * Get most visited sites.
     */
    List<HistoryEntry> getMostVisited(int limit);

    /**
     * Check if a URL exists in history.
     */
    boolean existsInHistory(String url);

    /**
     * Get history count.
     */
    int getHistoryCount();
}
