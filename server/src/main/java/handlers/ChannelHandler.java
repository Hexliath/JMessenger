package handlers;

import entities.api.ChannelApiBody;
import entities.enums.EnumHttpCode;
import entities.enums.EnumPushNotificationType;
import exceptions.ProcessingException;
import services.ChannelService;
import services.NotificationService;
import system.DatabaseConnection;
import system.Utils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class ChannelHandler extends GenericHandler<ChannelApiBody> {

    private final ChannelService channelService;
    private final NotificationService notificationService;

    public ChannelHandler(String endpointPath, DatabaseConnection databaseConnection) {
        super(endpointPath, ChannelApiBody.class, databaseConnection);
        this.channelService = new ChannelService(databaseConnection);
        this.notificationService = new NotificationService(databaseConnection);
    }

    @Override
    public void handleGET() throws IOException, SQLException, ProcessingException {
        String sourceUrl = getHandlerEndpoint();

        if (sourceUrl.equals("")) {

            List<ChannelApiBody> channelList = channelService.getAllChannelsForUser(this.sourceUser);

            addHeader("Content-Type", "application/json");
            sendResponseHeaders(EnumHttpCode.OK);
            sendResponseBody(channelList);
        } else {
            handleBadRequest();
        }
    }

    @Override
    public void handlePOST() throws IOException, ProcessingException, SQLException {
        String sourceUrl = getHandlerEndpoint();

        if (sourceUrl.equals("")) {
            ChannelApiBody body = getRequestBodyObject();
            // Check if required body parameters are present, throw ProcessingException if some are missing
            requireBodyElements(body, "name");

            ChannelApiBody newChannel = channelService.createChannel(body, this.sourceUser);
            notificationService.sendNotifications(this.pushServer, sourceUser, newChannel.getId(), EnumPushNotificationType.NEW_CHANNEL, newChannel);

            addHeader("Content-Type", "application/json");
            sendResponseHeaders(EnumHttpCode.OK);
            sendResponseBody(newChannel);
        } else {
            handleBadRequest();
        }
    }

    @Override
    protected void handlePATCH() throws IOException, SQLException, ProcessingException {
        if(getHandlerEndpoint().matches("^\\/"+ Utils.UUID_REGEX)) {
            ChannelApiBody body = getRequestBodyObject();
            // Check if at least one of parameters are present, throw ProcessingException if all are missing
            requireAtLeastOneOfBodyElements(body, "name", "owner");

            ChannelApiBody newChannel = channelService.updateChannel(body, getUidFromSourceUri(), this.sourceUser);
            notificationService.sendNotifications(this.pushServer, sourceUser, newChannel.getId(), EnumPushNotificationType.CHANNEL_UPDATED, newChannel);


            addHeader("Content-Type", "application/json");
            sendResponseHeaders(EnumHttpCode.OK);
            sendResponseBody(newChannel);
        } else {
            handleBadRequest();
        }
    }

    // Hotfix, client does not support PATCH method, this method allows to access PATCH handler witch PUT method
    @Override
    protected void handlePUT() throws IOException, SQLException, ProcessingException {
        if(getHandlerEndpoint().matches("^\\/"+ Utils.UUID_REGEX)) {
            handlePATCH();
        } else {
            handleUnsupportedMethod();
        }
    }
}
