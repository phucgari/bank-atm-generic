package com.training.atm.repository.db;

import com.training.atm.config.db.ConnectionManager;
import com.training.atm.model.ATMConfig;
import com.training.atm.repository.ATMInfoRepository;
import com.training.atm.repository.DenominationRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * H2 (JDBC) implementation of both {@link ATMInfoRepository} and
 * {@link DenominationRepository}.
 *
 * <p>Mirrors the file-based implementation: ATM metadata comes from the single-row
 * {@code atm_info} table and cash inventory from the {@code denominations} table.
 * Like {@link com.training.atm.repository.file.FileATMConfigRepository}, both
 * concerns are exposed through two separate interfaces (ISP) while sharing one
 * implementation class.
 */
public class JdbcATMConfigRepository extends AbstractJdbcRepository<ATMConfig, Integer> implements ATMInfoRepository, DenominationRepository {

    private static final long MAX_CAPACITY = 500_000_000L;

    /** Denominations available for dispensing, ordered largest-first. */
    private static final long[] DENOM_ORDER = {500_000L, 200_000L, 100_000L, 50_000L};

    /**
     * The denomination slot that receives deposited cash (smallest cassette).
     */
    private static final long DEPOSIT_INTAKE_DENOMINATION = 50_000L;

    private static final String SELECT_ATM =
            "SELECT location, branch_name FROM atm_info WHERE id = 1";

    private static final String SELECT_DENOMINATIONS =
            "SELECT denomination, bill_count FROM denominations ORDER BY denomination DESC";

    private static final String SELECT_TOTAL_CASH =
            "SELECT COALESCE(SUM(denomination * bill_count), 0) FROM denominations";

    private static final String UPDATE_BILL_COUNT =
            "UPDATE denominations SET bill_count = bill_count + ? WHERE denomination = ?";

    private static final String UPSERT_BILL_COUNT =
            "INSERT INTO denominations (denomination, bill_count) VALUES (?, ?)"
                    + " ON DUPLICATE KEY UPDATE bill_count = bill_count + VALUES(bill_count)";

    public JdbcATMConfigRepository(ConnectionManager connectionManager) {
        super(connectionManager);
    }

    // --- ATMInfoRepository ---
    @Override
    public String getLocation() {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ATM);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString("location");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading ATM location from database", e);
        }
        return null;
    }

    @Override
    public String getBranchName() {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ATM);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString("branch_name");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading ATM branch name from database", e);
        }
        return null;
    }

    @Override
    public long getMaxCapacity() {
        return MAX_CAPACITY;
    }

    // --- DenominationRepository ---
    @Override
    public long getTotalCash() {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_TOTAL_CASH);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException("Error loading total ATM cash from database", e);
        }
    }

    @Override
    public Map<Long, Integer> getDenominations() {
        Map<Long, Integer> denominations = new LinkedHashMap<>();
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_DENOMINATIONS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                denominations.put(rs.getLong("denomination"), rs.getInt("bill_count"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading denominations from database", e);
        }
        return denominations;
    }

    @Override
    public boolean isValidDenomination(long denom) {
        for (long d : DENOM_ORDER) if (d == denom) return true;
        return false;
    }

    @Override
    public void dispenseBills(Map<Long, Integer> dispensed) {
        Connection conn = null;
        try {
            conn = connectionManager.getConnection();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(UPDATE_BILL_COUNT)) {
                for (Map.Entry<Long, Integer> entry : dispensed.entrySet()) {
                    ps.setInt(1, -entry.getValue());
                    ps.setLong(2, entry.getKey());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            rollback(conn);
            throw new RuntimeException("Error dispensing bills from database", e);
        } finally {
            closeQuietly(conn);
        }
    }

    @Override
    public void addDepositCash(long amount) {
        int billCount = (int) (amount / DEPOSIT_INTAKE_DENOMINATION);
        upsertBills(DEPOSIT_INTAKE_DENOMINATION, billCount);
    }

    @Override
    public boolean replenish(long denom, int count) {
        if (!isValidDenomination(denom)) return false;
        if (getTotalCash() + denom * count > MAX_CAPACITY) return false;
        upsertBills(denom, count);
        return true;
    }

    private void upsertBills(long denom, int count) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPSERT_BILL_COUNT)) {
            ps.setLong(1, denom);
            ps.setInt(2, count);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating bill count for denomination: " + denom, e);
        }
    }

    private void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                System.err.println("Error rolling back denomination update: " + e.getMessage());
            }
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    // --- AbstractJdbcRepository abstract methods ---
    @Override
    protected String getTableName() {
        return "atm_info";
    }

    @Override
    protected String getIdColumnName() {
        return "id";
    }

    @Override
    protected ATMConfig mapRow(ResultSet rs) throws SQLException {
        return new ATMConfig(
                rs.getInt("id"),
                rs.getString("location"),
                rs.getString("branch_name"));
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, ATMConfig entity) throws SQLException {
        ps.setInt(1, entity.getId());
        ps.setString(2, entity.getLocation());
        ps.setString(3, entity.getBranchName());
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, ATMConfig entity) throws SQLException {
        ps.setString(1, entity.getLocation());
        ps.setString(2, entity.getBranchName());
        ps.setInt(3, entity.getId());
    }

    @Override
    protected String getInsertSQL() {
        return "INSERT INTO atm_info (id, location, branch_name) VALUES (?, ?, ?)";
    }

    @Override
    protected String getUpdateSQL() {
        return "UPDATE atm_info SET location = ?, branch_name = ? WHERE id = ?";
    }
}
