package com.training.atm.repository.db;

import com.training.atm.config.db.ConnectionManager;
import com.training.atm.model.ATMCard;
import com.training.atm.model.enums.CardStatus;
import com.training.atm.repository.CardRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * H2 (JDBC) implementation of {@link CardRepository}.
 *
 * <p>Mirrors {@code cards.txt} (cardId|pin|accountNumber|status|failedAttempts)
 * mapped onto the {@code cards} table.  State pattern preserved: the persisted
 * {@link CardStatus} value is only a serialisation token; {@code mapRow} uses
 * {@link ATMCard#stateFrom(CardStatus)} to rebuild the runtime
 * {@link com.training.atm.model.state.CardState} object.
 */
public class JdbcCardRepository extends AbstractJdbcRepository<ATMCard, String> implements CardRepository {

    private static final String SELECT_BY_ID =
            "SELECT card_id, pin, account_number, status, failed_attempts"
                    + " FROM cards WHERE card_id = ?";

    private static final String UPDATE =
            "UPDATE cards SET pin = ?, account_number = ?, status = ?, failed_attempts = ?"
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
            ps.setString(1, card.getPin());
            ps.setString(2, card.getAccountNumber());
            ps.setString(3, card.getStatus().name());
            ps.setInt(4, card.getFailedAttempts());
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
                rs.getString("pin"),
                rs.getString("account_number"),
                ATMCard.stateFrom(status),   // State pattern: reconstruct state object
                rs.getInt("failed_attempts"));
    }

    @Override
    protected String getTableName() {
        return "cards";
    }

    @Override
    protected String getIdColumnName() {
        return "card_id";
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, ATMCard entity) throws SQLException {
        ps.setString(1, entity.getCardId());
        ps.setString(2, entity.getPin());
        ps.setString(3, entity.getAccountNumber());
        ps.setString(4, entity.getStatus().name());
        ps.setInt(5, entity.getFailedAttempts());
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, ATMCard entity) throws SQLException {
        ps.setString(1, entity.getPin());
        ps.setString(2, entity.getAccountNumber());
        ps.setString(3, entity.getStatus().name());
        ps.setInt(4, entity.getFailedAttempts());
        ps.setString(5, entity.getCardId());
    }

    @Override
    protected String getInsertSQL() {
        return "INSERT INTO cards (card_id, pin, account_number, status, failed_attempts) VALUES (?, ?, ?, ?, ?)";
    }

    @Override
    protected String getUpdateSQL() {
        return "UPDATE cards SET pin = ?, account_number = ?, status = ?, failed_attempts = ? WHERE card_id = ?";
    }
}
