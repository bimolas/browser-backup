package com.example.nexus.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BookmarkFolder {
    private int id;
    private int userId;
    private String name;
    private Integer parentFolderId;
    private int position;
    private boolean isFavorite;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Bookmark> bookmarks;
    private List<BookmarkFolder> subFolders;

    public BookmarkFolder() {
        this.bookmarks = new ArrayList<>();
        this.subFolders = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public BookmarkFolder(String name) {
        this();
        this.name = name;
    }

    public BookmarkFolder(String name, Integer parentFolderId) {
        this(name);
        this.parentFolderId = parentFolderId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getParentFolderId() { return parentFolderId; }
    public void setParentFolderId(Integer parentFolderId) {
        this.parentFolderId = parentFolderId;
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

    public List<Bookmark> getBookmarks() { return bookmarks; }
    public void setBookmarks(List<Bookmark> bookmarks) { this.bookmarks = bookmarks; }

    public List<BookmarkFolder> getSubFolders() { return subFolders; }
    public void setSubFolders(List<BookmarkFolder> subFolders) { this.subFolders = subFolders; }

    public void addBookmark(Bookmark bookmark) {
        this.bookmarks.add(bookmark);
        bookmark.setFolderId(this.id);
    }

    public void removeBookmark(Bookmark bookmark) {
        this.bookmarks.remove(bookmark);
    }

    public void addSubFolder(BookmarkFolder folder) {
        this.subFolders.add(folder);
        folder.setParentFolderId(this.id);
    }

    public void removeSubFolder(BookmarkFolder folder) {
        this.subFolders.remove(folder);
    }

    @Override
    public String toString() {
        return name;
    }
}
