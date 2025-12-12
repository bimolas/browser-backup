package com.example.nexus.service;


import com.example.nexus.core.DIContainer;
import com.example.nexus.model.HistoryEntry;
import com.example.nexus.repository.HistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

public class HistoryService {
    private static final Logger logger = LoggerFactory.getLogger(HistoryService.class);

    private final HistoryRepository historyRepository;

    public HistoryService(DIContainer container) {
        this.historyRepository = container.getOrCreate(HistoryRepository.class);
    }

    public List<HistoryEntry> getAllHistory() {
        return historyRepository.findAll();
    }

    public HistoryEntry getHistoryEntry(int id) {
        return historyRepository.findById(id);
    }

    public void addToHistory(String url, String title) {
        // Check if the URL already exists in history
        HistoryEntry existingEntry = historyRepository.findByUrl(url);

        if (existingEntry != null) {
            // Update the existing entry
            existingEntry.setVisitCount(existingEntry.getVisitCount() + 1);
            existingEntry.setLastVisit(LocalDateTime.now());
            historyRepository.update(existingEntry);
        } else {
            // Create a new entry
            HistoryEntry entry = new HistoryEntry(title, url);
            historyRepository.save(entry);
        }
    }

    public void updateHistoryEntry(HistoryEntry entry) {
        historyRepository.update(entry);
    }

    public void deleteHistoryEntry(int id) {
        historyRepository.delete(id);
    }

    public void clearHistory() {
        historyRepository.clearAll();
    }

    public List<HistoryEntry> searchHistory(String query) {
        return historyRepository.search(query);
    }

    public List<HistoryEntry> getHistoryByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return historyRepository.findByDateRange(startDate, endDate);
    }
}