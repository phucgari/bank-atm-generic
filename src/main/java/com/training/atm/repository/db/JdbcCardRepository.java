package com.training.atm.repository.db;

import com.training.atm.config.db.ConnectionManager;
import com.training.atm.model.ATMCard;
import com.training.atm.model.enums.CardStatus;
import com.training.atm.repository.CardRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * H2 (JDBC) implementation of {@link CardRepository}.
 */
public class JdbcCardRepository extends AbstractJdbcRepository<ATMCard, String> implements CardRepository {

    private static final String SELECT_BY_ID =
            "SELECT card_id, pin_hash, status, failed_attempts, linked_account_id"
                    + " FROM atm_cards WHERE card_id = ?";

    private static final String UPDATE =
            "UPDATE atm_cards SET pin_hash = ?, status = ?, failed_attempts = ?, linked_account_id = ?"
                    + " WHERE card_id = ?";

    public JdbcCardRepository(ConnectionManager connectionManager) {
        super(connectionManager);
    }

    @Override
    public Optional<ATMCard> findById(String cardId) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setString(1, cardId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding card by id: " + cardId, e);
        }
        return Optional.empty();
    }

    @Override
    public ATMCard update(ATMCard card) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setString(1, hashPin(card.getPin()));
            ps.setString(2, card.getStatus().name());
            ps.setInt(3, card.getFailedAttempts());
            ps.setString(4, card.getAccountNumber());
            ps.setString(5, card.getCardId());
            ps.executeUpdate();
            return card;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating card: " + card.getCardId(), e);
        }
    }

    @Override
    protected ATMCard mapRow(ResultSet rs) throws SQLException {
        CardStatus status = CardStatus.valueOf(rs.getString("status"));
        return new ATMCard(
                rs.getString("card_id"),
                rs.getString("pin_hash"),
                rs.getString("linked_account_id"),
                ATMCard.stateFrom(status),
                rs.getInt("failed_attempts"));
    }

    @Override
    protected String getTableName() {
        return "atm_cards";
    }

    @Override
    protected String getIdColumnName() {
        return "card_id";
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, ATMCard entity) throws SQLException {
        ps.setString(1, entity.getCardId());
        ps.setString(2, hashPin(entity.getPin()));
        ps.setString(3, entity.getStatus().name());
        ps.setInt(4, entity.getFailedAttempts());
        ps.setString(5, entity.getAccountNumber());
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, ATMCard entity) throws SQLException {
        ps.setString(1, hashPin(entity.getPin()));
        ps.setString(2, entity.getStatus().name());
        ps.setInt(3, entity.getFailedAttempts());
        ps.setString(4, entity.getAccountNumber());
        ps.setString(5, entity.getCardId());
    }

    @Override
    protected String getInsertSQL() {
        return "INSERT INTO atm_cards (card_id, pin_hash, status, failed_attempts, linked_account_id) VALUES (?, ?, ?, ?, ?)";
    }

    @Override
    protected String getUpdateSQL() {
        return "UPDATE atm_cards SET pin_hash = ?, status = ?, failed_attempts = ?, linked_account_id = ? WHERE card_id = ?";
    }

    private String hashPin(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
