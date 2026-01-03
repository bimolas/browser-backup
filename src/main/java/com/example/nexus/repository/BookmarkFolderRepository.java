package com.example.nexus.repository;

import com.example.nexus.exception.BrowserException;
import com.example.nexus.model.BookmarkFolder;
import com.example.nexus.util.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class BookmarkFolderRepository extends BaseRepository<BookmarkFolder> {

    public BookmarkFolderRepository(DatabaseManager dbManager) {
        super(dbManager);
        initializeTable();
    }

    private void initializeTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS bookmark_folders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER DEFAULT 1,
                name TEXT NOT NULL,
                parent_folder_id INTEGER,
                position INTEGER DEFAULT 0,
                is_favorite INTEGER DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (parent_folder_id) REFERENCES bookmark_folders(id) ON DELETE CASCADE
            )
            """;

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.executeUpdate();
            logger.info("Bookmark folders table initialized");
        } catch (SQLException e) {
            logger.error("Error initializing bookmark_folders table", e);
        }
    }

    @Override
    public List<BookmarkFolder> findAll() {
        List<BookmarkFolder> folders = new ArrayList<>();
        String sql = "SELECT * FROM bookmark_folders ORDER BY position";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                folders.add(mapResultSetToFolder(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all bookmark folders", e);
        }

        return folders;
    }

    @Override
    public BookmarkFolder findById(int id) {
        String sql = "SELECT * FROM bookmark_folders WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFolder(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding bookmark folder by ID: " + id, e);
        }

        return null;
    }

    public List<BookmarkFolder> findByParentId(Integer parentId) {
        List<BookmarkFolder> folders = new ArrayList<>();
        String sql = parentId == null
            ? "SELECT * FROM bookmark_folders WHERE parent_folder_id IS NULL ORDER BY position"
            : "SELECT * FROM bookmark_folders WHERE parent_folder_id = ? ORDER BY position";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            if (parentId != null) {
                stmt.setInt(1, parentId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    folders.add(mapResultSetToFolder(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding bookmark folders by parent ID: " + parentId, e);
        }

        return folders;
    }

    public List<BookmarkFolder> findRootFolders() {
        return findByParentId(null);
    }

    @Override
    public void save(BookmarkFolder folder) {
        String sql = """
            INSERT INTO bookmark_folders (user_id, name, parent_folder_id, position, is_favorite, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = getConnection().prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, folder.getUserId() > 0 ? folder.getUserId() : 1);
            stmt.setString(2, folder.getName());

            if (folder.getParentFolderId() != null) {
                stmt.setInt(3, folder.getParentFolderId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }

            stmt.setInt(4, folder.getPosition());
            stmt.setInt(5, folder.isFavorite() ? 1 : 0);
            stmt.setTimestamp(6, Timestamp.valueOf(folder.getCreatedAt()));
            stmt.setTimestamp(7, Timestamp.valueOf(folder.getUpdatedAt()));

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        folder.setId(generatedKeys.getInt(1));
                    }
                }
            }

            logger.info("Saved bookmark folder: " + folder.getName());
        } catch (SQLException e) {
            logger.error("Error saving bookmark folder", e);
        }
    }

    @Override
    public void update(BookmarkFolder folder) {
        String sql = """
            UPDATE bookmark_folders
            SET name = ?, parent_folder_id = ?, position = ?, is_favorite = ?, updated_at = ?
            WHERE id = ?
            """;

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, folder.getName());

            if (folder.getParentFolderId() != null) {
                stmt.setInt(2, folder.getParentFolderId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }

            stmt.setInt(3, folder.getPosition());
            stmt.setInt(4, folder.isFavorite() ? 1 : 0);
            stmt.setTimestamp(5, Timestamp.valueOf(folder.getUpdatedAt()));
            stmt.setInt(6, folder.getId());

            stmt.executeUpdate();
            logger.info("Updated bookmark folder: " + folder.getName());
        } catch (SQLException e) {
            logger.error("Error updating bookmark folder", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM bookmark_folders WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            logger.info("Deleted bookmark folder with ID: " + id);
        } catch (SQLException e) {
            logger.error("Error deleting bookmark folder", e);
        }
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM bookmark_folders";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting bookmark folders", e);
        }

        return 0;
    }

    private BookmarkFolder mapResultSetToFolder(ResultSet rs) throws SQLException {
        BookmarkFolder folder = new BookmarkFolder();
        folder.setId(rs.getInt("id"));
        folder.setUserId(rs.getInt("user_id"));
        folder.setName(rs.getString("name"));

        int parentId = rs.getInt("parent_folder_id");
        folder.setParentFolderId(rs.wasNull() ? null : parentId);

        folder.setPosition(rs.getInt("position"));
        folder.setFavorite(rs.getInt("is_favorite") == 1);

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            folder.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            folder.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return folder;
    }
}
