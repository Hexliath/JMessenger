package services;

import entities.User;
import entities.api.ChannelUserApiBody;
import entities.api.UserApiBody;
import entities.enums.EnumChannelRole;
import entities.enums.EnumCustomErrorCode;
import exceptions.ProcessingException;
import repositories.ChannelRepository;
import repositories.ChannelUserRepository;
import repositories.UserRepository;
import system.DatabaseConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class RoleService extends GenericService {

    private final ChannelUserRepository channelUserRepository;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;

    public RoleService(DatabaseConnection databaseConnection) {
        super(databaseConnection);
        this.channelUserRepository = new ChannelUserRepository(databaseConnection.get());
        this.channelRepository = new ChannelRepository(databaseConnection.get());
        this.userRepository = new UserRepository(databaseConnection.get());
    }

    public ChannelUserApiBody setUserRole(ChannelUserApiBody body, User sourceUser, UUID channelUid) throws SQLException, ProcessingException {

        // Check desired role
        EnumChannelRole targetDesiredRole = body.getRole();
        if (targetDesiredRole == null) {
            throw new ProcessingException(EnumCustomErrorCode.PARAMETER_REQUIRED, "You need to specify desired user role");
        } else if(targetDesiredRole.equals(EnumChannelRole.OWNER)) {
            throw new ProcessingException(EnumCustomErrorCode.PRIVILEGE_CONFLICT, "To change channel owner, edit the channel");
        }


        // Check if channel exist
        ResultSet rsChannel = channelRepository.getChannelById(channelUid);
        if(!rsChannel.next()) {
            throw new ProcessingException(EnumCustomErrorCode.ELEMENT_NOT_FOUND, "No Channel found for this id");
        }

        // Get source channel privileges
        EnumChannelRole sourceRole;
        ResultSet rsChannelUser = channelUserRepository.getUserChannelDetails(sourceUser.getId(), channelUid);
        if(rsChannelUser.next()) {
            sourceRole = EnumChannelRole.valueOf(rsChannelUser.getString("user_role"));
            if(!sourceRole.equals(EnumChannelRole.OWNER)) {
                throw new ProcessingException(EnumCustomErrorCode.NOT_ENOUGH_PRIVILEGES, "You don't have enough privileges to manage roles on this channel");
            }
        } else {
            throw new ProcessingException(EnumCustomErrorCode.NOT_ENOUGH_PRIVILEGES, "You don't have enough privileges to manage roles on this channel");
        }

        // Check if user is trying to change his own role
        if(body.getUser().getId().equals(sourceUser.getId())) {
            throw new ProcessingException(EnumCustomErrorCode.PRIVILEGE_CONFLICT, "You are the owner of this channel, you can't change your role, edit the channel to set a new owner.");
        }

        // Check if user exist
        UserApiBody targetUser;
        ResultSet rsTargetUser = userRepository.getUserByUid(body.getUser().getId());
        if(!rsTargetUser.next()) {
            throw new ProcessingException(EnumCustomErrorCode.USER_NOT_FOUND, "Specified user does not exist");
        } else {
            targetUser = new UserApiBody(
                    body.getUser().getId(),
                    rsTargetUser.getString("login"),
                    rsTargetUser.getString("display_name")
            );
        }

        // Check if target user already have some rights
        ResultSet rsTargetChannelUser = channelUserRepository.getUserChannelDetails(body.getUser().getId(), channelUid);
        if(!rsTargetChannelUser.next()){
            throw new ProcessingException(EnumCustomErrorCode.NEED_TO_JOIN_CHANNEL, "Target user must join the channel before you can change his role");
        } else {
            if(EnumChannelRole.valueOf(rsTargetChannelUser.getString("user_role")).equals(EnumChannelRole.NONE)) {
                throw new ProcessingException(EnumCustomErrorCode.NEED_TO_JOIN_CHANNEL, "Target user must join the channel before you can change his role");
            } else {
                channelUserRepository.updateChannelRole(body.getUser().getId(), channelUid, body.getRole());
            }
        }

        return new ChannelUserApiBody(
                targetUser,
                body.getRole()
        );
    }

}
