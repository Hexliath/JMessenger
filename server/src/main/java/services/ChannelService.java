package services;

import entities.User;
import entities.api.ChannelApiBody;
import entities.api.ChannelUserApiBody;
import entities.api.UserApiBody;
import entities.enums.EnumChannelRole;
import entities.enums.EnumChannelType;
import entities.enums.EnumCustomErrorCode;
import exceptions.ProcessingException;
import repositories.ChannelRepository;
import repositories.ChannelUserRepository;
import repositories.UserRepository;
import system.DatabaseConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

public class ChannelService extends GenericService {

    private final ChannelRepository channelRepository;
    private final ChannelUserRepository channelUserRepository;
    private final UserRepository userRepository;

    public ChannelService(DatabaseConnection databaseConnection) {
        super(databaseConnection);
        this.channelRepository = new ChannelRepository(databaseConnection.get());
        this.channelUserRepository = new ChannelUserRepository(databaseConnection.get());
        this.userRepository = new UserRepository(databaseConnection.get());
    }

    public ChannelApiBody createChannel(ChannelApiBody channelApiBody, User sourceUser) throws ProcessingException, SQLException {

        // Check if a channel with that name already exist
        ResultSet rsChannel = channelRepository.getChannelByName(channelApiBody.getName());
        if(rsChannel.next()) {
            throw new ProcessingException(EnumCustomErrorCode.UNAVAILABLE_CHANNEL_NAME,"A channel with this name already exist");
        }


        // Set an empty user list in no list provided
        if (channelApiBody.getMembers() == null ) {
            channelApiBody.setMembers(new ArrayList<>());
        }

        // Count members specified
        int membersCount = channelApiBody.getMembers().size();

        // Set channel type is not specified
        if(channelApiBody.getType() == null) {
            switch (membersCount) {
                case 0:
                    channelApiBody.setType(EnumChannelType.PUBLIC);
                    break;
                case 1:
                    channelApiBody.setType(EnumChannelType.PRIVATE);
                    break;
                default:
                    channelApiBody.setType(EnumChannelType.GROUP);
            }
        }

        // Check every declared member
        for (int i = channelApiBody.getMembers().size()-1; i >= 0 ; i--) {
            ChannelUserApiBody channelUser = channelApiBody.getMembers().get(i);
            // Check if such userId exist, remove from list if not
            try {
                ResultSet rsUser = userRepository.getUserByUid(channelUser.getUser().getId());
                if(rsUser.next()) {
                    channelUser.getUser().setLogin(rsUser.getString("login"));
                    channelUser.getUser().setDisplayName(rsUser.getString("display_name"));
                } else {
                    channelApiBody.getMembers().remove(i);
                    continue;
                }
            } catch (SQLException e) {
                log.error(e);
            }
            // Check if user is owner, remove from list if so
            if(channelUser.getUser().getId().equals(sourceUser.getId())) {
                channelApiBody.getMembers().remove(i);
            }
            // Check if user have a role, set to MEMBER if not
            if(channelUser.getRole() == null) {
                channelUser.setRole(EnumChannelRole.MEMBER);
            }
        }

        // Create a new uid for channel
        UUID newUid = UUID.randomUUID();

        // Create new channel
        channelRepository.saveNewChannel(newUid, channelApiBody.getName(), channelApiBody.getType(), Date.from(Instant.now()));


        // Append owner
        Date ownerJoinTime = Date.from(Instant.now());
        channelUserRepository.saveNewRelation(sourceUser.getId(), newUid, EnumChannelRole.OWNER, ownerJoinTime);


        // Append users
        for (ChannelUserApiBody channelUser: channelApiBody.getMembers()) {
            channelUserRepository.saveNewRelation(channelUser.getUser().getId(), newUid, channelUser.getRole(), Date.from(Instant.now()));
        }

        return new ChannelApiBody(
                newUid,
                channelApiBody.getName(),
                channelApiBody.getType(),
                channelApiBody.getMembers(),
                new UserApiBody(
                        sourceUser.getId(),
                        sourceUser.getLogin(), sourceUser.getDisplayName()
                ),
                ownerJoinTime
        );
    }

