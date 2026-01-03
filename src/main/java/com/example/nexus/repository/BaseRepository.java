package com.example.nexus.repository;

import com.example.nexus.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public abstract class BaseRepository<T> {
    protected static final Logger logger = LoggerFactory.getLogger(BaseRepository.class);

    protected final DatabaseManager dbManager;

    public BaseRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    protected Connection getConnection() throws SQLException {
        return dbManager.getConnection();
    }

    protected void closeResources(ResultSet rs, PreparedStatement stmt) {
        try {
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            logger.error("Error closing resources", e);
        }
    }

    public abstract List<T> findAll();

    public abstract T findById(int id);

    public abstract void save(T entity);

    public abstract void update(T entity);

    public abstract void delete(int id);
}
