package com.example.nexus.service;

import com.example.nexus.model.HistoryEntry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IHistoryService {

    List<HistoryEntry> getAllHistory();

    Optional<HistoryEntry> getHistoryEntry(int id);

    HistoryEntry addToHistory(String url, String title, String faviconUrl);

    HistoryEntry addToHistory(String url, String title);

    void updateHistoryEntry(HistoryEntry entry);

    void deleteHistoryEntry(int id);

    void clearHistory();

    List<HistoryEntry> searchHistory(String query);

    List<HistoryEntry> getHistoryByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    List<HistoryEntry> getTodayHistory();

    List<HistoryEntry> getYesterdayHistory();

    List<HistoryEntry> getThisWeekHistory();

    List<HistoryEntry> getThisMonthHistory();

    List<HistoryEntry> getMostVisited(int limit);

    boolean existsInHistory(String url);

    int getHistoryCount();
}
