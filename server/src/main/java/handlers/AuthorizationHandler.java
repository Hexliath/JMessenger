package handlers;

import entities.User;
import entities.api.AuthorizationTokenApiBody;
import entities.api.LoginApiBody;
import entities.api.LoginResultApiBody;
import entities.api.UserApiBody;
import entities.enums.EnumCustomErrorCode;
import entities.enums.EnumHttpCode;
import entities.enums.EnumPushNotificationType;
import exceptions.ProcessingException;
import services.AuthorizationService;
import services.NotificationService;
import system.DatabaseConnection;
import system.Utils;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;


public class AuthorizationHandler extends GenericHandler<LoginApiBody> {

    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;

    public AuthorizationHandler(String endpointPath, DatabaseConnection databaseConnection) {
        super(endpointPath, LoginApiBody.class, databaseConnection);
        this.authorizationService = new AuthorizationService(databaseConnection);
        this.notificationService = new NotificationService(databaseConnection);
    }

    @Override
    protected void handleUnsignedPOST() throws IOException, SQLException, ProcessingException {
        switch (getHandlerEndpoint()) {
            case "/register":
                registerNewAccount();
                break;
            case "/login":
                provideAuthorizationTokenForUser();
                break;
            default:
                handleForbiddenMethod();
        }

    }

    @Override
    protected void handlePOST() throws IOException, ProcessingException, SQLException {
        switch (getHandlerEndpoint()) {
            case "/register":
                throw new ProcessingException(EnumCustomErrorCode.LOGOUT_REQUIRED, "You must logout to register a new account");
            case "/login":
                provideAuthorizationTokenForUser();
                break;
            case "/logout":
                destroyUserToken();
                break;
            case "/logout/all":
                destroyAllUserToken();
                break;
            default:
                handleNotFoundMethod();
        }
    }

    private void registerNewAccount() throws IOException, SQLException, ProcessingException {
        LoginApiBody body = getRequestBodyObject();
        // Check if required body parameters are present, throw ProcessingException if some are missing
        requireBodyElements(body, "login", "password");

        UserApiBody newUser = authorizationService.createAccount(body);

        User mockUser = new User(newUser.getId());
        mockUser.setAuthorizationToken(new AuthorizationTokenApiBody(
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                Date.from(Instant.now().plus(1, ChronoUnit.HOURS))));
        notificationService.sendNotifications(this.pushServer, mockUser, UUID.fromString(Utils.getConfigMap().get("default_channel_uid")), EnumPushNotificationType.USER_REGISTER, newUser);

        addHeader("Content-Type", "application/json");
        sendResponseHeaders(EnumHttpCode.OK);
        sendResponseBody(newUser);

    }

    private void provideAuthorizationTokenForUser() throws IOException, SQLException, ProcessingException {
        LoginApiBody body = getRequestBodyObject();
        // Check if required body parameters are present, throw ProcessingException if some are missing
        requireBodyElements(body, "login", "password");

        LoginResultApiBody loginResult = authorizationService.getAuthorizationToken(body, ipAddress);

        addHeader("Content-Type", "application/json");
        sendResponseHeaders(EnumHttpCode.OK);
        sendResponseBody(loginResult);
    }

    private void destroyUserToken() throws IOException, SQLException {
        authorizationService.deleteAuthorizationToken(sourceUser.getAuthorizationToken());
        addHeader("Content-Type", "application/json");
        sendResponseHeaders(EnumHttpCode.OK);
        sendResponseBody();
    }

    private void destroyAllUserToken() throws IOException, SQLException, ProcessingException {
        authorizationService.deleteAllAuthorizationToken(sourceUser.getAuthorizationToken());
        addHeader("Content-Type", "application/json");
        sendResponseHeaders(EnumHttpCode.OK);
        sendResponseBody();
    }

}
