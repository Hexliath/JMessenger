package push;

import entities.api.GenericApiEntity;
import entities.enums.EnumPushNotificationType;
import exceptions.ProcessingException;
import services.AuthorizationService;
import services.SocketService;
import system.DatabaseConnection;
import system.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.SQLException;
import java.util.*;

/**
 * Push server service
 * This service provides a java socket listener that let users to connect and listen for server events.
 * Every user is handled by a separate thread, notifications are dispatched and threads identified whether
 * they are concerned or not by the notification
 */
public class PushServer extends Thread {
    protected static final Logger log = new Logger(PushServer.class);

    private ServerSocket serverSocket;
    private Map<UUID, PushClientThread> socketMap;

    private final AuthorizationService authorizationService;
    private final SocketService socketService;
    private final int port;
    private final UUID defaultChannelId;

    // Initiate required components
    public PushServer(int port, DatabaseConnection databaseConnection, UUID defaultChannelId) {
        this.socketMap = new HashMap<>();
        this.authorizationService = new AuthorizationService(databaseConnection);
        this.socketService = new SocketService(databaseConnection);
        this.port = port;
        try {
            socketService.clearSocketTable();
        } catch (SQLException e) {
            log.error(e);
        }
        this.defaultChannelId = defaultChannelId;
    }

    // Start infinite listen for connections loop
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            // Wait for connection queries, then create new thread and add it to list
            while (true) {
                // Create new UUID for the thread
                UUID newUuid = UUID.randomUUID();
                // Create (when a request arrived) a new thread
                PushClientThread newThread = new PushClientThread(this, serverSocket.accept(), newUuid, authorizationService, socketService);
                // Add it to map
                socketMap.put(newUuid, newThread);
                // start handling
                newThread.start();
            }
        } catch (IOException e) {
            log.error(e);
        }
    }

    // Sends notification to all active threads
    // Threads will decide to send or not the notification to user
    public void sendPushNotifications(EnumPushNotificationType type, GenericApiEntity body, List<String> targetTokens, UUID channelId) throws ProcessingException {
        if(channelId == null) {
            channelId = defaultChannelId;
        }
        List<UUID> keysToRemove = new ArrayList<>();
        // Iterate all active sockets threads, if they are finished then mark to delete from list
        // Else send notification demand
        for (Map.Entry<UUID, PushClientThread> entry : socketMap.entrySet())
        {
            if(!entry.getValue().isAlive()) {
                keysToRemove.add(entry.getKey());
            } else {
                entry.getValue().sendPushNotification(type, body, targetTokens, channelId);
            }
        }
        // Remove finished threads from list
        for (UUID id: keysToRemove) {
            socketMap.remove(id);
        }
    }

}
