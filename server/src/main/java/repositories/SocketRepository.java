package repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class SocketRepository extends GenericRepository {

    public SocketRepository(Connection connection) {
        super(connection);
    }

    public void saveNewSocket(String userToken, UUID socketId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("INSERT INTO sockets VALUES (?, ?)");
        stmt.setString(1, userToken);
        stmt.setObject(2, socketId, java.sql.Types.OTHER);
        stmt.execute();
    }

    public ResultSet getSocketByToken(String userToken) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM sockets WHERE user_token = ?");
        stmt.setString(1, userToken);
        return stmt.executeQuery();
    }

    public void removeSocket(UUID socketId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("DELETE FROM sockets WHERE socket_id = ?");
        stmt.setObject(1, socketId, java.sql.Types.OTHER);
        stmt.execute();
    }

    public void removeAllSockets() throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("DELETE FROM sockets");
        stmt.execute();
    }

    public ResultSet getAllOnlineUsers() throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM sockets INNER JOIN auth ON auth.auth_token = sockets.user_token INNER JOIN users ON auth.user_id = users.id ");
        return stmt.executeQuery();
    }



}
