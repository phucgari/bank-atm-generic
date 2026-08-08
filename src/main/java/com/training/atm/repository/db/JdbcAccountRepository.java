package com.training.atm.repository.db;

import com.training.atm.config.db.ConnectionManager;
import com.training.atm.model.Account;
import com.training.atm.model.CurrentAccount;
import com.training.atm.model.SavingsAccount;
import com.training.atm.model.enums.AccountType;
import com.training.atm.model.strategy.CompoundInterestStrategy;
import com.training.atm.model.strategy.ZeroOnNegativeInterestStrategy;
import com.training.atm.repository.AccountRepository;

import java.math.BigDecimal;
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
 */
public class JdbcAccountRepository extends AbstractJdbcRepository<Account,String> implements AccountRepository {

    private static final String SELECT_ALL =
            "SELECT account_id, account_number, account_type, balance, interest_rate, min_balance, overdraft_limit"
                    + " FROM accounts";

    private static final String UPDATE =
            "UPDATE accounts SET balance = ?, interest_rate = ?, account_type = ?, min_balance = ?, overdraft_limit = ?"
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
            setUpdateParameters(ps, account);
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
                setUpdateParameters(ps, account);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Error batch-updating accounts", e);
        }
    }

    protected Account mapRow(ResultSet rs) throws SQLException {
        String accountNumber = rs.getString("account_number");
        long balance = rs.getBigDecimal("balance").longValueExact();
        String lastInterestYearMonth = "";
        AccountType type = AccountType.valueOf(rs.getString("account_type"));
        double interestRate = rs.getBigDecimal("interest_rate").doubleValue();
        long minimumBalance = rs.getBigDecimal("min_balance").longValueExact();
        long overdraftLimit = rs.getBigDecimal("overdraft_limit").longValueExact();
        return switch (type) {
            case SAVINGS -> new SavingsAccount(accountNumber, balance, lastInterestYearMonth,
                    new CompoundInterestStrategy(interestRate), minimumBalance);
            case CURRENT -> new CurrentAccount(accountNumber, balance, lastInterestYearMonth,
                    new ZeroOnNegativeInterestStrategy(interestRate), -overdraftLimit);
        };
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, Account entity) throws SQLException {
        ps.setString(1, entity.getAccountNumber());
        ps.setString(2, entity.getAccountNumber());
        ps.setBigDecimal(3, BigDecimal.valueOf(entity.getAccountBalance()));
        ps.setBigDecimal(4, BigDecimal.valueOf(entity.getInterestRate()));
        ps.setString(5, entity.getAccountType().name());
        if (entity instanceof SavingsAccount) {
            ps.setBigDecimal(6, BigDecimal.valueOf(entity.getWithdrawalFloor()));
            ps.setBigDecimal(7, BigDecimal.ZERO);
        } else {
            ps.setBigDecimal(6, BigDecimal.ZERO);
            ps.setBigDecimal(7, BigDecimal.valueOf(-entity.getWithdrawalFloor()));
        }
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, Account entity) throws SQLException {
        ps.setBigDecimal(1, BigDecimal.valueOf(entity.getAccountBalance()));
        ps.setBigDecimal(2, BigDecimal.valueOf(entity.getInterestRate()));
        ps.setString(3, entity.getAccountType().name());
        if (entity instanceof SavingsAccount) {
            ps.setBigDecimal(4, BigDecimal.valueOf(entity.getWithdrawalFloor()));
            ps.setBigDecimal(5, BigDecimal.ZERO);
        } else {
            ps.setBigDecimal(4, BigDecimal.ZERO);
            ps.setBigDecimal(5, BigDecimal.valueOf(-entity.getWithdrawalFloor()));
        }
        ps.setString(6, entity.getAccountNumber());
    }

    @Override
    protected String getInsertSQL() {
        return "INSERT INTO accounts (account_id, account_number, balance, interest_rate, account_type, min_balance, overdraft_limit) VALUES (?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected String getUpdateSQL() {
        return "UPDATE accounts SET balance = ?, interest_rate = ?, account_type = ?, min_balance = ?, overdraft_limit = ? WHERE account_number = ?";
    }
}
