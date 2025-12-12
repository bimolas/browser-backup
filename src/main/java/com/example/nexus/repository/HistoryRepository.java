package com.example.nexus.repository;


import com.example.nexus.model.HistoryEntry;
import com.example.nexus.util.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HistoryRepository extends BaseRepository<HistoryEntry> {
    public HistoryRepository(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override
    public List<HistoryEntry> findAll() {
        List<HistoryEntry> history = new ArrayList<>();
        String sql = "SELECT * FROM history ORDER BY last_visit DESC";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                HistoryEntry entry = mapResultSetToHistoryEntry(rs);
                history.add(entry);
            }
        } catch (SQLException e) {
            logger.error("Error finding all history entries", e);
        }

        return history;
    }

    @Override
    public HistoryEntry findById(int id) {
        String sql = "SELECT * FROM history WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToHistoryEntry(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding history entry by ID: " + id, e);
        }

        return null;
    }

    public HistoryEntry findByUrl(String url) {
        String sql = "SELECT * FROM history WHERE url = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, url);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToHistoryEntry(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding history entry by URL: " + url, e);
        }

        return null;
    }

    public List<HistoryEntry> search(String query) {
        List<HistoryEntry> history = new ArrayList<>();
        String sql = "SELECT * FROM history WHERE title LIKE ? OR url LIKE ? ORDER BY last_visit DESC";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            String searchPattern = "%" + query + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    HistoryEntry entry = mapResultSetToHistoryEntry(rs);
                    history.add(entry);
                }
            }
        } catch (SQLException e) {
            logger.error("Error searching history with query: " + query, e);
        }

        return history;
    }

    public List<HistoryEntry> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<HistoryEntry> history = new ArrayList<>();
        String sql = "SELECT * FROM history WHERE last_visit BETWEEN ? AND ? ORDER BY last_visit DESC";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(startDate));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    HistoryEntry entry = mapResultSetToHistoryEntry(rs);
                    history.add(entry);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding history entries by date range: " + startDate + " to " + endDate, e);
        }

        return history;
    }
    public void clearAll() {
        String sql = "DELETE FROM history";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error clearing history", e);
        }
    }

    @Override
    public void save(HistoryEntry entry) {
        String sql = "INSERT INTO history (user_id, title, url, favicon_url, visit_count, last_visit) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, 1); // Default user ID
            stmt.setString(2, entry.getTitle());
            stmt.setString(3, entry.getUrl());
            stmt.setString(4, entry.getFaviconUrl());
            stmt.setInt(5, entry.getVisitCount());
            stmt.setTimestamp(6, Timestamp.valueOf(entry.getLastVisit()));

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        entry.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving history entry", e);
        }
    }

    @Override
    public void update(HistoryEntry entry) {
        String sql = "UPDATE history SET title = ?, url = ?, favicon_url = ?, visit_count = ?, " +
                "last_visit = ? WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, entry.getTitle());
            stmt.setString(2, entry.getUrl());
            stmt.setString(3, entry.getFaviconUrl());
            stmt.setInt(4, entry.getVisitCount());
            stmt.setTimestamp(5, Timestamp.valueOf(entry.getLastVisit()));
            stmt.setInt(6, entry.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating history entry", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM history WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting history entry", e);
        }
    }

    private HistoryEntry mapResultSetToHistoryEntry(ResultSet rs) throws SQLException {
        HistoryEntry entry = new HistoryEntry();
        entry.setId(rs.getInt("id"));
        entry.setUserId(rs.getInt("user_id"));
        entry.setTitle(rs.getString("title"));
        entry.setUrl(rs.getString("url"));
        entry.setFaviconUrl(rs.getString("favicon_url"));
        entry.setVisitCount(rs.getInt("visit_count"));

        Timestamp timestamp = rs.getTimestamp("last_visit");
        if (timestamp != null) {
            entry.setLastVisit(timestamp.toLocalDateTime());
        }

        return entry;
    }
}


