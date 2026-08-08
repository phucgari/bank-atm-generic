package com.training.atm.repository.db;

import com.training.atm.config.db.ConnectionManager;
import com.training.atm.model.ATMConfig;
import com.training.atm.repository.ATMInfoRepository;
import com.training.atm.repository.DenominationRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * H2 (JDBC) implementation of both {@link ATMInfoRepository} and
 * {@link DenominationRepository}.
 */
public class JdbcATMConfigRepository extends AbstractJdbcRepository<ATMConfig, Integer> implements ATMInfoRepository, DenominationRepository {

    private static final long MAX_CAPACITY = 500_000_000L;
    private static final long[] DENOM_ORDER = {500_000L, 200_000L, 100_000L, 50_000L};
    private static final long DEPOSIT_INTAKE_DENOMINATION = 50_000L;

    private static final String SELECT_ATM =
            "SELECT atm_id, location, branch_name, total_cash, denomination_500k, denomination_200k, denomination_100k, denomination_50k"
                    + " FROM atm_machines ORDER BY atm_id LIMIT 1";

    private static final String SELECT_TOTAL_CASH =
            "SELECT total_cash FROM atm_machines ORDER BY atm_id LIMIT 1";

    private static final String INSERT_ATM =
            "INSERT INTO atm_machines (atm_id, location, branch_name, total_cash, denomination_500k, denomination_200k, denomination_100k, denomination_50k)"
                    + " VALUES (?, ?, ?, 0, 0, 0, 0, 0)";

    public JdbcATMConfigRepository(ConnectionManager connectionManager) {
        super(connectionManager);
    }

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

    @Override
    public long getTotalCash() {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_TOTAL_CASH);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("total_cash");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading total ATM cash from database", e);
        }
        return 0L;
    }

    @Override
    public Map<Long, Integer> getDenominations() {
        Map<Long, Integer> denominations = new LinkedHashMap<>();
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ATM);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                denominations.put(500_000L, rs.getInt("denomination_500k"));
                denominations.put(200_000L, rs.getInt("denomination_200k"));
                denominations.put(100_000L, rs.getInt("denomination_100k"));
                denominations.put(50_000L, rs.getInt("denomination_50k"));
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
        try (Connection conn = connectionManager.getConnection()) {
            long atmId = ensureAtmMachine(conn);
            for (Map.Entry<Long, Integer> entry : dispensed.entrySet()) {
                adjustDenomination(conn, atmId, entry.getKey(), -entry.getValue());
            }
            updateTotalCash(conn, atmId);
        } catch (SQLException e) {
            throw new RuntimeException("Error dispensing bills from database", e);
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
        try (Connection conn = connectionManager.getConnection()) {
            long atmId = ensureAtmMachine(conn);
            adjustDenomination(conn, atmId, denom, count);
            updateTotalCash(conn, atmId);
        } catch (SQLException e) {
            throw new RuntimeException("Error updating bill count for denomination: " + denom, e);
        }
    }

    private long ensureAtmMachine(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_ATM);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return 1L;
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(INSERT_ATM)) {
            ps.setString(1, "1");
            ps.setString(2, "Main Branch");
            ps.setString(3, "Main Branch");
            ps.executeUpdate();
        }
        return 1L;
    }

    private void adjustDenomination(Connection conn, long atmId, long denom, int delta) throws SQLException {
        String column = switch ((int) denom) {
            case 500_000 -> "denomination_500k";
            case 200_000 -> "denomination_200k";
            case 100_000 -> "denomination_100k";
            case 50_000 -> "denomination_50k";
            default -> throw new IllegalArgumentException("Unsupported denomination: " + denom);
        };
        try (PreparedStatement ps = conn.prepareStatement("UPDATE atm_machines SET " + column + " = " + column + " + ? WHERE atm_id = ?")) {
            ps.setInt(1, delta);
            ps.setString(2, "1");
            ps.executeUpdate();
        }
    }

    private void updateTotalCash(Connection conn, long atmId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE atm_machines SET total_cash = (denomination_500k * 500000 + denomination_200k * 200000 + denomination_100k * 100000 + denomination_50k * 50000) WHERE atm_id = ?")) {
            ps.setString(1, "1");
            ps.executeUpdate();
        }
    }

    @Override
    protected String getTableName() {
        return "atm_machines";
    }

    @Override
    protected String getIdColumnName() {
        return "atm_id";
    }

    @Override
    protected ATMConfig mapRow(ResultSet rs) throws SQLException {
        return new ATMConfig(
                1,
                rs.getString("location"),
                rs.getString("branch_name"));
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, ATMConfig entity) throws SQLException {
        ps.setString(1, String.valueOf(entity.getId()));
        ps.setString(2, entity.getLocation());
        ps.setString(3, entity.getBranchName());
        ps.setLong(4, 0L);
        ps.setInt(5, 0);
        ps.setInt(6, 0);
        ps.setInt(7, 0);
        ps.setInt(8, 0);
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, ATMConfig entity) throws SQLException {
        ps.setString(1, entity.getLocation());
        ps.setString(2, entity.getBranchName());
        ps.setString(3, String.valueOf(entity.getId()));
    }

    @Override
    protected String getInsertSQL() {
        return "INSERT INTO atm_machines (atm_id, location, branch_name, total_cash, denomination_500k, denomination_200k, denomination_100k, denomination_50k) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected String getUpdateSQL() {
        return "UPDATE atm_machines SET location = ?, branch_name = ? WHERE atm_id = ?";
    }
}
