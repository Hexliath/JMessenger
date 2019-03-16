package entities.api;

import entities.enums.EnumPushNotificationType;

import java.util.UUID;

public class PushNotificationApiBody extends GenericApiEntity {

    private EnumPushNotificationType type;
    private UUID channelId;
    private GenericApiEntity body;

    public PushNotificationApiBody(EnumPushNotificationType type, UUID channelId, GenericApiEntity body) {
        this.type = type;
        this.channelId = channelId;
        this.body = body;
    }

    public EnumPushNotificationType getType() {
        return type;
    }

    public void setType(EnumPushNotificationType type) {
        this.type = type;
    }

    public GenericApiEntity getBody() {
        return body;
    }

    public void setBody(GenericApiEntity body) {
        this.body = body;
    }

    public UUID getChannelId() {
        return channelId;
    }

    public void setChannelId(UUID channelId) {
        this.channelId = channelId;
    }
}
