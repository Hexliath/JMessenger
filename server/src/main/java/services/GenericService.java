package services;

import entities.api.UserApiBody;
import entities.enums.EnumChannelRole;
import entities.enums.EnumChannelType;
import entities.enums.EnumCustomErrorCode;
import exceptions.ProcessingException;
import repositories.ChannelRepository;
import repositories.ChannelUserRepository;
import repositories.UserRepository;
import system.DatabaseConnection;
import system.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Abstract service class
 * Provides children with common services and methods to be used depending of needs.
 */
public abstract class GenericService {
    protected static final Logger log = new Logger(GenericService.class);

    private final ChannelRepository channelRepository;
    private final ChannelUserRepository channelUserRepository;
    private final UserRepository userRepository;

    private final DatabaseConnection databaseConnection;

    public GenericService(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.channelRepository = new ChannelRepository(databaseConnection.get());
        this.channelUserRepository = new ChannelUserRepository(databaseConnection.get());
        this.userRepository = new UserRepository(databaseConnection.get());
    }

    // Check if user has one of given roles in channel
    protected boolean hasUserRoleInChannel(UUID userId, UUID channelId, EnumChannelRole... roles) throws SQLException, ProcessingException {
        ResultSet rsSourceChannelUser = channelUserRepository.getUserChannelDetails(userId, channelId);
        EnumChannelRole userChannelRole;
        if(rsSourceChannelUser.next()) {
            userChannelRole = EnumChannelRole.valueOf(rsSourceChannelUser.getString("user_role"));
        } else {
            return false;
        }

        for (EnumChannelRole role: roles) {
            if(role.equals(userChannelRole)) {
                return true;
            }
        }

        return false;
    }

    // Check if channel is one of given types
    protected boolean isChannelOfType(UUID channelId, EnumChannelType... types) throws SQLException, ProcessingException {
        EnumChannelType targetChannelType;
        ResultSet rsTargetChannel = channelRepository.getChannelById(channelId);
        if(rsTargetChannel.next()) {
            targetChannelType = EnumChannelType.valueOf(rsTargetChannel.getString("channel_type"));
        } else {
            throw new ProcessingException(EnumCustomErrorCode.ELEMENT_NOT_FOUND, "Channel not found");
        }

        for (EnumChannelType type: types) {
            if(type.equals(targetChannelType)) {
                return true;
            }
        }

        return false;
    }

    // Get UserApiBody from userId
    protected UserApiBody getUserApiBody(UUID userId) throws SQLException, ProcessingException {
        UserApiBody targetUser;
        ResultSet rsTargetUser = userRepository.getUserByUid(userId);
        if(!rsTargetUser.next()) {
            throw new ProcessingException(EnumCustomErrorCode.USER_NOT_FOUND, "Target user not found");
        } else {
            return new UserApiBody(
                    userId,
                    rsTargetUser.getString("login"),
                    rsTargetUser.getString("display_name")
            );
        }
    }

    // Check if user user is member of a channel
    protected boolean isUserChannelMember(UUID userId, UUID channelId) throws SQLException {
        ResultSet rsTargetChannelUser = channelUserRepository.getUserChannelDetails(userId, channelId);
        return rsTargetChannelUser.next();
    }

    // Get user's role in given channel
    protected EnumChannelRole getUserChannelRole(UUID userId, UUID channelId) throws SQLException, ProcessingException {
        ResultSet rsTargetChannelUser = channelUserRepository.getUserChannelDetails(userId, channelId);
        if(rsTargetChannelUser.next()) {
            return EnumChannelRole.valueOf(rsTargetChannelUser.getString("user_role"));
        } else {
            throw new ProcessingException(EnumCustomErrorCode.NOT_A_MEMBER, "This user isn't member of this channel");
        }
    }

    // Check if a channel exist
    protected boolean isChannel(UUID channelId) throws SQLException {
        return channelRepository.getChannelById(channelId).next();
    }


}
