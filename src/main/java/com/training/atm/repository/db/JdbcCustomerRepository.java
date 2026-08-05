package com.training.atm.repository.db;

import com.training.atm.config.db.ConnectionManager;
import com.training.atm.model.BankCustomer;
import com.training.atm.repository.CustomerRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * H2 (JDBC) implementation of {@link CustomerRepository}.
 */
public class JdbcCustomerRepository extends AbstractJdbcRepository<BankCustomer, String> implements CustomerRepository {

    private static final String SELECT_ALL =
            "SELECT c.customer_id, c.name, c.address, c.email, c.card_id, a.account_number"
                    + " FROM customers c JOIN accounts a ON c.account_id = a.account_id";

    public JdbcCustomerRepository(ConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public Optional<BankCustomer> findByCardId(String cardId) {
        return querySingle(SELECT_ALL + " WHERE c.card_id = ?", cardId);
    }

    @Override
    public Optional<BankCustomer> findByAccountNumber(String accountNumber) {
        return querySingle(SELECT_ALL + " WHERE a.account_number = ?", accountNumber);
    }

    @Override
    public List<BankCustomer> findAll() {
        List<BankCustomer> results = new ArrayList<>();
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading customers from database", e);
        }
        return Collections.unmodifiableList(results);
    }

    private Optional<BankCustomer> querySingle(String sql, String value) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding customer by parameter", e);
        }
        return Optional.empty();
    }

    @Override
    protected BankCustomer mapRow(ResultSet rs) throws SQLException {
        return new BankCustomer(
                rs.getString("customer_id"),
                rs.getString("name"),
                rs.getString("address"),
                rs.getString("email"),
                rs.getString("card_id"),
                rs.getString("account_number"));
    }

    @Override
    protected String getTableName() {
        return "customers";
    }

    @Override
    protected String getIdColumnName() {
        return "customer_id";
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, BankCustomer entity) throws SQLException {
        ps.setString(1, entity.getCustomerId());
        ps.setString(2, entity.getCustomerName());
        ps.setString(3, entity.getAddress());
        ps.setString(4, entity.getEmail());
        ps.setString(5, entity.getCardId());
        ps.setString(6, entity.getAccountNumber());
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, BankCustomer entity) throws SQLException {
        ps.setString(1, entity.getCustomerName());
        ps.setString(2, entity.getAddress());
        ps.setString(3, entity.getEmail());
        ps.setString(4, entity.getCardId());
        ps.setString(5, entity.getAccountNumber());
        ps.setString(6, entity.getCustomerId());
    }

    @Override
    protected String getInsertSQL() {
        return "INSERT INTO customers (customer_id, name, address, email, card_id, account_id) VALUES (?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected String getUpdateSQL() {
        return "UPDATE customers SET name = ?, address = ?, email = ?, card_id = ?, account_id = ? WHERE customer_id = ?";
    }
}
