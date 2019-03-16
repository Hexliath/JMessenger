package push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import entities.User;
import entities.api.*;
import entities.enums.EnumCustomErrorCode;
import entities.enums.EnumHttpCode;
import entities.enums.EnumPushNotificationType;
import exceptions.ProcessingException;
import services.AuthorizationService;
import services.SocketService;
import system.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Single client push thread
 * This class handles socket connection for a single user.
 * It provides token authorization on the initial handshake.
 * It sends notifications to the stream when needed.
 */
public class PushClientThread extends Thread {
    protected static final Logger log = new Logger(PushClientThread.class);

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private UUID socketId;
    private final PushServer sourceServer;
    private User user;

    private final AuthorizationService authorizationService;
    private final SocketService socketService;
    private String userToken;

    public PushClientThread(PushServer sourceServer, Socket socket, UUID socketId, AuthorizationService authorizationService, SocketService socketService) {
        this.sourceServer = sourceServer;
        this.clientSocket = socket;
        this.socketId = socketId;
        this.authorizationService = authorizationService;
        this.socketService = socketService;
        this.user = null;
    }

    public void run() {
        try {
            // Open streams
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String inputLine;

            // Listen for authorization token
            while ((inputLine = in.readLine()) != null) {
                // Check if body is present
                AuthorizationPushBody body;
                try {
                    body = getAuthorizationBody(inputLine);
                } catch (ProcessingException e) {
                    out.println(new DefaultApiResponse(
                            EnumHttpCode.BAD_REQUEST,
                            EnumCustomErrorCode.REST_EXCEPTION,
                            "Provided body isn't valid"
                    ).toJson());
                    closeSocket(false);
                    return;
                }

                try {
                    user = authorizationService.getUserFromToken(body.getToken());
                } catch (ProcessingException e) {
                    user = null;
                }

                // Check if provided token is valid
                if( body.getToken() == null || body.getToken().length() != 64 || user == null  ) {
                    out.println(new DefaultApiResponse(
                            EnumHttpCode.UNAUTHORISED,
                            EnumCustomErrorCode.REST_EXCEPTION,
                            "You need to provide a valid token to access this resource"
                    ).toJson());
                    closeSocket(false);
                    return;
                }

                // Save token
                userToken = body.getToken();

                // Check if provided token is occupied
                if ( socketService.isTokenOccupied(userToken) ){
                    out.println(new DefaultApiResponse(
                            EnumHttpCode.CUSTOM,
                            EnumCustomErrorCode.TOO_MANY_CONNECTIONS,
                            "Only one connection to push service per session is allowed"
                    ).toJson());
                    closeSocket(false);
                    return;
                }

                // success
                break;
            }

            // Make sure the token is provided
            if (userToken == null || user == null) {
                out.println(new DefaultApiResponse(
                        EnumHttpCode.CUSTOM,
                        EnumCustomErrorCode.TRY_AGAIN_LATER,
                        "Unable to get token"
                ).toJson());
                closeSocket(false);
                return;
            }

            // Inform about successful connection
            out.println(new DefaultApiResponse(
                    EnumHttpCode.CUSTOM,
                    EnumCustomErrorCode.SUCCESS,
                    "Connected to push notification service"
            ).toJson());

            // Save the socket
            socketService.saveSocket(userToken, socketId);
            log.debug("Push socket "+socketId+" opened");

            // Send global notification
            List<String> targetTokens = new ArrayList<>();
            targetTokens.add("GLOBAL");
            targetTokens.add(userToken);
            sourceServer.sendPushNotifications(EnumPushNotificationType.USER_ONLINE, new UserApiBody(
                    user.getId(),
                    user.getLogin(),
                    user.getDisplayName(),
                    true
            ), targetTokens, null);

            while ((inputLine = in.readLine()) != null) {

                // Inform that socket does not accept any further data
                out.println(new DefaultApiResponse(
                        EnumHttpCode.CUSTOM,
                        EnumCustomErrorCode.NO_INCOMING_DATA_ACCEPTED,
                        "Push service does not accept any data input"
                ).toJson());
            }

            this.closeSocket(true);
        } catch (IOException | SQLException | ProcessingException e) {
            log.error(e);
        }
    }

    // Check if the auth token attached to this thread is in destinations of the notification, and send if true
    public void sendPushNotification(EnumPushNotificationType type, GenericApiEntity body, List<String> tokens, UUID channelId) throws ProcessingException {
        if(!tokens.isEmpty() && (tokens.contains(userToken) || tokens.contains("GLOBAL"))) {
            if(tokens.contains("GLOBAL") && tokens.contains(userToken)) {
                return; // If list contains global, then all other tokens in list are sending exceptions
            }
            out.println(new PushNotificationApiBody(
                    type,
                    channelId,
                    body
            ).toJson());
        }
    }

    // Close streams and free the token in database
    private void closeSocket(boolean notifiy) throws IOException, SQLException, ProcessingException {
        in.close();
        out.close();
        clientSocket.close();
        socketService.removeSocket(socketId);
        log.debug("Push socket " + socketId + " closed");

        if(notifiy) {
            // Send global notification
            List<String> targetTokens = new ArrayList<>();
            targetTokens.add("GLOBAL");
            sourceServer.sendPushNotifications(EnumPushNotificationType.USER_OFFLINE, new UserApiBody(
                    user.getId(),
                    user.getLogin(),
                    user.getDisplayName(),
                    false
            ), targetTokens, null);
        }
    }

    // Get auth body from user query
    private AuthorizationPushBody getAuthorizationBody(String payload) throws ProcessingException {
        ObjectMapper mapper = getObjectMapper();
        try {
            return mapper.readValue(payload, AuthorizationPushBody.class);
        } catch (IOException e) {
            throw new ProcessingException(EnumCustomErrorCode.REST_EXCEPTION, "Request body is invalid, please refer to api documentation. Required entity is: " + payload.getClass().getSimpleName().replace("ApiBody", ""), EnumHttpCode.BAD_REQUEST);
        }
    }

    // Object mapper provider, lets customise the strategy in a single place
    private ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return objectMapper;
    }

}
