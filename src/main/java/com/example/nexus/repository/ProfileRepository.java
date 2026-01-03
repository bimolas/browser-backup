package com.example.nexus.repository;

import com.example.nexus.model.Profile;
import com.example.nexus.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProfileRepository extends BaseRepository {
    private static final Logger logger = LoggerFactory.getLogger(ProfileRepository.class);

    public ProfileRepository(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override
    public Profile findById(int id) {
        String sql = "SELECT id, username, email, profile_image_path, password_hash, is_guest, logged_in FROM profile WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProfile(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding profile by id: {}", id, e);
        }
        return null;
    }

    @Override
    public void save(Object entity) {
        if (entity instanceof Profile) {
            save((Profile) entity);
        }
    }

    @Override
    public void update(Object entity) {
        if (entity instanceof Profile) {
            update((Profile) entity);
        }
    }

    @Override
    public List<Profile> findAll() {
        List<Profile> profiles = new ArrayList<>();
        String sql = "SELECT id, username, email, profile_image_path, password_hash, is_guest, logged_in FROM profile";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                profiles.add(mapResultSetToProfile(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all profiles", e);
        }
        return profiles;
    }

    public void save(Profile profile) {
        String sql = "INSERT INTO profile (username, email, profile_image_path, password_hash, is_guest, logged_in) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, profile.getUsername());
            stmt.setString(2, profile.getEmail());
            stmt.setString(3, profile.getProfileImagePath());
            stmt.setString(4, profile.getPasswordHash());
            stmt.setBoolean(5, profile.isGuest());
            stmt.setBoolean(6, profile.isLoggedIn());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    profile.setId((int) generatedKeys.getLong(1));
                }
            }
            logger.info("Profile saved: {}", profile.getUsername());
        } catch (SQLException e) {
            logger.error("Error saving profile", e);
        }
    }

    public void update(Profile profile) {
        String sql = "UPDATE profile SET username = ?, email = ?, profile_image_path = ?, password_hash = ?, is_guest = ?, logged_in = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, profile.getUsername());
            stmt.setString(2, profile.getEmail());
            stmt.setString(3, profile.getProfileImagePath());
            stmt.setString(4, profile.getPasswordHash());
            stmt.setBoolean(5, profile.isGuest());
            stmt.setBoolean(6, profile.isLoggedIn());
            stmt.setInt(7, profile.getId());
            stmt.executeUpdate();
            logger.info("Profile updated: {}", profile.getUsername());
        } catch (SQLException e) {
            logger.error("Error updating profile", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM profile WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            logger.info("Profile deleted: {}", id);
        } catch (SQLException e) {
            logger.error("Error deleting profile", e);
        }
    }

    private Profile mapResultSetToProfile(ResultSet rs) throws SQLException {
        Profile profile = new Profile();
        profile.setId(rs.getInt("id"));
        profile.setUsername(rs.getString("username"));
        profile.setEmail(rs.getString("email"));
        profile.setProfileImagePath(rs.getString("profile_image_path"));

        try {
            profile.setPasswordHash(rs.getString("password_hash"));
        } catch (SQLException e) {
            logger.debug("password_hash column not found, setting to null");
            profile.setPasswordHash(null);
        }

        try {
            profile.setGuest(rs.getBoolean("is_guest"));
        } catch (SQLException e) {
            logger.debug("is_guest column not found, setting to false");
            profile.setGuest(false);
        }

        try {
            profile.setLoggedIn(rs.getBoolean("logged_in"));
        } catch (SQLException e) {
            logger.debug("logged_in column not found, setting to true");
            profile.setLoggedIn(true);
        }

        return profile;
    }

    public Profile findByEmail(String email) {
        String sql = "SELECT * FROM profile WHERE email = ? LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToProfile(rs);
            }
        } catch (SQLException e) {
            logger.error("Error finding profile by email", e);
        }
        return null;
    }

    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM profile WHERE email = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking email existence", e);
        }
        return false;
    }

    public List<Profile> findLoggedInProfiles() {
        List<Profile> profiles = new ArrayList<>();
        String sql = "SELECT id, username, email, profile_image_path, password_hash, is_guest, logged_in FROM profile WHERE logged_in = 1";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                profiles.add(mapResultSetToProfile(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding logged in profiles", e);
        }
        return profiles;
    }
}

