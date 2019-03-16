package services;

import entities.User;
import entities.api.ComplexArchiveQueryApiBody;
import entities.api.MessageApiBody;
import entities.api.UserApiBody;
import entities.enums.EnumChannelRole;
import entities.enums.EnumCustomErrorCode;
import exceptions.ProcessingException;
import repositories.MessageRepository;
import system.DatabaseConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ArchiveService extends GenericService{

    private final MessageRepository messageRepository;
    private Map<UUID, UserApiBody> knownAuthors;

    public ArchiveService(DatabaseConnection databaseConnection) {
        super(databaseConnection);
        this.messageRepository = new MessageRepository(databaseConnection.get());
        this.knownAuthors = new HashMap<>();
    }

    public Map<UUID, List<MessageApiBody>> loadMessages(ComplexArchiveQueryApiBody body, User sourceUser) throws SQLException, ProcessingException {
        // Make sure list exist
        List<UUID> channelIdList = body.getChannels();
        if(channelIdList == null) {
            channelIdList = new ArrayList<>();
        }
        // Make sure date exist
        Date since = body.getSince();
        if(since == null) {
            since = Date.from(Instant.now().minus(30, ChronoUnit.MINUTES));
        }

        // Output map
        Map<UUID, List<MessageApiBody>> output = new HashMap<>();

        // Get history for every channel in list
        for (UUID channelId: channelIdList) {
            output.put(channelId, getChannelHistory(sourceUser.getId(), channelId, since));
        }

        return output;
    }

    public List<MessageApiBody> loadChannelHistory(UUID channelId, Date limitDate, User sourceUser) throws SQLException, ProcessingException {
        return getChannelHistory(sourceUser.getId(), channelId, limitDate);
    }

    public String getExportXml(UUID channelId, User sourceUser) throws SQLException, ProcessingException {
        if(!hasUserRoleInChannel(sourceUser.getId(), channelId, EnumChannelRole.ADMIN, EnumChannelRole.OWNER)) {
            throw new ProcessingException(EnumCustomErrorCode.NOT_ENOUGH_PRIVILEGES, "You are not allowed to export channel history");
        }

        if(!isChannel(channelId)) {
            throw new ProcessingException(EnumCustomErrorCode.ELEMENT_NOT_FOUND, "Channel not found");
        }

        List<MessageApiBody> messages = loadChannelHistory(channelId, Date.from(Instant.ofEpochMilli(1L)), sourceUser);

        StringBuilder builder = new StringBuilder();
        builder.append("<archive>\n");
        for (MessageApiBody message: messages) {
            builder.append("\t<message>\n");
            builder.append("\t\t<user>").append(message.getAuthor().getDisplayName()).append("</user>\n");
            builder.append("\t\t<content>").append(message.getContent()).append("</content>\n");
            builder.append("\t\t<time>").append(message.getCreationTime().getTime()).append("</time>\n");
            builder.append("\t</message>\n");
        }
        builder.append("</archive>");

        return builder.toString();
    }

    private List<MessageApiBody> getChannelHistory(UUID userId, UUID channelId, Date since) throws SQLException, ProcessingException {
        // Check if user has rights
        if( !hasUserRoleInChannel(userId, channelId, EnumChannelRole.OWNER, EnumChannelRole.ADMIN, EnumChannelRole.MEMBER) ) {
            return new ArrayList<>();
        }
        ResultSet rsChannelMessages = messageRepository.getMessagesByChannelAndDateAfter(channelId, since);
        List<MessageApiBody> channelMessages = new ArrayList<>();
        while (rsChannelMessages.next()) {
            // Get author and load details il not already loaded
            UUID authorId = UUID.fromString(rsChannelMessages.getString("user_id"));
            if(!this.knownAuthors.containsKey(authorId)) {
                this.knownAuthors.put(authorId, getUserApiBody(authorId));
            }
            // Get attachment
            UUID attachment = null;
            String attachmentString = rsChannelMessages.getString("attachment_id");
            if(attachmentString != null) {
                attachment = UUID.fromString(attachmentString);
            }

            // Add new line
            channelMessages.add(new MessageApiBody(
                    UUID.fromString(rsChannelMessages.getString("id")),
                    rsChannelMessages.getString("content"),
                    this.knownAuthors.get(authorId),
                    UUID.fromString(rsChannelMessages.getString("channel_id")),
                    new Date(rsChannelMessages.getTimestamp("creation_time").getTime()),
                    attachment
            ));
        }
        return channelMessages;
    }
}
