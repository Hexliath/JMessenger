package services;

import entities.User;
import entities.api.MessageApiBody;
import entities.api.UserApiBody;
import entities.enums.EnumChannelRole;
import entities.enums.EnumCustomErrorCode;
import exceptions.ProcessingException;
import repositories.ChannelUserRepository;
import repositories.MessageRepository;
import system.DatabaseConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class MessageService extends GenericService {

    private final MessageRepository messageRepository;
    private final ChannelUserRepository channelUserRepository;
    public static int MAX_MESSAGE_LENGTH = 500;

    public MessageService(DatabaseConnection databaseConnection) {
        super(databaseConnection);
        this.messageRepository = new MessageRepository(databaseConnection.get());
        this.channelUserRepository = new ChannelUserRepository(databaseConnection.get());
    }

    public MessageApiBody sendMessage(MessageApiBody messageApiBody, User sourceUser) throws ProcessingException, SQLException {

        if(messageApiBody.getContent().length() > MAX_MESSAGE_LENGTH) {
            throw new ProcessingException(EnumCustomErrorCode.MESSAGE_TOO_LONG, "You message exceeds the maximum length");
        }

        ResultSet rsChannelUser;

        rsChannelUser = channelUserRepository.getUserChannelDetails(sourceUser.getId(), messageApiBody.getChannelId());
        if(rsChannelUser.next()) {
            EnumChannelRole role = EnumChannelRole.valueOf(rsChannelUser.getString("user_role"));
            if(role.equals(EnumChannelRole.NONE)) {
                throw new ProcessingException(EnumCustomErrorCode.NOT_ENOUGH_PRIVILEGES, "You don't have required privileges to send messages on this channel");
            }
        } else {
            throw new ProcessingException(EnumCustomErrorCode.NOT_ENOUGH_PRIVILEGES, "You don't have required privileges to send messages on this channel");
        }


        UUID newUid = UUID.randomUUID();
        Date creationTime = Date.from(Instant.now());

        messageRepository.saveNewMessage(newUid, messageApiBody.getChannelId(), sourceUser.getId(), creationTime, messageApiBody.getContent());

        messageApiBody.setId(newUid);
        messageApiBody.setAuthor(new UserApiBody(sourceUser.getId(), sourceUser.getLogin(), sourceUser.getDisplayName()));
        messageApiBody.setCreationTime(creationTime);

        return messageApiBody;
    }

}
