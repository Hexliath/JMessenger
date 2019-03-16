package handlers;

import entities.api.ChannelUserApiBody;
import entities.api.UserApiBody;
import entities.enums.EnumHttpCode;
import entities.enums.EnumPushNotificationType;
import exceptions.ProcessingException;
import services.AllocatorService;
import services.ChannelService;
import services.NotificationService;
import system.DatabaseConnection;
import system.Utils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class AllocatorHandler extends GenericHandler<UserApiBody> {

    private final AllocatorService allocatorService;
    private final NotificationService notificationService;
    private final ChannelService channelService;

    public AllocatorHandler(String endpointPath, DatabaseConnection databaseConnection) {
        super(endpointPath, UserApiBody.class, databaseConnection);
        this.allocatorService = new AllocatorService(databaseConnection);
        this.notificationService = new NotificationService(databaseConnection);
        this.channelService = new ChannelService(databaseConnection);
    }

    @Override
    public void handlePOST() throws IOException, SQLException, ProcessingException {
        String sourceUrl = getHandlerEndpoint();

        if(sourceUrl.matches("^/add-to/"+ Utils.UUID_REGEX+"$")) {
            UserApiBody body = getRequestBodyObject();
            // Check if required body parameters are present, throw ProcessingException if some are missing
            requireBodyElements(body, "id");

            ChannelUserApiBody output = allocatorService.addUserToChannel(body, sourceUser, getUidFromSourceUri());
            notificationService.sendNotifications(this.pushServer, sourceUser, getUidFromSourceUri(), EnumPushNotificationType.USER_JOIN, output);

            addHeader("Content-Type", "application/json");
            sendResponseHeaders(EnumHttpCode.OK);
            sendResponseBody(output);
        } else if(sourceUrl.matches("^/kick-from/"+ Utils.UUID_REGEX+"$")) {
            UserApiBody body = getRequestBodyObject();
            // Check if required body parameters are present, throw ProcessingException if some are missing
            requireBodyElements(body, "id");

            notificationService.sendNotifications(this.pushServer, sourceUser, getUidFromSourceUri(), EnumPushNotificationType.USER_LEAVE, body);
            allocatorService.kickUserFromChannel(body, sourceUser, getUidFromSourceUri());

            addHeader("Content-Type", "application/json");
            sendResponseHeaders(EnumHttpCode.OK);
            sendResponseBody();
        } else if (sourceUrl.matches("^/info-from/"+ Utils.UUID_REGEX+"$")) {
            UserApiBody body = getRequestBodyObject();
            // Check if required body parameters are present, throw ProcessingException if some are missing
            requireBodyElements(body, "id");

            ChannelUserApiBody output = allocatorService.getUserChannelInfo(body, sourceUser, getUidFromSourceUri());

            addHeader("Content-Type", "application/json");
            sendResponseHeaders(EnumHttpCode.OK);
            sendResponseBody(output);
        } else if (sourceUrl.matches("^/join/"+ Utils.UUID_REGEX+"$")) {
            allocatorService.joinChannel(sourceUser, getUidFromSourceUri());
            notificationService.sendNotifications(this.pushServer, sourceUser, getUidFromSourceUri(), EnumPushNotificationType.USER_JOIN,
                    allocatorService.getUserChannelInfo(new UserApiBody(
                            sourceUser.getId(),
                            null,
                            null
                    ),sourceUser,getUidFromSourceUri()));


            addHeader("Content-Type", "application/json");
            sendResponseHeaders(EnumHttpCode.OK);
            sendResponseBody();
        } else if (sourceUrl.matches("^/leave/"+ Utils.UUID_REGEX+"$")) {
            allocatorService.leaveChannel(sourceUser, getUidFromSourceUri());
            notificationService.sendNotifications(this.pushServer, sourceUser, getUidFromSourceUri(), EnumPushNotificationType.USER_LEAVE,
                    allocatorService.getUserChannelInfo(new UserApiBody(
                            sourceUser.getId(),
                            null,
                            null
                    ),sourceUser,getUidFromSourceUri()));

            addHeader("Content-Type", "application/json");
            sendResponseHeaders(EnumHttpCode.OK);
            sendResponseBody();
        } else {
            handleBadRequest();
        }
    }

    @Override
    public void handleGET() throws IOException, SQLException, ProcessingException {
        if(getHandlerEndpoint().matches("^/"+ Utils.UUID_REGEX+"/users$")) {
            List<ChannelUserApiBody> output = allocatorService.getChannelUsers(sourceUser, getUidFromSourceUri());

            addHeader("Content-Type", "application/json");
            sendResponseHeaders(EnumHttpCode.OK);
            sendResponseBody(output);
        } else {
            handleBadRequest();
        }
    }


}
