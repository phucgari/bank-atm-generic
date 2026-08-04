package com.training.atm.repository.db;

import com.training.atm.config.db.ConnectionManager;
import com.training.atm.model.Transaction;
import com.training.atm.model.enums.TransactionType;
import com.training.atm.repository.TransactionRepository;
import com.training.atm.util.DateUtil;

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
 *
 * <p>Mirrors {@code transactions.txt} (txId|accountNumber|dateTime|type|amount|balanceAfter|description)
 * mapped onto the {@code transactions} table.  {@code date_time} is stored as a
 * {@code DATETIME} and converted to the application's {@code "yyyy-MM-dd HH:mm:ss"}
 * string convention on read/write.
 */
public class JdbcTransactionRepository extends AbstractJdbcRepository<Transaction, String> implements TransactionRepository {

    private static final String INSERT =
            "INSERT INTO transactions (transaction_id, account_number, date_time, type,"
                    + " amount, balance_after, description) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ACCOUNT =
            "SELECT transaction_id, account_number, date_time, type, amount, balance_after, description"
                    + " FROM transactions WHERE account_number = ? ORDER BY date_time";

    private static final String SUM_BY_TYPE_AND_DATE =
            "SELECT COALESCE(SUM(amount), 0) FROM transactions"
                    + " WHERE account_number = ? AND type = ?"
                    + " AND CAST(date_time AS VARCHAR(19)) LIKE ?";

    public JdbcTransactionRepository(ConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public Transaction save(Transaction transaction) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1, transaction.getTransactionId());
            ps.setString(2, transaction.getAccountNumber());
            ps.setTimestamp(3, toTimestamp(transaction.getDateTime()));
            ps.setString(4, transaction.getType().name());
            ps.setLong(5, transaction.getAmount());
            ps.setLong(6, transaction.getBalanceAfter());
            ps.setString(7, transaction.getDescription());
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
                rs.getString("account_number"),
                rs.getTimestamp("date_time").toLocalDateTime().format(DateUtil.DT_FMT),
                TransactionType.valueOf(rs.getString("type")),
                rs.getLong("amount"),
                rs.getLong("balance_after"),
                rs.getString("description"));
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
        ps.setTimestamp(3, toTimestamp(entity.getDateTime()));
        ps.setString(4, entity.getType().name());
        ps.setLong(5, entity.getAmount());
        ps.setLong(6, entity.getBalanceAfter());
        ps.setString(7, entity.getDescription());
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, Transaction entity) throws SQLException {
        ps.setString(1, entity.getAccountNumber());
        ps.setTimestamp(2, toTimestamp(entity.getDateTime()));
        ps.setString(3, entity.getType().name());
        ps.setLong(4, entity.getAmount());
        ps.setLong(5, entity.getBalanceAfter());
        ps.setString(6, entity.getDescription());
        ps.setString(7, entity.getTransactionId());
    }

    @Override
    protected String getInsertSQL() {
        return INSERT;
    }

    @Override
    protected String getUpdateSQL() {
        return "UPDATE transactions SET account_number = ?, date_time = ?, type = ?, amount = ?, balance_after = ?, description = ? WHERE transaction_id = ?";
    }
}
