package com.training.atm.repository.db;

import com.training.atm.config.db.ConnectionManager;
import com.training.atm.model.Transaction;
import com.training.atm.model.enums.TransactionType;
import com.training.atm.repository.TransactionRepository;
import com.training.atm.util.DateUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * H2 (JDBC) implementation of {@link TransactionRepository}.
 */
public class JdbcTransactionRepository extends AbstractJdbcRepository<Transaction, String> implements TransactionRepository {

    private static final String INSERT =
            "INSERT INTO transactions (transaction_id, account_id, type, amount, balance_after, created_at)"
                    + " VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ACCOUNT =
            "SELECT transaction_id, account_id, type, amount, balance_after, created_at"
                    + " FROM transactions WHERE account_id = ? ORDER BY created_at";

    private static final String SUM_BY_TYPE_AND_DATE =
            "SELECT COALESCE(SUM(amount), 0) FROM transactions"
                    + " WHERE account_id = ? AND type = ?"
                    + " AND CAST(created_at AS CHAR(19)) LIKE ?";

    public JdbcTransactionRepository(ConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public Transaction save(Transaction transaction) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1, transaction.getTransactionId());
            ps.setString(2, transaction.getAccountNumber());
            ps.setString(3, transaction.getType().name());
            ps.setBigDecimal(4, BigDecimal.valueOf(transaction.getAmount()));
            ps.setBigDecimal(5, BigDecimal.valueOf(transaction.getBalanceAfter()));
            ps.setTimestamp(6, toTimestamp(transaction.getDateTime()));
            ps.executeUpdate();
            return transaction;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving transaction: " + transaction.getTransactionId(), e);
        }
    }

    @Override
    public List<Transaction> findByAccountNumber(String accountNumber) {
        List<Transaction> results = new ArrayList<>();
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ACCOUNT)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading transactions for account: " + accountNumber, e);
        }
        return results;
    }

    @Override
    public long sumByAccountNumberTypeAndDate(String accountNumber, TransactionType type, String date) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SUM_BY_TYPE_AND_DATE)) {
            ps.setString(1, accountNumber);
            ps.setString(2, type.name());
            ps.setString(3, date + "%");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error summing " + type + " transactions for account: " + accountNumber, e);
        }
    }

    @Override
    protected Transaction mapRow(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getString("transaction_id"),
                rs.getString("account_id"),
                rs.getTimestamp("created_at").toLocalDateTime().format(DateUtil.DT_FMT),
                TransactionType.valueOf(rs.getString("type")),
                rs.getBigDecimal("amount").longValueExact(),
                rs.getBigDecimal("balance_after").longValueExact(),
                "");
    }

    private Timestamp toTimestamp(String dateTime) {
        return Timestamp.valueOf(LocalDateTime.parse(dateTime, DateUtil.DT_FMT));
    }

    @Override
    protected String getTableName() {
        return "transactions";
    }

    @Override
    protected String getIdColumnName() {
        return "transaction_id";
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, Transaction entity) throws SQLException {
        ps.setString(1, entity.getTransactionId());
        ps.setString(2, entity.getAccountNumber());
        ps.setString(3, entity.getType().name());
        ps.setBigDecimal(4, BigDecimal.valueOf(entity.getAmount()));
        ps.setBigDecimal(5, BigDecimal.valueOf(entity.getBalanceAfter()));
        ps.setTimestamp(6, toTimestamp(entity.getDateTime()));
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, Transaction entity) throws SQLException {
        ps.setString(1, entity.getAccountNumber());
        ps.setString(2, entity.getType().name());
        ps.setBigDecimal(3, BigDecimal.valueOf(entity.getAmount()));
        ps.setBigDecimal(4, BigDecimal.valueOf(entity.getBalanceAfter()));
        ps.setTimestamp(5, toTimestamp(entity.getDateTime()));
        ps.setString(6, entity.getTransactionId());
    }

    @Override
    protected String getInsertSQL() {
        return INSERT;
    }

    @Override
    protected String getUpdateSQL() {
        return "UPDATE transactions SET account_id = ?, type = ?, amount = ?, balance_after = ?, created_at = ? WHERE transaction_id = ?";
    }
}
