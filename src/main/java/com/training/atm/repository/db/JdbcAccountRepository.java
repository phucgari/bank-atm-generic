package com.training.atm.repository.db;

import com.training.atm.config.db.ConnectionManager;
import com.training.atm.model.Account;
import com.training.atm.model.CurrentAccount;
import com.training.atm.model.SavingsAccount;
import com.training.atm.model.enums.AccountType;
import com.training.atm.model.strategy.CompoundInterestStrategy;
import com.training.atm.model.strategy.ZeroOnNegativeInterestStrategy;
import com.training.atm.repository.AccountRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * H2 (JDBC) implementation of {@link AccountRepository}.
 *
 * <p>Mirrors {@code accounts.txt} (accountNumber|type|balance|lastInterestYearMonth)
 * mapped onto the {@code accounts} table.  Strategy pattern preserved: account
 * objects are rebuilt with the same interest algorithm the file repository
 * injects, so behaviour is identical regardless of persistence backend.
 */
public class JdbcAccountRepository extends AbstractJdbcRepository<Account,String> implements AccountRepository {

    private static final String SELECT_ALL =
            "SELECT account_number, account_type, balance, last_interest_year_month"
                    + " FROM accounts";

    private static final String UPDATE =
            "UPDATE accounts SET balance = ?, last_interest_year_month = ?"
                    + " WHERE account_number = ?";

    public JdbcAccountRepository(ConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    protected String getTableName() {
        return "accounts";
    }

    @Override
    protected String getIdColumnName() {
        return "account_number";
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL + " WHERE account_number = ?")) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding account by account number: " + accountNumber, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Account> findAll() {
        List<Account> results = new ArrayList<>();
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading accounts from database", e);
        }
        return Collections.unmodifiableList(results);
    }

    @Override
    public Account update(Account account) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setLong(1, account.getAccountBalance());
            ps.setString(2, account.getLastInterestYearMonth());
            ps.setString(3, account.getAccountNumber());
            ps.executeUpdate();
            return account;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating account: " + account.getAccountNumber(), e);
        }
    }

    @Override
    public void updateAll(Collection<Account> updated) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            for (Account account : updated) {
                ps.setLong(1, account.getAccountBalance());
                ps.setString(2, account.getLastInterestYearMonth());
                ps.setString(3, account.getAccountNumber());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Error batch-updating accounts", e);
        }
    }

    protected Account mapRow(ResultSet rs) throws SQLException {
        String accountNumber = rs.getString("account_number");
        long balance = rs.getLong("balance");
        String lastInterestYearMonth = rs.getString("last_interest_year_month");
        AccountType type = AccountType.valueOf(rs.getString("account_type"));
        // Strategy pattern: each account type gets its own algorithm injected.
        return switch (type) {
            case SAVINGS -> new SavingsAccount(accountNumber, balance, lastInterestYearMonth,
                    new CompoundInterestStrategy(SavingsAccount.MONTHLY_RATE));
            case CURRENT -> new CurrentAccount(accountNumber, balance, lastInterestYearMonth,
                    new ZeroOnNegativeInterestStrategy(CurrentAccount.MONTHLY_RATE));
        };
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, Account entity) throws SQLException {
        ps.setString(1, entity.getAccountNumber());
        ps.setString(2, entity.getAccountType().name());
        ps.setLong(3, entity.getAccountBalance());
        ps.setString(4, entity.getLastInterestYearMonth());
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, Account entity) throws SQLException {
        ps.setString(1, entity.getAccountType().name());
        ps.setLong(2, entity.getAccountBalance());
        ps.setString(3, entity.getLastInterestYearMonth());
        ps.setString(4, entity.getAccountNumber());
    }

    @Override
    protected String getInsertSQL() {
        return "INSERT INTO accounts (account_number, account_type, balance, last_interest_year_month) VALUES (?, ?, ?, ?)";
    }

    @Override
    protected String getUpdateSQL() {
        return "UPDATE accounts SET account_type = ?, balance = ?, last_interest_year_month = ? WHERE account_number = ?";
    }
}
