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


public class BookmarkService {
    private static final Logger logger = LoggerFactory.getLogger(BookmarkService.class);

    private final BookmarkRepository bookmarkRepository;
    private final BookmarkFolderRepository folderRepository;

    public BookmarkService(DIContainer container) {
        this.bookmarkRepository = container.getOrCreate(BookmarkRepository.class);
        this.folderRepository = container.getOrCreate(BookmarkFolderRepository.class);
    }

    public List<Bookmark> getAllBookmarks() {
        try {
            return bookmarkRepository.findAll();
        } catch (Exception e) {
            logger.error("Error retrieving all bookmarks", e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR,
                "Failed to retrieve bookmarks", e);
        }
    }


    public Optional<Bookmark> getBookmark(int id) {
        try {
            return Optional.ofNullable(bookmarkRepository.findById(id));
        } catch (Exception e) {
            logger.error("Error retrieving bookmark with ID: " + id, e);
            throw new BrowserException(BrowserException.ErrorCode.BOOKMARK_NOT_FOUND,
                "Failed to retrieve bookmark", e);
        }
    }


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
            Optional<Bookmark> existingBookmark = getBookmarkByUrl(bookmark.getUrl());
            if (existingBookmark.isPresent()) {

                Bookmark existing = existingBookmark.get();
                existing.setTitle(bookmark.getTitle());
                existing.setFolderId(bookmark.getFolderId());
                existing.setFavorite(bookmark.isFavorite());
                bookmarkRepository.update(existing);
                logger.info("Updated existing bookmark: " + existing.getTitle());
                return existing;
            } else {

                bookmarkRepository.save(bookmark);
                logger.info("Saved new bookmark: " + bookmark.getTitle());
                return bookmark;
            }
        } catch (Exception e) {
            logger.error("Error saving bookmark", e);
            throw new BrowserException(BrowserException.ErrorCode.BOOKMARK_SAVE_ERROR,
                "Failed to save bookmark", e);
        }
    }

    public void updateBookmark(Bookmark bookmark) {
        if (bookmark == null) {
            throw new BrowserException(BrowserException.ErrorCode.INVALID_INPUT,
                    "Bookmark cannot be null");
        }

        try {

            Optional<Bookmark> existingBookmark = getBookmark(bookmark.getId());
            if (existingBookmark.isPresent()) {
                bookmarkRepository.update(bookmark);
                logger.info("Updated bookmark: " + bookmark.getTitle());
            } else {
                throw new BrowserException(BrowserException.ErrorCode.BOOKMARK_NOT_FOUND,
                        "Bookmark not found with ID: " + bookmark.getId());
            }
        } catch (BrowserException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error updating bookmark", e);
            throw new BrowserException(BrowserException.ErrorCode.BOOKMARK_SAVE_ERROR,
                    "Failed to update bookmark", e);
        }
    }
    public void deleteBookmark(int id) {
        try {

            Optional<Bookmark> existingBookmark = getBookmark(id);
            if (existingBookmark.isPresent()) {
                bookmarkRepository.delete(id);
                logger.info("Deleted bookmark with ID: " + id);
            } else {
                throw new BrowserException(BrowserException.ErrorCode.BOOKMARK_NOT_FOUND,
                        "Bookmark not found with ID: " + id);
            }
        } catch (BrowserException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error deleting bookmark with ID: " + id, e);
            throw new BrowserException(BrowserException.ErrorCode.BOOKMARK_DELETE_ERROR,
                    "Failed to delete bookmark", e);
        }
    }

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

    public List<Bookmark> getRootBookmarks() {
        try {
            return bookmarkRepository.findRootBookmarks();
        } catch (Exception e) {
            logger.error("Error retrieving root bookmarks", e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR,
                "Failed to retrieve root bookmarks", e);
        }
    }


    public List<Bookmark> getFavorites() {
        try {
            return bookmarkRepository.findFavorites();
        } catch (Exception e) {
            logger.error("Error retrieving favorite bookmarks", e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR,
                "Failed to retrieve favorites", e);
        }
    }

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

    public List<BookmarkFolder> getAllFolders() {
        try {
            return folderRepository.findAll();
        } catch (Exception e) {
            logger.error("Error retrieving all folders", e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR,
                "Failed to retrieve folders", e);
        }
    }

    public Optional<BookmarkFolder> getFolder(int id) {
        try {
            return Optional.ofNullable(folderRepository.findById(id));
        } catch (Exception e) {
            logger.error("Error retrieving folder with ID: " + id, e);
            throw new BrowserException(BrowserException.ErrorCode.FOLDER_NOT_FOUND,
                "Failed to retrieve folder", e);
        }
    }

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

    public void deleteFolder(int folderId, boolean deleteContents) {
        try {
            if (deleteContents) {

                List<Bookmark> bookmarks = getBookmarksByFolderId(folderId);
                for (Bookmark bookmark : bookmarks) {
                    deleteBookmark(bookmark.getId());
                }

                List<BookmarkFolder> subFolders = getSubFolders(folderId);
                for (BookmarkFolder subFolder : subFolders) {
                    deleteFolder(subFolder.getId(), true);
                }
            } else {

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

    public List<BookmarkFolder> getSubFolders(Integer parentFolderId) {
        try {
            return folderRepository.findByParentId(parentFolderId);
        } catch (Exception e) {
            logger.error("Error retrieving subfolders for parent ID: " + parentFolderId, e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR,
                "Failed to retrieve subfolders", e);
        }
    }

    public List<BookmarkFolder> getRootFolders() {
        try {
            return folderRepository.findRootFolders();
        } catch (Exception e) {
            logger.error("Error retrieving root folders", e);
            throw new BrowserException(BrowserException.ErrorCode.DATABASE_ERROR,
                "Failed to retrieve root folders", e);
        }
    }

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
}
