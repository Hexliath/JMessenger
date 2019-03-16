package repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

public class AuthorisationRepository extends GenericRepository {

    public AuthorisationRepository(Connection connection) {
        super(connection);
    }

    public ResultSet getAuthByToken(String token) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM auth WHERE auth_token = ?");
        stmt.setString(1, token);
        return stmt.executeQuery();
    }

    public ResultSet getAuthByLogin(String login) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM auth WHERE login = ?");
        stmt.setString(1, login);
        return stmt.executeQuery();
    }

    public void saveNewRecord(UUID userId, String authToken, Date validity, String userIp) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("INSERT INTO auth VALUES (?, ?, ?, ?)");
        stmt.setObject(1, userId, java.sql.Types.OTHER);
        stmt.setString(2, authToken);
        stmt.setTimestamp(3, new java.sql.Timestamp(validity.getTime()));
        stmt.setString(4, userIp);
        stmt.execute();
    }

    public void removeUserExpiredTokens(UUID userId, Date now) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("DELETE FROM auth WHERE user_id = ? AND validity <= ?");
        stmt.setObject(1, userId, java.sql.Types.OTHER);
        stmt.setTimestamp(2, new java.sql.Timestamp(now.getTime()));
        stmt.execute();
    }

    public boolean isTokenInFreeToUse(String token) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT 1 FROM auth WHERE auth_token = ?");
        stmt.setString(1, token);
        ResultSet rs = stmt.executeQuery();
        return !rs.next();

    }

    public int getOpenConnectionsCount(UUID userId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT 1 FROM auth WHERE user_id = ?");
        stmt.setObject(1, userId, java.sql.Types.OTHER);
        ResultSet rs = stmt.executeQuery();
        return getRowCount(rs);
    }

    public void destroyToken(String token) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("DELETE FROM auth WHERE auth_token = ?");
        stmt.setString(1, token);
        stmt.execute();
    }

    public void destroyAllUserTokens(UUID userId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("DELETE FROM auth WHERE user_id = ?");
        stmt.setObject(1, userId, java.sql.Types.OTHER);
        stmt.execute();
    }

    public void renewTokenValidity(String token, Date validity) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("UPDATE auth SET validity = ? WHERE auth_token = ?");
        stmt.setTimestamp(1, new java.sql.Timestamp(validity.getTime()));
        stmt.setString(2, token);
        stmt.execute();
    }

}
