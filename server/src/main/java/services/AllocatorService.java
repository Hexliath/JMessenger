package services;

import entities.User;
import entities.api.ChannelUserApiBody;
import entities.api.UserApiBody;
import entities.enums.EnumChannelRole;
import entities.enums.EnumChannelType;
import entities.enums.EnumCustomErrorCode;
import exceptions.ProcessingException;
import repositories.ChannelUserRepository;
import system.DatabaseConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class AllocatorService extends GenericService {

    private final ChannelUserRepository channelUserRepository;

    public AllocatorService(DatabaseConnection databaseConnection) {
        super(databaseConnection);
        this.channelUserRepository = new ChannelUserRepository(databaseConnection.get());
    }

    public ChannelUserApiBody addUserToChannel(UserApiBody body, User sourceUser, UUID channelId) throws SQLException, ProcessingException {

        // Check source user rights
        if( !hasUserRoleInChannel(sourceUser.getId(), channelId, EnumChannelRole.OWNER, EnumChannelRole.ADMIN) ) {
            throw new ProcessingException(EnumCustomErrorCode.NOT_ENOUGH_PRIVILEGES, "You are not allowed to manage this channel");
        }

        // Check channel type
        if( !isChannelOfType(channelId, EnumChannelType.GROUP) ) {
            throw new ProcessingException(EnumCustomErrorCode.WRONG_CHANNEL_TYPE, "You can only add users to group channels");
        }

        // Get target user data
        UserApiBody targetUser = getUserApiBody(body.getId());

        // Check if user already in channel
        if( isUserChannelMember(body.getId(), channelId) ) {
            throw new ProcessingException(EnumCustomErrorCode.ALREADY_EXIST, "This user is already member of this channel");
        }

        // Add to channel
        channelUserRepository.saveNewRelation(body.getId(), channelId, EnumChannelRole.MEMBER, Date.from(Instant.now()));


        return new ChannelUserApiBody(
                targetUser,
                EnumChannelRole.MEMBER
        );
    }

    public void kickUserFromChannel(UserApiBody body, User sourceUser, UUID channelId) throws SQLException, ProcessingException {
        // Check source user rights
        if( !hasUserRoleInChannel(sourceUser.getId(), channelId, EnumChannelRole.OWNER, EnumChannelRole.ADMIN) ) {
            throw new ProcessingException(EnumCustomErrorCode.NOT_ENOUGH_PRIVILEGES, "You are not allowed to manage this channel");
        }

        // Check channel type
        if( !isChannelOfType(channelId, EnumChannelType.GROUP, EnumChannelType.PUBLIC) ) {
            throw new ProcessingException(EnumCustomErrorCode.WRONG_CHANNEL_TYPE, "You can only add users to group channels");
        }

        // Get target user data
        UserApiBody targetUser = getUserApiBody(body.getId());

        // Check if user in channel
        if( !isUserChannelMember(body.getId(), channelId) ) {
            throw new ProcessingException(EnumCustomErrorCode.NOT_A_MEMBER, "This user isn't member of this channel");
        }

        // Remove from channel
        channelUserRepository.removeRelation(body.getId(), channelId);
    }

    public ChannelUserApiBody getUserChannelInfo(UserApiBody body, User sourceUser, UUID channelId) throws SQLException, ProcessingException {
        // Check source user rights
        if( !hasUserRoleInChannel(sourceUser.getId(), channelId, EnumChannelRole.OWNER, EnumChannelRole.ADMIN, EnumChannelRole.MEMBER) ) {
            throw new ProcessingException(EnumCustomErrorCode.NOT_ENOUGH_PRIVILEGES, "You are not allowed to see this channel");
        }

        // Check if channel exist
        if( !isChannel(channelId) ) {
            throw new ProcessingException(EnumCustomErrorCode.ELEMENT_NOT_FOUND, "Channel not found");
        }

        // Check if user in channel
        if( !isUserChannelMember(body.getId(), channelId) ) {
            throw new ProcessingException(EnumCustomErrorCode.NOT_A_MEMBER, "This user isn't member of this channel");
        }

        // Get target user data
        UserApiBody targetUser = getUserApiBody(body.getId());
        EnumChannelRole targetUserRole = getUserChannelRole(body.getId(), channelId);

        return new ChannelUserApiBody(
                targetUser,
                targetUserRole
        );
    }

    public List<ChannelUserApiBody> getChannelUsers(User sourceUser, UUID channelId) throws SQLException, ProcessingException {
        // Check source user rights
        if( !hasUserRoleInChannel(sourceUser.getId(), channelId, EnumChannelRole.OWNER, EnumChannelRole.ADMIN, EnumChannelRole.MEMBER) ) {
            throw new ProcessingException(EnumCustomErrorCode.NOT_ENOUGH_PRIVILEGES, "You are not allowed to see this channel");
        }

        // Check if channel exist
        if( !isChannel(channelId) ) {
            throw new ProcessingException(EnumCustomErrorCode.ELEMENT_NOT_FOUND, "Channel not found");
        }

        // Get all users for channel
        List<ChannelUserApiBody> output = new ArrayList<>();
        ResultSet rsChannelUsers = channelUserRepository.getAllChannelUsers(channelId);
        while (rsChannelUsers.next()) {
            // Get info about user from current line
            EnumChannelRole userRole = EnumChannelRole.valueOf(rsChannelUsers.getString("user_role"));
            UserApiBody user = new UserApiBody(
                    UUID.fromString(rsChannelUsers.getString("user_id")),
                    rsChannelUsers.getString("login"),
                    rsChannelUsers.getString("display_name")
            );
            ChannelUserApiBody channelUser = new ChannelUserApiBody(user, userRole);
            output.add(channelUser);
        }

        return output;
    }

    public void joinChannel(User sourceUser, UUID channelId) throws SQLException, ProcessingException {
        // Check if source user in channel
        if( isUserChannelMember(sourceUser.getId(), channelId) ) {
            throw new ProcessingException(EnumCustomErrorCode.ALREADY_JOINED, "Your already are member of this channel");
        }

        // Check channel type
        if( !isChannelOfType(channelId, EnumChannelType.PUBLIC) ) {
            throw new ProcessingException(EnumCustomErrorCode.WRONG_CHANNEL_TYPE, "You can only join public channels");
        }

        // Add source user to channel
        channelUserRepository.saveNewRelation(sourceUser.getId(), channelId, EnumChannelRole.MEMBER, Date.from(Instant.now()));
    }

    public void leaveChannel(User sourceUser, UUID channelId) throws SQLException, ProcessingException {
        // Check if source user in channel
        if( !isUserChannelMember(sourceUser.getId(), channelId) ) {
            throw new ProcessingException(EnumCustomErrorCode.NOT_A_MEMBER, "Your aren't member of this channel");
        }

        // Check channel type
        if( !isChannelOfType(channelId, EnumChannelType.PUBLIC, EnumChannelType.GROUP) ) {
            throw new ProcessingException(EnumCustomErrorCode.WRONG_CHANNEL_TYPE, "You can't leave this channel");
        }

        // Check source user rights
        if( hasUserRoleInChannel(sourceUser.getId(), channelId, EnumChannelRole.OWNER) ) {
            throw new ProcessingException(EnumCustomErrorCode.PRIVILEGE_CONFLICT, "You are the owner of this channel. Edit channel to set a new owner before leaving");
        }

        // Delete source user relation
        channelUserRepository.removeRelation(sourceUser.getId(), channelId);
    }

}
