package entities.api;

import java.util.Date;
import java.util.UUID;

public class MessageApiBody extends GenericApiEntity {

    private UUID id;
    private String content;
    private UserApiBody author;
    private UUID channelId;
    private Date creationTime;
    private UUID attachment;

    public MessageApiBody() {
    }

    public MessageApiBody(UUID id, String content, UserApiBody author, UUID channelId, Date creationTime, UUID attachment) {
        this.id = id;
        this.content = content;
        this.author = author;
        this.channelId = channelId;
        this.creationTime = creationTime;
        this.attachment = attachment;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public UserApiBody getAuthor() {
        return author;
    }

    public void setAuthor(UserApiBody author) {
        this.author = author;
    }

    public UUID getChannelId() {
        return channelId;
    }

    public void setChannelId(UUID channelId) {
        this.channelId = channelId;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public UUID getAttachment() {
        return attachment;
    }

    public void setAttachment(UUID attachment) {
        this.attachment = attachment;
    }
}
