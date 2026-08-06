package com.training.atm.repository.db;

import com.training.atm.config.db.ConnectionManager;
import com.training.atm.model.AdminLog;
import com.training.atm.repository.AdminLogRepository;
import com.training.atm.util.DateUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * H2 (JDBC) implementation of {@link AdminLogRepository}.
 */
public class JdbcAdminLogRepository extends AbstractJdbcRepository<AdminLog, Long> implements AdminLogRepository {

    private static final String INSERT =
            "INSERT INTO admin_audit_log (created_at, admin_username, action) VALUES (?, ?, ?)";

    public JdbcAdminLogRepository(ConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public void log(String timestamp, String adminUser, String action) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setTimestamp(1, toTimestamp(timestamp));
            ps.setString(2, adminUser);
            ps.setString(3, action);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error writing admin log", e);
        }
    }

    private Timestamp toTimestamp(String timestamp) {
        return Timestamp.valueOf(LocalDateTime.parse(timestamp, DateUtil.DT_FMT));
    }

    @Override
    protected String getTableName() {
        return "admin_audit_log";
    }

    @Override
    protected String getIdColumnName() {
        return "log_id";
    }

    @Override
    protected AdminLog mapRow(ResultSet rs) throws SQLException {
        return new AdminLog(
                Long.valueOf(rs.getString("log_id")),
                rs.getTimestamp("created_at").toLocalDateTime().format(DateUtil.DT_FMT),
                rs.getString("admin_username"),
                rs.getString("action"));
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, AdminLog entity) throws SQLException {
        ps.setTimestamp(1, toTimestamp(entity.getLogTime()));
        ps.setString(2, entity.getAdminUser());
        ps.setString(3, entity.getAction());
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, AdminLog entity) throws SQLException {
        ps.setTimestamp(1, toTimestamp(entity.getLogTime()));
        ps.setString(2, entity.getAdminUser());
        ps.setString(3, entity.getAction());
        ps.setObject(4, null);
        ps.setString(5, String.valueOf(entity.getId()));
    }

    @Override
    protected String getInsertSQL() {
        return INSERT;
    }

    @Override
    protected String getUpdateSQL() {
        return "UPDATE admin_audit_log SET created_at = ?, admin_username = ?, action = ?, details = ? WHERE log_id = ?";
    }
}

