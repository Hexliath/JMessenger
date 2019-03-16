package repositories;

import java.sql.*;
import java.util.UUID;

public class UserRepository extends GenericRepository {

    public UserRepository(Connection connection) {
        super(connection);
    }

    public ResultSet getUserByLogin(String login) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM users WHERE login = ?");
        stmt.setString(1, login);
        return stmt.executeQuery();
    }

    public ResultSet getUserByUid(UUID userId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM users WHERE id = ?");
        stmt.setObject(1, userId, Types.OTHER);
        return stmt.executeQuery();
    }

    public void saveNewUser(UUID userId, String login, String passwordHash, Date birthDate, String displayName, String avatarUrl) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("INSERT INTO users VALUES (?,?,?,?,?,?)");
        stmt.setObject(1, userId, Types.OTHER);
        stmt.setString(2, login);
        stmt.setString(3, passwordHash);
        stmt.setDate(4, birthDate);
        stmt.setString(5, displayName);
        stmt.setString(6, avatarUrl);
        stmt.execute();
    }

    public ResultSet getAllUsers() throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM users");
        return stmt.executeQuery();
    }

    public ResultSet searchUsersByName(String name) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM users WHERE LOWER(login) LIKE ? OR LOWER(display_name) LIKE ?");
        stmt.setString(1, "%"+name.toLowerCase()+"%");
        stmt.setString(2, "%"+name.toLowerCase()+"%");
        return stmt.executeQuery();
    }

    public ResultSet getUserByDisplayName(String displayName) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM users WHERE LOWER(display_name) = ?");
        stmt.setString(1, displayName.toLowerCase());
        return stmt.executeQuery();
    }

    public void updateDisplayName(UUID userId, String displayName) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("UPDATE users SET display_name = ? WHERE id = ?");
        stmt.setString(1, displayName);
        stmt.setObject(2, userId, Types.OTHER);
        stmt.execute();
    }

}
