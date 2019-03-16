package repositories;

import java.sql.*;
import java.util.Date;
import java.util.UUID;

public class MessageRepository extends GenericRepository {

    public MessageRepository(Connection connection) {
        super(connection);
    }

    public void saveNewMessage(UUID messageId, UUID channelId, UUID userId, Date creationTime, String content) throws SQLException {
        saveNewMessage(messageId, channelId, userId, creationTime, content, null);
    }

    public void saveNewMessage(UUID messageId, UUID channelId, UUID userId, Date creationTime, String content, UUID attachment) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("INSERT INTO messages VALUES (?,?,?,?,?,?)");
        stmt.setObject(1, messageId, Types.OTHER);
        stmt.setObject(2, channelId, Types.OTHER);
        stmt.setObject(3, userId, Types.OTHER);
        stmt.setTimestamp(4, new java.sql.Timestamp(creationTime.getTime()));
        stmt.setString(5, content);
        stmt.setObject(6, attachment, Types.OTHER);
        stmt.execute();
    }

    public ResultSet getMessagesByChannelAndDateAfter(UUID channelId, Date creationTime) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM messages WHERE channel_id = ? AND creation_time >= ? ORDER BY creation_time ASC");
        stmt.setObject(1, channelId, Types.OTHER);
        stmt.setTimestamp(2, new java.sql.Timestamp(creationTime.getTime()));
        return stmt.executeQuery();
    }

}
