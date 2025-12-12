package com.example.nexus.model;

public class Bookmark {
    private int id;
    private int userId;
    private String title;
    private String url;
    private String faviconUrl;
    private Integer folderId;
    private int position;

    // Constructors, getters, and setters
    public Bookmark() {}

    public Bookmark(String title, String url) {
        this.title = title;
        this.url = url;
        this.position = 0;
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

    public Integer getFolderId() { return folderId; }
    public void setFolderId(Integer folderId) { this.folderId = folderId; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}