package repositories;

import entities.enums.EnumChannelType;

import java.sql.*;
import java.util.Date;
import java.util.UUID;

public class ChannelRepository extends GenericRepository {

    public ChannelRepository(Connection connection) {
        super(connection);
    }

    public ResultSet getAllAllowedChannels(UUID userId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM channels " +
                        "INNER JOIN channel_users on channels.id = channel_users.channel_id " +
                        "INNER JOIN users on channel_users.user_id = users.id " +
                        "WHERE ( channel_users.channel_id IN " +
                            "(select channel_id from channel_users as cuu where cuu.user_id = ?) " +
                            "and channel_users.user_role != 'NONE') OR channels.channel_type = 'PUBLIC'");
        stmt.setObject(1, userId, Types.OTHER);
        return stmt.executeQuery();
    }

    public void saveNewChannel(UUID newId, String name, EnumChannelType type, Date creationTime) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("INSERT INTO channels VALUES (?, ?, ?, ?)");
        stmt.setObject(1, newId, java.sql.Types.OTHER);
        stmt.setString(2, name);
        stmt.setString(3, type.toString());
        stmt.setTimestamp(4, new java.sql.Timestamp(creationTime.getTime()));
        stmt.execute();
    }

    public ResultSet getChannelByName(String name) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM channels WHERE channel_name = ?");
        stmt.setString(1, name);
        return stmt.executeQuery();
    }

    public ResultSet getChannelById(UUID channelId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM channels WHERE id = ?");
        stmt.setObject(1, channelId, Types.OTHER);
        return stmt.executeQuery();
    }

    public void updateChannelName(UUID channelId, String name) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("UPDATE channels SET channel_name = ? WHERE id = ?");
        stmt.setString(1, name);
        stmt.setObject(2, channelId, java.sql.Types.OTHER);
        stmt.execute();
    }

}
