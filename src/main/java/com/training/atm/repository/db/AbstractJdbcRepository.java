package com.training.atm.repository.db;

import com.training.atm.config.db.ConnectionManager;
import com.training.atm.model.Identifiable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractJdbcRepository<T extends Identifiable<ID>, ID>
        implements GenericRepository<T, ID> {

    protected final ConnectionManager connectionManager;

    protected AbstractJdbcRepository(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    // Subclasses define these (Template Method pattern):
    protected abstract String getTableName();

    protected abstract String getIdColumnName();

    protected abstract T mapRow(ResultSet rs) throws SQLException;

    protected abstract void setInsertParameters(PreparedStatement ps, T entity)
            throws SQLException;

    protected abstract void setUpdateParameters(PreparedStatement ps, T entity)
            throws SQLException;

    protected abstract String getInsertSQL();

    protected abstract String getUpdateSQL();

    // Concrete CRUD methods implemented using safe resource management

    @Override
    public Optional<T> findById(ID id) {
        String sql = "SELECT * FROM " + getTableName()
                + " WHERE " + getIdColumnName() + " = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding entity by ID: " + id, e);
        }

        return Optional.empty();
    }

    @Override
    public List<T> findAll() {
        String sql = "SELECT * FROM " + getTableName();
        List<T> results = new ArrayList<>();

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all entities from " + getTableName(), e);
        }

        return results;
    }

    @Override
    public T save(T entity) {
        if (entity.getId() == null || findById(entity.getId()).isEmpty()) {
            return insert(entity);
        } else {
            return update(entity);
        }
    }

    protected T insert(T entity) {
        String sql = getInsertSQL();

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setInsertParameters(ps, entity);
            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating entity failed, no rows affected.");
            }

            // Optional: Handle auto-generated keys if IDs are database-generated
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    @SuppressWarnings("unchecked")
                    ID generatedId = (ID) generatedKeys.getObject(1);
                    entity.setId(generatedId);
                }
            }

            return entity;
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting entity into " + getTableName(), e);
        }
    }

    public T update(T entity) {
        String sql = getUpdateSQL();

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setUpdateParameters(ps, entity);
            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Updating entity failed, entity not found or no changes made.");
            }

            return entity;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating entity with ID: " + entity.getId(), e);
        }
    }

    /**
     * Edits an existing entity by first checking if it exists,
     * then delegating to the update workflow.
     */
    public T edit(T entity) {
        if (entity.getId() == null || !existsById(entity.getId())) {
            throw new IllegalArgumentException("Cannot edit entity because it does not exist with ID: " + entity.getId());
        }
        return update(entity);
    }

    @Override
    public boolean deleteById(ID id) {
        String sql = "DELETE FROM " + getTableName() + " WHERE " + getIdColumnName() + " = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting entity with ID: " + id, e);
        }
    }

    @Override
    public boolean existsById(ID id) {
        String sql = "SELECT 1 FROM " + getTableName() + " WHERE " + getIdColumnName() + " = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking existence for entity with ID: " + id, e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM " + getTableName();

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting entities in " + getTableName(), e);
        }

        return 0L;
    }
}