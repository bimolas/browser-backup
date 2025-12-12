package com.example.nexus.repository;


import com.example.nexus.model.Download;
import com.example.nexus.util.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DownloadRepository extends BaseRepository<Download> {
    public DownloadRepository(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override
    public List<Download> findAll() {
        List<Download> downloads = new ArrayList<>();
        String sql = "SELECT * FROM downloads ORDER BY start_time DESC";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Download download = mapResultSetToDownload(rs);
                downloads.add(download);
            }
        } catch (SQLException e) {
            logger.error("Error finding all downloads", e);
        }

        return downloads;
    }

    @Override
    public Download findById(int id) {
        String sql = "SELECT * FROM downloads WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDownload(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding download by ID: " + id, e);
        }

        return null;
    }

    @Override
    public void save(Download download) {
        String sql = "INSERT INTO downloads (user_id, url, file_name, file_path, file_size, " +
                "downloaded_size, status, start_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, 1); // Default user ID
            stmt.setString(2, download.getUrl());
            stmt.setString(3, download.getFileName());
            stmt.setString(4, download.getFilePath());
            stmt.setLong(5, download.getFileSize());
            stmt.setLong(6, download.getDownloadedSize());
            stmt.setString(7, download.getStatus());
            stmt.setTimestamp(8, Timestamp.valueOf(download.getStartTime()));

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        download.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving download", e);
        }
    }

    @Override
    public void update(Download download) {
        String sql = "UPDATE downloads SET url = ?, file_name = ?, file_path = ?, file_size = ?, " +
                "downloaded_size = ?, status = ?, end_time = ? WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, download.getUrl());
            stmt.setString(2, download.getFileName());
            stmt.setString(3, download.getFilePath());
            stmt.setLong(4, download.getFileSize());
            stmt.setLong(5, download.getDownloadedSize());
            stmt.setString(6, download.getStatus());

            if (download.getEndTime() != null) {
                stmt.setTimestamp(7, Timestamp.valueOf(download.getEndTime()));
            } else {
                stmt.setNull(7, java.sql.Types.TIMESTAMP);
            }

            stmt.setInt(8, download.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating download", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM downloads WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting download", e);
        }
    }

    public void clearAll() {
        String sql = "DELETE FROM downloads";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error clearing downloads", e);
        }
    }

    private Download mapResultSetToDownload(ResultSet rs) throws SQLException {
        Download download = new Download();
        download.setId(rs.getInt("id"));
        download.setUserId(rs.getInt("user_id"));
        download.setUrl(rs.getString("url"));
        download.setFileName(rs.getString("file_name"));
        download.setFilePath(rs.getString("file_path"));
        download.setFileSize(rs.getLong("file_size"));
        download.setDownloadedSize(rs.getLong("downloaded_size"));
        download.setStatus(rs.getString("status"));

        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            download.setStartTime(startTime.toLocalDateTime());
        }

        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) {
            download.setEndTime(endTime.toLocalDateTime());
        }

        return download;
    }
}