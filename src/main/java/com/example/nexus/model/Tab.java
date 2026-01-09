package com.example.nexus.model;

public class Tab {
    private int id;
    private int profileId;
    private String title;
    private String url;
    private String faviconUrl;
    private boolean isPinned;
    private boolean isActive;
    private int position;
    private String sessionId;

    public Tab() {}

    public Tab(String url) {
        this.url = url;
        this.title = "New Tab";
        this.isPinned = false;
        this.isActive = false;
        this.position = 0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProfileId() { return profileId; }
    public void setProfileId(int profileId) { this.profileId = profileId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getFaviconUrl() { return faviconUrl; }
    public void setFaviconUrl(String faviconUrl) { this.faviconUrl = faviconUrl; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}
