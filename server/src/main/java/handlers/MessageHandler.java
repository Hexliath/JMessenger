package handlers;

import entities.api.MessageApiBody;
import entities.enums.EnumHttpCode;
import entities.enums.EnumPushNotificationType;
import exceptions.ProcessingException;
import services.MessageService;
import services.NotificationService;
import system.DatabaseConnection;

import java.io.IOException;
import java.sql.SQLException;

public class MessageHandler extends GenericHandler<MessageApiBody> {

    private final MessageService messageService;
    private final NotificationService notificationService;

    public MessageHandler(String endpointPath, DatabaseConnection databaseConnection) {
        super(endpointPath, MessageApiBody.class, databaseConnection);
        this.messageService = new MessageService(databaseConnection);
        this.notificationService = new NotificationService(databaseConnection);
    }

    @Override
    protected void handlePOST() throws IOException, SQLException, ProcessingException {
        if(getHandlerEndpoint().equals("")){
            MessageApiBody body = getRequestBodyObject();
            // Check if required body parameters are present, throw ProcessingException if some are missing
            requireBodyElements(body, "content", "channelId");

            MessageApiBody newMessage = messageService.sendMessage(body, this.sourceUser);
            notificationService.sendNotifications(this.pushServer, sourceUser, newMessage.getChannelId(), EnumPushNotificationType.NEW_MESSAGE, newMessage);

            addHeader("Content-Type", "application/json");
            sendResponseHeaders(EnumHttpCode.OK);
            sendResponseBody(newMessage);
        } else {
            handleBadRequest();
        }
    }
}
