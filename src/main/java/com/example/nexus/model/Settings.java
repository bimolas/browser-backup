package com.example.nexus.model;


public class Settings {
    private int id;
    private int userId;
    private String theme;
    private String accentColor;
    private String searchEngine;
    private String homePage;
    private String startupBehavior;
    private boolean restoreSession;
    private boolean clearHistoryOnExit;

    // Constructors, getters, and setters
    public Settings() {}

    public Settings(int userId) {
        this.userId = userId;
        this.theme = "light";
        this.accentColor = "#2196f3";
        this.searchEngine = "google";
        this.homePage = "https://www.google.com";
        this.startupBehavior = "show_home";
        this.restoreSession = true;
        this.clearHistoryOnExit = false;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getAccentColor() { return accentColor; }
    public void setAccentColor(String accentColor) { this.accentColor = accentColor; }

    public String getSearchEngine() { return searchEngine; }
    public void setSearchEngine(String searchEngine) { this.searchEngine = searchEngine; }

    public String getHomePage() { return homePage; }
    public void setHomePage(String homePage) { this.homePage = homePage; }

    public String getStartupBehavior() { return startupBehavior; }
    public void setStartupBehavior(String startupBehavior) { this.startupBehavior = startupBehavior; }

    public boolean isRestoreSession() { return restoreSession; }
    public void setRestoreSession(boolean restoreSession) { this.restoreSession = restoreSession; }

    public boolean isClearHistoryOnExit() { return clearHistoryOnExit; }
    public void setClearHistoryOnExit(boolean clearHistoryOnExit) { this.clearHistoryOnExit = clearHistoryOnExit; }
}
