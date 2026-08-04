package com.training.atm.repository.db;

import com.training.atm.config.db.ConnectionManager;
import com.training.atm.model.ScheduledTransfer;
import com.training.atm.model.enums.TransferFrequency;
import com.training.atm.model.enums.TransferStatus;
import com.training.atm.model.state.TransferLifecycleState;
import com.training.atm.repository.ScheduledTransferRepository;
import com.training.atm.util.DateUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * H2 (JDBC) implementation of {@link ScheduledTransferRepository}.
 *
 * <p>Mirrors {@code scheduled_transfers.txt}
 * (id|sourceAccount|destAccount|amount|frequency|nextExecutionDate|status|maxRepeat|repeatCount|endDate)
 * mapped onto the {@code scheduled_transfers} table.  State pattern preserved:
 * the persisted {@link TransferStatus} is only a serialisation token; {@code mapRow}
 * uses {@link ScheduledTransfer#stateFrom(TransferStatus)} to rebuild the runtime
 * {@link TransferLifecycleState} object.
 */
public class JdbcScheduledTransferRepository extends AbstractJdbcRepository<ScheduledTransfer, String> implements ScheduledTransferRepository {

    private static final String INSERT =
            "INSERT INTO scheduled_transfers (id, source_account, dest_account, amount,"
                    + " frequency, next_execution_date, status, max_repeat, repeat_count, end_date)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE =
            "UPDATE scheduled_transfers SET source_account = ?, dest_account = ?, amount = ?,"
                    + " frequency = ?, next_execution_date = ?, status = ?, max_repeat = ?,"
                    + " repeat_count = ?, end_date = ? WHERE id = ?";

    private static final String SELECT_BY_ID =
            "SELECT id, source_account, dest_account, amount, frequency, next_execution_date,"
                    + " status, max_repeat, repeat_count, end_date FROM scheduled_transfers"
                    + " WHERE id = ?";

    private static final String SELECT_BY_SOURCE =
            "SELECT id, source_account, dest_account, amount, frequency, next_execution_date,"
                    + " status, max_repeat, repeat_count, end_date FROM scheduled_transfers"
                    + " WHERE source_account = ? ORDER BY next_execution_date";

    private static final String SELECT_ACTIVE =
            "SELECT id, source_account, dest_account, amount, frequency, next_execution_date,"
                    + " status, max_repeat, repeat_count, end_date FROM scheduled_transfers"
                    + " WHERE status = 'ACTIVE' ORDER BY next_execution_date";

    private static final String COUNT_ACTIVE_BY_SOURCE =
            "SELECT COUNT(*) FROM scheduled_transfers"
                    + " WHERE source_account = ? AND status = 'ACTIVE'";

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
            ps.setString(10, st.getId());
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
        ps.setLong(start + 2, st.getAmount());
        ps.setString(start + 3, st.getFrequency().name());
        ps.setDate(start + 4, toDate(st.getNextExecutionDate()));
        ps.setString(start + 5, st.getStatus().name());   // getStatus() delegates to state
        ps.setInt(start + 6, st.getMaxRepeat());
        ps.setInt(start + 7, st.getRepeatCount());
        ps.setDate(start + 8, toDateOrNull(st.getEndDate()));
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
                ScheduledTransfer.stateFrom(TransferStatus.valueOf(rs.getString("status"))); // State pattern
        Date endDate = rs.getDate("end_date");
        return new ScheduledTransfer(
                rs.getString("id"),
                rs.getString("source_account"),
                rs.getString("dest_account"),
                rs.getLong("amount"),
                TransferFrequency.valueOf(rs.getString("frequency")),
                rs.getDate("next_execution_date").toLocalDate().format(DateUtil.DATE_FMT),
                state,
                rs.getInt("max_repeat"),
                rs.getInt("repeat_count"),
                endDate != null ? endDate.toLocalDate().format(DateUtil.DATE_FMT) : "");
    }

    private Date toDate(String date) {
        return Date.valueOf(LocalDate.parse(date, DateUtil.DATE_FMT));
    }

    private Date toDateOrNull(String date) {
        if (date == null || date.isEmpty()) return null;
        return toDate(date);
    }

    @Override
    protected String getTableName() {
        return "scheduled_transfers";
    }

    @Override
    protected String getIdColumnName() {
        return "id";
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, ScheduledTransfer entity) throws SQLException {
        ps.setString(1, entity.getId());
        setCommonParameters(ps, entity, 2);
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, ScheduledTransfer entity) throws SQLException {
        setCommonParameters(ps, entity, 1);
        ps.setString(10, entity.getId());
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
