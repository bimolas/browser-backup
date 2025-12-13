package com.example.nexus.service;

import com.example.nexus.core.DIContainer;
import com.example.nexus.exception.BrowserException;
import com.example.nexus.model.Bookmark;
import com.example.nexus.model.BookmarkFolder;
import com.example.nexus.repository.BookmarkFolderRepository;
import com.example.nexus.repository.BookmarkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class BookmarkService implements IBookmarkService {
    private static final Logger logger = LoggerFactory.getLogger(BookmarkService.class);

    private final BookmarkRepository bookmarkRepository;
    private final BookmarkFolderRepository folderRepository;

    public BookmarkService(DIContainer container) {
        this.bookmarkRepository = container.getOrCreate(BookmarkRepository.class);
        this.folderRepository = container.getOrCreate(BookmarkFolderRepository.class);
    }

    // ==================== Bookmark Operations ====================

    @Override
    public List<Bookmark> getAllBookmarks() {
        try {
            return bookmarkRepository.findAll();
        } catch (Exception e) {
            logger.error("Error retrieving all bookmarks", e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR,
                "Failed to retrieve bookmarks", e);
        }
    }

    @Override
    public Optional<Bookmark> getBookmark(int id) {
        try {
            return Optional.ofNullable(bookmarkRepository.findById(id));
        } catch (Exception e) {
            logger.error("Error retrieving bookmark with ID: " + id, e);
            throw new BrowserException(BrowserException.ErrorCode.BOOKMARK_NOT_FOUND,
                "Failed to retrieve bookmark", e);
        }
    }

    @Override
    public Bookmark saveBookmark(Bookmark bookmark) {
        if (bookmark == null) {
            throw new BrowserException(BrowserException.ErrorCode.INVALID_INPUT,
                "Bookmark cannot be null");
        }
        if (bookmark.getUrl() == null || bookmark.getUrl().trim().isEmpty()) {
            throw new BrowserException(BrowserException.ErrorCode.INVALID_INPUT,
                "Bookmark URL cannot be null or empty");
        }

        try {
            bookmarkRepository.save(bookmark);
            logger.info("Saved bookmark: " + bookmark.getTitle());
            return bookmark;
        } catch (Exception e) {
            logger.error("Error saving bookmark", e);
            throw new BrowserException(BrowserException.ErrorCode.BOOKMARK_SAVE_ERROR,
                "Failed to save bookmark", e);
        }
    }

    // Keep the old method signature for backward compatibility
    public void saveBookmark(Bookmark bookmark, boolean ignored) {
        saveBookmark(bookmark);
    }

    @Override
    public Bookmark saveBookmark(String title, String url, String faviconUrl, Integer folderId) {
        Bookmark bookmark = new Bookmark(title, url, faviconUrl, folderId);
        return saveBookmark(bookmark);
    }

    @Override
    public Bookmark saveToFavorites(String title, String url, String faviconUrl) {
        Bookmark bookmark = new Bookmark(title, url, faviconUrl);
        bookmark.setFavorite(true);
        return saveBookmark(bookmark);
    }

    @Override
    public void updateBookmark(Bookmark bookmark) {
        if (bookmark == null) {
            throw new BrowserException(BrowserException.ErrorCode.INVALID_INPUT,
                "Bookmark cannot be null");
        }

        try {
            bookmarkRepository.update(bookmark);
            logger.info("Updated bookmark: " + bookmark.getTitle());
        } catch (Exception e) {
            logger.error("Error updating bookmark", e);
            throw new BrowserException(BrowserException.ErrorCode.BOOKMARK_SAVE_ERROR,
                "Failed to update bookmark", e);
        }
    }

    @Override
    public void deleteBookmark(int id) {
        try {
            bookmarkRepository.delete(id);
            logger.info("Deleted bookmark with ID: " + id);
        } catch (Exception e) {
            logger.error("Error deleting bookmark with ID: " + id, e);
            throw new BrowserException(BrowserException.ErrorCode.BOOKMARK_DELETE_ERROR,
                "Failed to delete bookmark", e);
        }
    }

    @Override
    public List<Bookmark> getBookmarksByFolderId(Integer folderId) {
        try {
            if (folderId == null) {
                return getRootBookmarks();
            }
            return bookmarkRepository.findByFolderId(folderId);
        } catch (Exception e) {
            logger.error("Error retrieving bookmarks by folder ID: " + folderId, e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR,
                "Failed to retrieve bookmarks", e);
        }
    }

    @Override
    public List<Bookmark> getRootBookmarks() {
        try {
            return bookmarkRepository.findRootBookmarks();
        } catch (Exception e) {
            logger.error("Error retrieving root bookmarks", e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR,
                "Failed to retrieve root bookmarks", e);
        }
    }

    @Override
    public List<Bookmark> getFavorites() {
        try {
            return bookmarkRepository.findFavorites();
        } catch (Exception e) {
            logger.error("Error retrieving favorite bookmarks", e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR,
                "Failed to retrieve favorites", e);
        }
    }

    @Override
    public List<Bookmark> searchBookmarks(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllBookmarks();
        }

        try {
            return bookmarkRepository.search(query.trim());
        } catch (Exception e) {
            logger.error("Error searching bookmarks with query: " + query, e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR,
                "Failed to search bookmarks", e);
        }
    }

    @Override
    public boolean isBookmarked(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        try {
            return bookmarkRepository.existsByUrl(url);
        } catch (Exception e) {
            logger.error("Error checking if URL is bookmarked: " + url, e);
            return false;
        }
    }

    @Override
    public Optional<Bookmark> getBookmarkByUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(bookmarkRepository.findByUrl(url));
        } catch (Exception e) {
            logger.error("Error finding bookmark by URL: " + url, e);
            return Optional.empty();
        }
    }

    @Override
    public void moveBookmark(int bookmarkId, Integer newFolderId) {
        try {
            Optional<Bookmark> optBookmark = getBookmark(bookmarkId);
            if (optBookmark.isPresent()) {
                Bookmark bookmark = optBookmark.get();
                bookmark.setFolderId(newFolderId);
                updateBookmark(bookmark);
                logger.info("Moved bookmark " + bookmarkId + " to folder " + newFolderId);
            } else {
                throw new BrowserException(BrowserException.ErrorCode.BOOKMARK_NOT_FOUND,
                    "Bookmark not found with ID: " + bookmarkId);
            }
        } catch (BrowserException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error moving bookmark", e);
            throw new BrowserException(BrowserException.ErrorCode.BOOKMARK_SAVE_ERROR,
                "Failed to move bookmark", e);
        }
    }

    @Override
    public void reorderBookmark(int bookmarkId, int newPosition) {
        try {
            bookmarkRepository.updatePosition(bookmarkId, newPosition);
            logger.info("Reordered bookmark " + bookmarkId + " to position " + newPosition);
        } catch (Exception e) {
            logger.error("Error reordering bookmark", e);
            throw new BrowserException(BrowserException.ErrorCode.BOOKMARK_SAVE_ERROR,
                "Failed to reorder bookmark", e);
        }
    }

    @Override
    public void toggleFavorite(int bookmarkId) {
        try {
            Optional<Bookmark> optBookmark = getBookmark(bookmarkId);
            if (optBookmark.isPresent()) {
                Bookmark bookmark = optBookmark.get();
                bookmarkRepository.updateFavoriteStatus(bookmarkId, !bookmark.isFavorite());
                logger.info("Toggled favorite status for bookmark " + bookmarkId);
            } else {
                throw new BrowserException(BrowserException.ErrorCode.BOOKMARK_NOT_FOUND,
                    "Bookmark not found with ID: " + bookmarkId);
            }
        } catch (BrowserException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error toggling bookmark favorite status", e);
            throw new BrowserException(BrowserException.ErrorCode.BOOKMARK_SAVE_ERROR,
                "Failed to toggle favorite", e);
        }
    }

    // ==================== Folder Operations ====================

    @Override
    public List<BookmarkFolder> getAllFolders() {
        try {
            return folderRepository.findAll();
        } catch (Exception e) {
            logger.error("Error retrieving all folders", e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR,
                "Failed to retrieve folders", e);
        }
    }

    @Override
    public Optional<BookmarkFolder> getFolder(int id) {
        try {
            return Optional.ofNullable(folderRepository.findById(id));
        } catch (Exception e) {
            logger.error("Error retrieving folder with ID: " + id, e);
            throw new BrowserException(BrowserException.ErrorCode.FOLDER_NOT_FOUND,
                "Failed to retrieve folder", e);
        }
    }

    @Override
    public BookmarkFolder createFolder(String name, Integer parentFolderId) {
        if (name == null || name.trim().isEmpty()) {
            throw new BrowserException(BrowserException.ErrorCode.INVALID_INPUT,
                "Folder name cannot be null or empty");
        }

        try {
            BookmarkFolder folder = new BookmarkFolder(name.trim(), parentFolderId);
            folderRepository.save(folder);
            logger.info("Created folder: " + name);
            return folder;
        } catch (Exception e) {
            logger.error("Error creating folder", e);
            throw new BrowserException(BrowserException.ErrorCode.FOLDER_SAVE_ERROR,
                "Failed to create folder", e);
        }
    }

    @Override
    public BookmarkFolder createFolder(String name) {
        return createFolder(name, null);
    }

    @Override
    public void updateFolder(BookmarkFolder folder) {
        if (folder == null) {
            throw new BrowserException(BrowserException.ErrorCode.INVALID_INPUT,
                "Folder cannot be null");
        }

        try {
            folderRepository.update(folder);
            logger.info("Updated folder: " + folder.getName());
        } catch (Exception e) {
            logger.error("Error updating folder", e);
            throw new BrowserException(BrowserException.ErrorCode.FOLDER_SAVE_ERROR,
                "Failed to update folder", e);
        }
    }

    @Override
    public void deleteFolder(int folderId, boolean deleteContents) {
        try {
            if (deleteContents) {
                // Delete all bookmarks in the folder first
                List<Bookmark> bookmarks = getBookmarksByFolderId(folderId);
                for (Bookmark bookmark : bookmarks) {
                    deleteBookmark(bookmark.getId());
                }
                // Delete subfolders recursively
                List<BookmarkFolder> subFolders = getSubFolders(folderId);
                for (BookmarkFolder subFolder : subFolders) {
                    deleteFolder(subFolder.getId(), true);
                }
            } else {
                // Move contents to root
                List<Bookmark> bookmarks = getBookmarksByFolderId(folderId);
                for (Bookmark bookmark : bookmarks) {
                    moveBookmark(bookmark.getId(), null);
                }
                List<BookmarkFolder> subFolders = getSubFolders(folderId);
                for (BookmarkFolder subFolder : subFolders) {
                    moveFolder(subFolder.getId(), null);
                }
            }

            folderRepository.delete(folderId);
            logger.info("Deleted folder with ID: " + folderId);
        } catch (Exception e) {
            logger.error("Error deleting folder with ID: " + folderId, e);
            throw new BrowserException(BrowserException.ErrorCode.FOLDER_DELETE_ERROR,
                "Failed to delete folder", e);
        }
    }

    @Override
    public List<BookmarkFolder> getSubFolders(Integer parentFolderId) {
        try {
            return folderRepository.findByParentId(parentFolderId);
        } catch (Exception e) {
            logger.error("Error retrieving subfolders for parent ID: " + parentFolderId, e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR,
                "Failed to retrieve subfolders", e);
        }
    }

    @Override
    public List<BookmarkFolder> getRootFolders() {
        try {
            return folderRepository.findRootFolders();
        } catch (Exception e) {
            logger.error("Error retrieving root folders", e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR,
                "Failed to retrieve root folders", e);
        }
    }

    @Override
    public void moveFolder(int folderId, Integer newParentFolderId) {
        try {
            Optional<BookmarkFolder> optFolder = getFolder(folderId);
            if (optFolder.isPresent()) {
                BookmarkFolder folder = optFolder.get();
                folder.setParentFolderId(newParentFolderId);
                updateFolder(folder);
                logger.info("Moved folder " + folderId + " to parent " + newParentFolderId);
            } else {
                throw new BrowserException(BrowserException.ErrorCode.FOLDER_NOT_FOUND,
                    "Folder not found with ID: " + folderId);
            }
        } catch (BrowserException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error moving folder", e);
            throw new BrowserException(BrowserException.ErrorCode.FOLDER_SAVE_ERROR,
                "Failed to move folder", e);
        }
    }

    @Override
    public BookmarkFolder getFolderWithContents(int folderId) {
        try {
            Optional<BookmarkFolder> optFolder = getFolder(folderId);
            if (optFolder.isPresent()) {
                BookmarkFolder folder = optFolder.get();
                folder.setBookmarks(getBookmarksByFolderId(folderId));
                folder.setSubFolders(getSubFolders(folderId));
                return folder;
            }
            return null;
        } catch (Exception e) {
            logger.error("Error retrieving folder with contents for ID: " + folderId, e);
            throw new BrowserException(BrowserException.ErrorCode.FOLDER_NOT_FOUND,
                "Failed to retrieve folder with contents", e);
        }
    }

    @Override
    public int getBookmarksCount() {
        try {
            return bookmarkRepository.count();
        } catch (Exception e) {
            logger.error("Error counting bookmarks", e);
            return 0;
        }
    }

    @Override
    public int getFoldersCount() {
        try {
            return folderRepository.count();
        } catch (Exception e) {
            logger.error("Error counting folders", e);
            return 0;
        }
    }
}
