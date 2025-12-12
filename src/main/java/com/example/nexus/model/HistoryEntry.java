package com.example.nexus.model;

import java.time.LocalDateTime;

public class HistoryEntry {
    private int id;
    private int userId;
    private String title;
    private String url;
    private String faviconUrl;
    private int visitCount;
    private LocalDateTime lastVisit;

    // Constructors, getters, and setters
    public HistoryEntry() {}

    public HistoryEntry(String title, String url) {
        this.title = title;
        this.url = url;
        this.visitCount = 1;
        this.lastVisit = LocalDateTime.now();
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getFaviconUrl() { return faviconUrl; }
    public void setFaviconUrl(String faviconUrl) { this.faviconUrl = faviconUrl; }

    public int getVisitCount() { return visitCount; }
    public void setVisitCount(int visitCount) { this.visitCount = visitCount; }

    public LocalDateTime getLastVisit() { return lastVisit; }
    public void setLastVisit(LocalDateTime lastVisit) { this.lastVisit = lastVisit; }
}
