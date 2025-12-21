package com.example.nexus.controller;

import com.example.nexus.model.HistoryEntry;
import com.example.nexus.service.HistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;


public class HistoryController {
    private static final Logger logger = LoggerFactory.getLogger(HistoryController.class);

    private final HistoryService historyService;
    private Consumer<String> onOpenUrl;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }


    public void setOnOpenUrl(Consumer<String> callback) {
        this.onOpenUrl = callback;
    }


    public void recordVisit(String url, String title) {
        if (url == null || url.isEmpty()) {
            return;
        }

        try {
            historyService.addToHistory(url, title);
            logger.debug("Recorded history visit: {}", url);
        } catch (Exception e) {
            logger.error("Error recording history visit", e);
        }
    }


    public List<HistoryEntry> getAllHistory() {
        return historyService.getAllHistory();
    }

    public List<HistoryEntry> getTodayHistory() {
        return historyService.getTodayHistory();
    }

    public List<HistoryEntry> getWeekHistory() {
        return historyService.getThisWeekHistory();
    }


    public List<HistoryEntry> getMonthHistory() {
        return historyService.getThisMonthHistory();
    }


    public List<HistoryEntry> searchHistory(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllHistory();
        }
        return historyService.searchHistory(query.trim());
    }


    public void deleteEntry(int id) {
        try {
            historyService.deleteHistoryEntry(id);
            logger.info("Deleted history entry: {}", id);
        } catch (Exception e) {
            logger.error("Error deleting history entry", e);
        }
    }


    public void clearAllHistory() {
        try {
            historyService.clearHistory();
            logger.info("Cleared all history");
        } catch (Exception e) {
            logger.error("Error clearing history", e);
        }
    }


    public void openHistoryEntry(HistoryEntry entry) {
        if (entry != null && onOpenUrl != null) {
            onOpenUrl.accept(entry.getUrl());
        }
    }
}
