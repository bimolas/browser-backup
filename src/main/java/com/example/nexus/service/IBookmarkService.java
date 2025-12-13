package com.example.nexus.service;

import com.example.nexus.model.Bookmark;
import com.example.nexus.model.BookmarkFolder;

import java.util.List;
import java.util.Optional;

/**
 * Interface for bookmark management operations.
 */
public interface IBookmarkService {

    // ==================== Bookmark Operations ====================

    /**
     * Get all bookmarks.
     */
    List<Bookmark> getAllBookmarks();

    /**
     * Get a bookmark by its ID.
     */
    Optional<Bookmark> getBookmark(int id);

    /**
     * Save a new bookmark.
     */
    Bookmark saveBookmark(Bookmark bookmark);

    /**
     * Save a bookmark with folder assignment.
     */
    Bookmark saveBookmark(String title, String url, String faviconUrl, Integer folderId);

    /**
     * Save a bookmark to favorites.
     */
    Bookmark saveToFavorites(String title, String url, String faviconUrl);

    /**
     * Update an existing bookmark.
     */
    void updateBookmark(Bookmark bookmark);

    /**
     * Delete a bookmark by ID.
     */
    void deleteBookmark(int id);

    /**
     * Get bookmarks in a specific folder.
     */
    List<Bookmark> getBookmarksByFolderId(Integer folderId);

    /**
     * Get bookmarks without a folder (root bookmarks).
     */
    List<Bookmark> getRootBookmarks();

    /**
     * Get favorite bookmarks.
     */
    List<Bookmark> getFavorites();

    /**
     * Search bookmarks by title or URL.
     */
    List<Bookmark> searchBookmarks(String query);

    /**
     * Check if a URL is bookmarked.
     */
    boolean isBookmarked(String url);

    /**
     * Get bookmark by URL.
     */
    Optional<Bookmark> getBookmarkByUrl(String url);

    /**
     * Move bookmark to a different folder.
     */
    void moveBookmark(int bookmarkId, Integer newFolderId);

    /**
     * Reorder bookmark within its folder.
     */
    void reorderBookmark(int bookmarkId, int newPosition);

    /**
     * Toggle favorite status of a bookmark.
     */
    void toggleFavorite(int bookmarkId);

    // ==================== Folder Operations ====================

    /**
     * Get all folders.
     */
    List<BookmarkFolder> getAllFolders();

    /**
     * Get a folder by its ID.
     */
    Optional<BookmarkFolder> getFolder(int id);

    /**
     * Create a new folder.
     */
    BookmarkFolder createFolder(String name, Integer parentFolderId);

    /**
     * Create a root folder.
     */
    BookmarkFolder createFolder(String name);

    /**
     * Update a folder.
     */
    void updateFolder(BookmarkFolder folder);

    /**
     * Delete a folder and optionally its contents.
     */
    void deleteFolder(int folderId, boolean deleteContents);

    /**
     * Get subfolders of a folder.
     */
    List<BookmarkFolder> getSubFolders(Integer parentFolderId);

    /**
     * Get root folders.
     */
    List<BookmarkFolder> getRootFolders();

    /**
     * Move folder to a different parent.
     */
    void moveFolder(int folderId, Integer newParentFolderId);

    /**
     * Get folder with all its contents.
     */
    BookmarkFolder getFolderWithContents(int folderId);

    /**
     * Get bookmarks count.
     */
    int getBookmarksCount();

    /**
     * Get folders count.
     */
    int getFoldersCount();
}
