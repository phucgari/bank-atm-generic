package com.training.atm.repository.db;

import com.training.atm.config.db.ConnectionManager;
import com.training.atm.model.ScheduledTransfer;
import com.training.atm.model.enums.TransferFrequency;
import com.training.atm.model.enums.TransferStatus;
import com.training.atm.model.state.TransferLifecycleState;
import com.training.atm.repository.ScheduledTransferRepository;
import com.training.atm.util.DateUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * H2 (JDBC) implementation of {@link ScheduledTransferRepository}.
 */
public class JdbcScheduledTransferRepository extends AbstractJdbcRepository<ScheduledTransfer, String> implements ScheduledTransferRepository {

    private static final String INSERT =
            "INSERT INTO scheduled_transfers (transfer_id, source_account_id, dest_account_id, amount,"
                    + " frequency, next_execution, status, max_repeats, executed_count)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE =
            "UPDATE scheduled_transfers SET source_account_id = ?, dest_account_id = ?, amount = ?,"
                    + " frequency = ?, next_execution = ?, status = ?, max_repeats = ?,"
                    + " executed_count = ? WHERE transfer_id = ?";

    private static final String SELECT_BY_ID =
            "SELECT transfer_id, source_account_id, dest_account_id, amount, frequency, next_execution,"
                    + " status, max_repeats, executed_count FROM scheduled_transfers"
                    + " WHERE transfer_id = ?";

    private static final String SELECT_BY_SOURCE =
            "SELECT transfer_id, source_account_id, dest_account_id, amount, frequency, next_execution,"
                    + " status, max_repeats, executed_count FROM scheduled_transfers"
                    + " WHERE source_account_id = ? ORDER BY next_execution";

    private static final String SELECT_ACTIVE =
            "SELECT transfer_id, source_account_id, dest_account_id, amount, frequency, next_execution,"
                    + " status, max_repeats, executed_count FROM scheduled_transfers"
                    + " WHERE status = 'ACTIVE' ORDER BY next_execution";

    private static final String COUNT_ACTIVE_BY_SOURCE =
            "SELECT COUNT(*) FROM scheduled_transfers"
                    + " WHERE source_account_id = ? AND status = 'ACTIVE'";

    public JdbcScheduledTransferRepository(ConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public ScheduledTransfer save(ScheduledTransfer st) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1, st.getId());
            setCommonParameters(ps, st, 2);
            ps.executeUpdate();
            return st;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving scheduled transfer: " + st.getId(), e);
        }
    }

    @Override
    public ScheduledTransfer update(ScheduledTransfer st) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            setCommonParameters(ps, st, 1);
            ps.setString(9, st.getId());
            ps.executeUpdate();
            return st;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating scheduled transfer: " + st.getId(), e);
        }
    }

    @Override
    public Optional<ScheduledTransfer> findById(String id) {
        return querySingle(SELECT_BY_ID, id);
    }

    @Override
    public List<ScheduledTransfer> findBySourceAccount(String accountNumber) {
        return queryList(SELECT_BY_SOURCE, accountNumber);
    }

    @Override
    public List<ScheduledTransfer> findAllActive() {
        return queryList(SELECT_ACTIVE);
    }

    @Override
    public long countActiveBySourceAccount(String accountNumber) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(COUNT_ACTIVE_BY_SOURCE)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting active transfers for account: " + accountNumber, e);
        }
    }

    private void setCommonParameters(PreparedStatement ps, ScheduledTransfer st, int start)
            throws SQLException {
        ps.setString(start, st.getSourceAccount());
        ps.setString(start + 1, st.getDestAccount());
        ps.setBigDecimal(start + 2, BigDecimal.valueOf(st.getAmount()));
        ps.setString(start + 3, st.getFrequency().name());
        ps.setTimestamp(start + 4, Timestamp.valueOf(LocalDate.parse(st.getNextExecutionDate(), DateUtil.DATE_FMT).atStartOfDay()));
        ps.setString(start + 5, st.getStatus().name());
        ps.setInt(start + 6, st.getMaxRepeat());
        ps.setInt(start + 7, st.getRepeatCount());
    }

    private Optional<ScheduledTransfer> querySingle(String sql, String value) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding scheduled transfer", e);
        }
        return Optional.empty();
    }

    private List<ScheduledTransfer> queryList(String sql) {
        List<ScheduledTransfer> results = new ArrayList<>();
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading scheduled transfers", e);
        }
        return results;
    }

    private List<ScheduledTransfer> queryList(String sql, String value) {
        List<ScheduledTransfer> results = new ArrayList<>();
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading scheduled transfers", e);
        }
        return results;
    }

    @Override
    protected ScheduledTransfer mapRow(ResultSet rs) throws SQLException {
        TransferLifecycleState state =
                ScheduledTransfer.stateFrom(TransferStatus.valueOf(rs.getString("status")));
        return new ScheduledTransfer(
                rs.getString("transfer_id"),
                rs.getString("source_account_id"),
                rs.getString("dest_account_id"),
                rs.getBigDecimal("amount").longValueExact(),
                TransferFrequency.valueOf(rs.getString("frequency")),
                rs.getTimestamp("next_execution").toLocalDateTime().toLocalDate().format(DateUtil.DATE_FMT),
                state,
                0,
                rs.getInt("executed_count"),
                "");
    }

    @Override
    protected String getTableName() {
        return "scheduled_transfers";
    }

    @Override
    protected String getIdColumnName() {
        return "transfer_id";
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, ScheduledTransfer entity) throws SQLException {
        ps.setString(1, entity.getId());
        setCommonParameters(ps, entity, 2);
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, ScheduledTransfer entity) throws SQLException {
        setCommonParameters(ps, entity, 1);
        ps.setString(9, entity.getId());
    }

    @Override
    protected String getInsertSQL() {
        return INSERT;
    }

    @Override
    protected String getUpdateSQL() {
        return UPDATE;
    }
}
