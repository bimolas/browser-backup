package com.example.nexus.service;


import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Bookmark;
import com.example.nexus.repository.BookmarkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BookmarkService {
    private static final Logger logger = LoggerFactory.getLogger(BookmarkService.class);

    private final BookmarkRepository bookmarkRepository;

    public BookmarkService(DIContainer container) {
        this.bookmarkRepository = container.getOrCreate(BookmarkRepository.class);
    }

    public List<Bookmark> getAllBookmarks() {
        return bookmarkRepository.findAll();
    }

    public Bookmark getBookmark(int id) {
        return bookmarkRepository.findById(id);
    }

    public void saveBookmark(Bookmark bookmark) {
        bookmarkRepository.save(bookmark);
    }

    public void updateBookmark(Bookmark bookmark) {
        bookmarkRepository.update(bookmark);
    }

    public void deleteBookmark(int id) {
        bookmarkRepository.delete(id);
    }

    public List<Bookmark> getBookmarksByFolderId(int folderId) {
        return bookmarkRepository.findByFolderId(folderId);
    }

    public List<Bookmark> searchBookmarks(String query) {
        return bookmarkRepository.search(query);
    }
}
