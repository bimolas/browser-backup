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
        String sql = "INSERT INTO bookmarks (user_id, title, url, favicon_url, folder_id, position) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, 1); // Default user ID
            stmt.setString(2, bookmark.getTitle());
            stmt.setString(3, bookmark.getUrl());
            stmt.setString(4, bookmark.getFaviconUrl());

            if (bookmark.getFolderId() != null) {
                stmt.setInt(5, bookmark.getFolderId());
            } else {
                stmt.setNull(5, java.sql.Types.INTEGER);
            }

            stmt.setInt(6, bookmark.getPosition());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        bookmark.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving bookmark", e);
        }
    }

    @Override
    public void update(Bookmark bookmark) {
        String sql = "UPDATE bookmarks SET title = ?, url = ?, favicon_url = ?, folder_id = ?, " +
                "position = ? WHERE id = ?";

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
            stmt.setInt(6, bookmark.getId());

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
        return bookmark;
    }
}
