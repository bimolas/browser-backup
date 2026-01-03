package com.example.nexus.repository;

import com.example.nexus.model.Bookmark;
import com.example.nexus.util.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BookmarkRepository extends BaseRepository<Bookmark> {
    public BookmarkRepository(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override
    public List<Bookmark> findAll() {
        List<Bookmark> bookmarks = new ArrayList<>();
        String sql = "SELECT * FROM bookmarks ORDER BY position";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Bookmark bookmark = mapResultSetToBookmark(rs);
                bookmarks.add(bookmark);
            }
        } catch (SQLException e) {
            logger.error("Error finding all bookmarks", e);
        }

        return bookmarks;
    }

    @Override
    public Bookmark findById(int id) {
        String sql = "SELECT * FROM bookmarks WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBookmark(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding bookmark by ID: " + id, e);
        }

        return null;
    }

    public List<Bookmark> findByFolderId(int folderId) {
        List<Bookmark> bookmarks = new ArrayList<>();
        String sql = "SELECT * FROM bookmarks WHERE folder_id = ? ORDER BY position";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, folderId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Bookmark bookmark = mapResultSetToBookmark(rs);
                    bookmarks.add(bookmark);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding bookmarks by folder ID: " + folderId, e);
        }

        return bookmarks;
    }

    public List<Bookmark> search(String query) {
        List<Bookmark> bookmarks = new ArrayList<>();
        String sql = "SELECT * FROM bookmarks WHERE title LIKE ? OR url LIKE ? ORDER BY position";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            String searchPattern = "%" + query + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Bookmark bookmark = mapResultSetToBookmark(rs);
                    bookmarks.add(bookmark);
                }
            }
        } catch (SQLException e) {
            logger.error("Error searching bookmarks with query: " + query, e);
        }

        return bookmarks;
    }

    @Override
    public void save(Bookmark bookmark) {
        String sql = "INSERT INTO bookmarks (user_id, title, url, favicon_url, folder_id, position, is_favorite) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, 1);
            stmt.setString(2, bookmark.getTitle());
            stmt.setString(3, bookmark.getUrl());
            stmt.setString(4, bookmark.getFaviconUrl());

            if (bookmark.getFolderId() != null) {
                stmt.setInt(5, bookmark.getFolderId());
            } else {
                stmt.setNull(5, java.sql.Types.INTEGER);
            }

            stmt.setInt(6, bookmark.getPosition());
            stmt.setInt(7, bookmark.isFavorite() ? 1 : 0);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        bookmark.setId(generatedKeys.getInt(1));
                        logger.info("Bookmark saved with ID: {} - {}", bookmark.getId(), bookmark.getTitle());
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving bookmark: " + bookmark.getTitle(), e);
            throw new RuntimeException("Failed to save bookmark", e);
        }
    }

    @Override
    public void update(Bookmark bookmark) {
        String sql = """
            UPDATE bookmarks SET title = ?, url = ?, favicon_url = ?, folder_id = ?,
            position = ?, is_favorite = ?, description = ?, tags = ?, updated_at = ?
            WHERE id = ?
            """;

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, bookmark.getTitle());
            stmt.setString(2, bookmark.getUrl());
            stmt.setString(3, bookmark.getFaviconUrl());

            if (bookmark.getFolderId() != null) {
                stmt.setInt(4, bookmark.getFolderId());
            } else {
                stmt.setNull(4, java.sql.Types.INTEGER);
            }

            stmt.setInt(5, bookmark.getPosition());
            stmt.setInt(6, bookmark.isFavorite() ? 1 : 0);
            stmt.setString(7, bookmark.getDescription());
            stmt.setString(8, bookmark.getTags());
            stmt.setTimestamp(9, java.sql.Timestamp.valueOf(bookmark.getUpdatedAt()));
            stmt.setInt(10, bookmark.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating bookmark", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM bookmarks WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting bookmark", e);
        }
    }

    public Bookmark findByUrl(String url) {
        String sql = "SELECT * FROM bookmarks WHERE url = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, url);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBookmark(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding bookmark by URL: " + url, e);
        }

        return null;
    }

    public List<Bookmark> findFavorites() {
        List<Bookmark> bookmarks = new ArrayList<>();
        String sql = "SELECT * FROM bookmarks WHERE is_favorite = 1 ORDER BY position";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                bookmarks.add(mapResultSetToBookmark(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding favorite bookmarks", e);
        }

        return bookmarks;
    }

    public List<Bookmark> findRootBookmarks() {
        List<Bookmark> bookmarks = new ArrayList<>();
        String sql = "SELECT * FROM bookmarks WHERE folder_id IS NULL ORDER BY position";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                bookmarks.add(mapResultSetToBookmark(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding root bookmarks", e);
        }

        return bookmarks;
    }

    public boolean existsByUrl(String url) {
        String sql = "SELECT COUNT(*) FROM bookmarks WHERE url = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, url);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("Error checking bookmark existence by URL: " + url, e);
        }

        return false;
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM bookmarks";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting bookmarks", e);
        }

        return 0;
    }

    public void updatePosition(int id, int newPosition) {
        String sql = "UPDATE bookmarks SET position = ?, updated_at = ? WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, newPosition);
            stmt.setTimestamp(2, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            stmt.setInt(3, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating bookmark position", e);
        }
    }

    public void updateFavoriteStatus(int id, boolean isFavorite) {
        String sql = "UPDATE bookmarks SET is_favorite = ?, updated_at = ? WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, isFavorite ? 1 : 0);
            stmt.setTimestamp(2, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            stmt.setInt(3, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating bookmark favorite status", e);
        }
    }

    private Bookmark mapResultSetToBookmark(ResultSet rs) throws SQLException {
        Bookmark bookmark = new Bookmark();
        bookmark.setId(rs.getInt("id"));
        bookmark.setUserId(rs.getInt("user_id"));
        bookmark.setTitle(rs.getString("title"));
        bookmark.setUrl(rs.getString("url"));
        bookmark.setFaviconUrl(rs.getString("favicon_url"));

        int folderId = rs.getInt("folder_id");
        if (!rs.wasNull()) {
            bookmark.setFolderId(folderId);
        }

        bookmark.setPosition(rs.getInt("position"));

        try {
            bookmark.setFavorite(rs.getInt("is_favorite") == 1);
        } catch (SQLException ignored) {
            bookmark.setFavorite(false);
        }

        try {
            bookmark.setDescription(rs.getString("description"));
        } catch (SQLException ignored) {}

        try {
            bookmark.setTags(rs.getString("tags"));
        } catch (SQLException ignored) {}

        try {
            java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                bookmark.setCreatedAt(createdAt.toLocalDateTime());
            }
        } catch (SQLException ignored) {}

        try {
            java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                bookmark.setUpdatedAt(updatedAt.toLocalDateTime());
            }
        } catch (SQLException ignored) {}

        return bookmark;
    }
}
