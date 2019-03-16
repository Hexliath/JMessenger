package repositories;

import entities.enums.EnumChannelRole;

import java.sql.*;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class ChannelUserRepository extends GenericRepository {

    public ChannelUserRepository(Connection connection) {
        super(connection);
    }

    public ResultSet getUserChannelDetails(UUID userId, UUID channelId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM channel_users WHERE user_id = ? AND channel_id = ?");
        stmt.setObject(1, userId, Types.OTHER);
        stmt.setObject(2, channelId, Types.OTHER);
        return stmt.executeQuery();
    }

    public ResultSet getAllChannelUsers(UUID channelId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM channel_users INNER JOIN users ON users.id = channel_users.user_id WHERE channel_users.channel_id = ?");
        stmt.setObject(1, channelId, Types.OTHER);
        return stmt.executeQuery();
    }

    public void saveNewRelation(UUID userId, UUID channelId, EnumChannelRole role, Date joinTime) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("INSERT INTO channel_users VALUES (?,?,?,?)");
        stmt.setObject(1, userId, Types.OTHER);
        stmt.setObject(2, channelId, Types.OTHER);
        stmt.setString(3, role.toString());
        stmt.setTimestamp(4, new java.sql.Timestamp(joinTime.getTime()));
        stmt.execute();
    }

    public void updateChannelRole(UUID userId, UUID channelId, EnumChannelRole role) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("UPDATE channel_users SET user_role = ? WHERE user_id = ? AND channel_id = ?");
        stmt.setString(1, role.toString());
        stmt.setObject(2, userId, Types.OTHER);
        stmt.setObject(3, channelId, Types.OTHER);
        stmt.execute();
    }

    public void updateChannelOwner(UUID channelId, UUID oldOwnerId, UUID newOwnerId) throws SQLException {
        // Remove old owner
        PreparedStatement stmt = connection.prepareStatement("UPDATE channel_users SET user_role = ? WHERE user_id = ? AND channel_id = ?");
        stmt.setString(1, EnumChannelRole.MEMBER.toString());
        stmt.setObject(2, oldOwnerId, java.sql.Types.OTHER);
        stmt.setObject(3, channelId, java.sql.Types.OTHER);
        stmt.execute();

        // Check if new owner already have some channel rights
        stmt = connection.prepareStatement("SELECT * FROM channel_users WHERE user_id = ? AND channel_id = ?");
        stmt.setObject(1, newOwnerId, java.sql.Types.OTHER);
        stmt.setObject(2, channelId, java.sql.Types.OTHER);

        // Add or update new owner role
        if(!stmt.executeQuery().next()) {
            // Add new role
            saveNewRelation(newOwnerId, channelId, EnumChannelRole.OWNER, Date.from(Instant.now()));
        } else {
            // Update new owner role
            stmt = connection.prepareStatement("UPDATE channel_users SET user_role = ? WHERE user_id = ? AND channel_id = ?");
            stmt.setString(1, EnumChannelRole.OWNER.toString());
            stmt.setObject(2, newOwnerId, java.sql.Types.OTHER);
            stmt.setObject(3, channelId, java.sql.Types.OTHER);
            stmt.execute();
        }

    }

    public void removeRelation(UUID userId, UUID channelId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("DELETE FROM channel_users WHERE user_id = ? AND channel_id = ?");
        stmt.setObject(1, userId, Types.OTHER);
        stmt.setObject(2, channelId, Types.OTHER);
        stmt.execute();
    }

    public ResultSet getAllChannelUserTokensWherePushActiveAndNotSource(UUID channelId, String sourceToken) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT auth.auth_token FROM auth INNER JOIN channel_users ON auth.user_id = channel_users.user_id INNER JOIN sockets ON auth.auth_token = sockets.user_token WHERE channel_users.channel_id = ? AND auth.auth_token != ?");
        stmt.setObject(1, channelId, Types.OTHER);
        stmt.setString(2, sourceToken);
        return stmt.executeQuery();

    }

}
