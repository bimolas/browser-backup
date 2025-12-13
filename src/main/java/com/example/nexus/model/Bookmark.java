package com.example.nexus.model;

import java.time.LocalDateTime;

public class Bookmark {
    private int id;
    private int userId;
    private String title;
    private String url;
    private String faviconUrl;
    private Integer folderId;
    private int position;
    private boolean isFavorite;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String description;
    private String tags;

    // Constructors, getters, and setters
    public Bookmark() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isFavorite = false;
    }

    public Bookmark(String title, String url) {
        this();
        this.title = title;
        this.url = url;
        this.position = 0;
    }

    public Bookmark(String title, String url, String faviconUrl) {
        this(title, url);
        this.faviconUrl = faviconUrl;
    }

    public Bookmark(String title, String url, String faviconUrl, Integer folderId) {
        this(title, url, faviconUrl);
        this.folderId = folderId;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { 
        this.title = title;
        this.updatedAt = LocalDateTime.now();
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { 
        this.url = url;
        this.updatedAt = LocalDateTime.now();
    }

    public String getFaviconUrl() { return faviconUrl; }
    public void setFaviconUrl(String faviconUrl) { 
        this.faviconUrl = faviconUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getFolderId() { return folderId; }
    public void setFolderId(Integer folderId) { 
        this.folderId = folderId;
        this.updatedAt = LocalDateTime.now();
    }

    public int getPosition() { return position; }
    public void setPosition(int position) { 
        this.position = position;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { 
        this.isFavorite = favorite;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { 
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public String getTags() { return tags; }
    public void setTags(String tags) { 
        this.tags = tags;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Get the domain from the URL.
     */
    public String getDomain() {
        if (url == null || url.isEmpty()) return "";
        try {
            String domain = url.replaceFirst("^(https?://)?", "")
                               .replaceFirst("/.*$", "")
                               .replaceFirst("^www\\.", "");
            return domain;
        } catch (Exception e) {
            return url;
        }
    }

    @Override
    public String toString() {
        return title != null ? title : url;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Bookmark bookmark = (Bookmark) obj;
        return id == bookmark.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}