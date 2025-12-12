package com.example.nexus.repository;


import com.example.nexus.util.DatabaseManager;
import com.example.nexus.model.Profile;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProfileRepository extends BaseRepository<Profile> {
    public ProfileRepository(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override
    public List<Profile> findAll() {
        List<Profile> profiles = new ArrayList<>();
        String sql = "SELECT * FROM profiles ORDER BY name";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Profile profile = mapResultSetToProfile(rs);
                profiles.add(profile);
            }
        } catch (SQLException e) {
            logger.error("Error finding all profiles", e);
        }

        return profiles;
    }

    @Override
    public Profile findById(int id) {
        String sql = "SELECT * FROM profiles WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProfile(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding profile by ID: " + id, e);
        }

        return null;
    }

    @Override
    public void save(Profile profile) {
        String sql = "INSERT INTO profiles (name, avatar_path) VALUES (?, ?)";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, profile.getName());
            stmt.setString(2, profile.getAvatarPath());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        profile.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving profile", e);
        }
    }

    @Override
    public void update(Profile profile) {
        String sql = "UPDATE profiles SET name = ?, avatar_path = ? WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, profile.getName());
            stmt.setString(2, profile.getAvatarPath());
            stmt.setInt(3, profile.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating profile", e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM profiles WHERE id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting profile", e);
        }
    }

    private Profile mapResultSetToProfile(ResultSet rs) throws SQLException {
        Profile profile = new Profile();
        profile.setId(rs.getInt("id"));
        profile.setName(rs.getString("name"));
        profile.setAvatarPath(rs.getString("avatar_path"));
        return profile;
    }
}