    public ChannelApiBody updateChannel(ChannelApiBody channelApiBody, UUID channelUid, User sourceUser) throws SQLException, ProcessingException {

        // Get source channel privileges
        EnumChannelRole sourceRole;
        Date sourceJoinDate;
        ResultSet rsChannelUser = channelUserRepository.getUserChannelDetails(sourceUser.getId(), channelUid);
        if(rsChannelUser.next()) {
            sourceRole = EnumChannelRole.valueOf(rsChannelUser.getString("user_role"));
            sourceJoinDate = new Date(rsChannelUser.getTimestamp("join_time").getTime());
        } else {
            throw new ProcessingException(EnumCustomErrorCode.NOT_ENOUGH_PRIVILEGES, "You don't have enough privileges to edit this channel");
        }

        // Check if channel exist
        ResultSet rsChannel = channelRepository.getChannelById(channelUid);
        if(!rsChannel.next()) {
            throw new ProcessingException(EnumCustomErrorCode.ELEMENT_NOT_FOUND, "No channel found for this channel id");
        }

        // Update name if present
        if(channelApiBody.getName() != null) {
            if(sourceRole.equals(EnumChannelRole.OWNER) || sourceRole.equals(EnumChannelRole.ADMIN)) {
                if(channelApiBody.getName().length() > 3 && channelApiBody.getName().length() < 64){
                    channelRepository.updateChannelName(channelUid, channelApiBody.getName());
                } else {
                    throw new ProcessingException(EnumCustomErrorCode.WRONG_LENGTH, "New channel name is too short or too long");
                }
            } else {
                throw new ProcessingException(EnumCustomErrorCode.NOT_ENOUGH_PRIVILEGES, "You don't have enough privileges to edit this channel name");
            }
        }

        // Update owner if present
        if(channelApiBody.getOwner() != null) {
            if(sourceRole.equals(EnumChannelRole.OWNER)) {
                if(userRepository.getUserByUid(channelApiBody.getOwner().getId()).next()){
                    channelUserRepository.updateChannelOwner(channelUid, sourceUser.getId(), channelApiBody.getOwner().getId());
                } else {
                    throw new ProcessingException(EnumCustomErrorCode.WRONG_LENGTH, "Required new owner does not exist");
                }
            } else {
                throw new ProcessingException(EnumCustomErrorCode.NOT_ENOUGH_PRIVILEGES, "You don't have enough privileges to edit this channel name");
            }
        }

        return getChannelById(channelUid, sourceJoinDate);
    }

    public List<ChannelApiBody> getAllChannelsForUser(User sourceUser) throws SQLException {
        Map<String, ChannelApiBody> channels = new HashMap<>();

        ResultSet rsChannels = channelRepository.getAllAllowedChannels(sourceUser.getId());
        while(rsChannels.next()) {
            // Get channel id
            UUID channelId = UUID.fromString(rsChannels.getString("channel_id"));
            // Add channel to map if not in already
            if(!channels.containsKey(channelId.toString())) {
                channels.put(channelId.toString(), new ChannelApiBody(
                        UUID.fromString(rsChannels.getString("channel_id")),
                        rsChannels.getString("channel_name"),
                        EnumChannelType.valueOf(rsChannels.getString("channel_type")),
                        new ArrayList<>(),
                        null,
                        new Date(rsChannels.getTimestamp("join_time").getTime())
                ));
            }

            // Get info about user from current line
            EnumChannelRole userRole = EnumChannelRole.valueOf(rsChannels.getString("user_role"));
            UserApiBody user = new UserApiBody(
                    UUID.fromString(rsChannels.getString("user_id")),
                    rsChannels.getString("login"),
                    rsChannels.getString("display_name")
            );
            ChannelUserApiBody channelUser = new ChannelUserApiBody(user, userRole);

            // Add to members list or to owner field
            switch (userRole){
                case NONE:
                    break;
                case ADMIN:
                case MEMBER:
                    channels.get(channelId.toString()).getMembers().add(channelUser);
                    break;
                case OWNER:
                    channels.get(channelId.toString()).setOwner(channelUser.getUser());
            }
        }

        // Convert channels map to list
        List<ChannelApiBody> output = new ArrayList<>(channels.values());

        // For every channel check if source user has already joined it
        for (ChannelApiBody channel: output) {
            if(doUserListContainUserId(channel.getMembers(), sourceUser.getId())) {
                channel.setJoined(true);
            } else {
                channel.setJoined(false);
            }
        }

        return output;
    }

    public ChannelApiBody getChannelById(UUID channelId, Date sourceJoinDate) throws SQLException {
        // Get new updated channel
        ResultSet rsUpdatedChannel = channelRepository.getChannelById(channelId);
        rsUpdatedChannel.next();
        ChannelApiBody updatedChannel = new ChannelApiBody(
                UUID.fromString(rsUpdatedChannel.getString("id")),
                rsUpdatedChannel.getString("channel_name"),
                EnumChannelType.valueOf(rsUpdatedChannel.getString("channel_type")),
                new ArrayList<>(),
                null,
                sourceJoinDate
        );

        // Get all users for channel
        List<ChannelUserApiBody> channelUsers = new ArrayList<>();
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

            if(channelUser.getRole().equals(EnumChannelRole.OWNER)) {
                updatedChannel.setOwner(channelUser.getUser());
            } else {
                channelUsers.add(channelUser);
            }
        }

        // Add users to chanel body
        updatedChannel.setMembers(channelUsers);

        return updatedChannel;
    }

    private boolean doUserListContainUserId(List<ChannelUserApiBody> users, UUID userId) {
        for (ChannelUserApiBody user: users) {
            if(user.getUser().getId().equals(userId)) {
                return true;
            }
        }
        return false;
    }

}
