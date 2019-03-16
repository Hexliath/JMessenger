package services;

import entities.User;
import entities.api.GenericApiEntity;
import entities.enums.EnumPushNotificationType;
import exceptions.ProcessingException;
import push.PushServer;
import repositories.ChannelUserRepository;
import system.DatabaseConnection;
import system.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NotificationService {
    protected static final Logger log = new Logger(NotificationService.class);

    private final ChannelUserRepository channelUserRepository;

    public NotificationService(DatabaseConnection databaseConnection) {
        this.channelUserRepository = new ChannelUserRepository(databaseConnection.get());
    }

    // Get token list of users in a channel
    private List<String> getAuthTokensToNotify(UUID channelId, User sourceUser) throws SQLException {
        ResultSet rsChannelUsersTokens = channelUserRepository.getAllChannelUserTokensWherePushActiveAndNotSource(channelId, sourceUser.getAuthorizationToken().getToken());
        List<String> output = new ArrayList<>();
        while (rsChannelUsersTokens.next()) {
            output.add(rsChannelUsersTokens.getString("auth_token"));
        }
        return output;
    }

    // Send notification request to pushServer
    public void sendNotifications(PushServer pushServer, User sourceUser, UUID channelId, EnumPushNotificationType type, GenericApiEntity body) throws SQLException, ProcessingException {
        List<String> targetTokens = getAuthTokensToNotify(channelId, sourceUser);
        if( targetTokens.isEmpty() ) {
            return;
        }
        if(pushServer == null) {
            log.warn("Unable to send notifications. PushServer is not defined for this controller");
        } else {
            pushServer.sendPushNotifications(type, body, targetTokens, channelId);
        }
    }


}
