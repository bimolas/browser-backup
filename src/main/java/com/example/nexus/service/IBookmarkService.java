package com.example.nexus.service;

import com.example.nexus.model.Bookmark;
import com.example.nexus.model.BookmarkFolder;

import java.util.List;
import java.util.Optional;

public interface IBookmarkService {

    List<Bookmark> getAllBookmarks();

    Optional<Bookmark> getBookmark(int id);

    Bookmark saveBookmark(Bookmark bookmark);

    Bookmark saveBookmark(String title, String url, String faviconUrl, Integer folderId);

    Bookmark saveToFavorites(String title, String url, String faviconUrl);

    void updateBookmark(Bookmark bookmark);

    void deleteBookmark(int id);

    List<Bookmark> getBookmarksByFolderId(Integer folderId);

    List<Bookmark> getRootBookmarks();

    List<Bookmark> getFavorites();

    List<Bookmark> searchBookmarks(String query);

    boolean isBookmarked(String url);

    Optional<Bookmark> getBookmarkByUrl(String url);

    void moveBookmark(int bookmarkId, Integer newFolderId);

    void reorderBookmark(int bookmarkId, int newPosition);

    void toggleFavorite(int bookmarkId);

    List<BookmarkFolder> getAllFolders();

    Optional<BookmarkFolder> getFolder(int id);

    BookmarkFolder createFolder(String name, Integer parentFolderId);

    BookmarkFolder createFolder(String name);

    void updateFolder(BookmarkFolder folder);

    void deleteFolder(int folderId, boolean deleteContents);

    List<BookmarkFolder> getSubFolders(Integer parentFolderId);

    List<BookmarkFolder> getRootFolders();

    void moveFolder(int folderId, Integer newParentFolderId);

    BookmarkFolder getFolderWithContents(int folderId);

    int getBookmarksCount();

    int getFoldersCount();
}
