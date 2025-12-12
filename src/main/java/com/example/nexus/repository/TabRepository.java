package com.example.nexus.repository;

import com.example.nexus.model.Tab;
import com.example.nexus.repository.BaseRepository;
import com.example.nexus.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TabRepository extends BaseRepository<Tab> {
    private static final Logger logger = LoggerFactory.getLogger(TabRepository.class);

    public TabRepository(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override
    public List<Tab> findAll() {
        List<Tab> tabs = new ArrayList<>();
        String sql = "SELECT * FROM tabs ORDER BY position";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Tab tab = mapResultSetToTab(rs);
                tabs.add(tab);
            }
        } catch (SQLException e) {
            logger.error("Error finding all tabs", e);
        }

        return tabs;
    }

    @Override
    public Tab findById(int id) {
        String sql = "SELECT * FROM tabs WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTab(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding tab by ID: " + id, e);
        }

        return null;
    }

    public List<Tab> findBySessionId(String sessionId) {
        List<Tab> tabs = new ArrayList<>();
        String sql = "SELECT * FROM tabs WHERE session_id = ? ORDER BY position";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, sessionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Tab tab = mapResultSetToTab(rs);
                    tabs.add(tab);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding tabs by session ID: " + sessionId, e);
        }

        return tabs;
    }

    @Override
    public void save(Tab tab) {
        String sql = "INSERT INTO tabs (user_id, title, url, favicon_url, is_pinned, is_active, position, session_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, 1); // Default user ID
            stmt.setString(2, tab.getTitle());
            stmt.setString(3, tab.getUrl());
            stmt.setString(4, tab.getFaviconUrl());
            stmt.setBoolean(5, tab.isPinned());
            stmt.setBoolean(6, tab.isActive());
            stmt.setInt(7, tab.getPosition());
            stmt.setString(8, tab.getSessionId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        tab.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving tab", e);
        }
    }

    @Override
    public void update(Tab tab) {
        String sql = "UPDATE tabs SET title = ?, url = ?, favicon_url = ?, is_pinned = ?, is_active = ?, " +
                "position = ?, session_id = ? WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, tab.getTitle());
            stmt.setString(2, tab.getUrl());
            stmt.setString(3, tab.getFaviconUrl());
            stmt.setBoolean(4, tab.isPinned());
            stmt.setBoolean(5, tab.isActive());
            stmt.setInt(6, tab.getPosition());
            stmt.setString(7, tab.getSessionId());
            stmt.setInt(8, tab.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating tab", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM tabs WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting tab", e);
        }
    }

    private Tab mapResultSetToTab(ResultSet rs) throws SQLException {
        Tab tab = new Tab();
        tab.setId(rs.getInt("id"));
        tab.setTitle(rs.getString("title"));
        tab.setUrl(rs.getString("url"));
        tab.setFaviconUrl(rs.getString("favicon_url"));
        tab.setPinned(rs.getBoolean("is_pinned"));
        tab.setActive(rs.getBoolean("is_active"));
        tab.setPosition(rs.getInt("position"));
        tab.setSessionId(rs.getString("session_id"));
        return tab;
    }
}