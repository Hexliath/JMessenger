package handlers;

import entities.api.UserApiBody;
import entities.enums.EnumHttpCode;
import exceptions.ProcessingException;
import services.UserService;
import system.DatabaseConnection;
import system.Utils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class UserHandler extends GenericHandler<UserApiBody> {

    private final UserService userService;

    public UserHandler(String endpointPath, DatabaseConnection databaseConnection) {
        super(endpointPath, UserApiBody.class, databaseConnection);
        this.userService = new UserService(databaseConnection);
    }

    @Override
    protected void handleGET() throws IOException, SQLException, ProcessingException {

        String sourceUrl = getHandlerEndpoint();
        String completeQuery = getHandlerEndpoint() + "?" + this.exchange.getRequestURI().getQuery();

        if(sourceUrl.equals("/me")) {
            UserApiBody userApiBody = userService.getSelfUser(this.sourceUser);
            addHeader("Content-Type", "application/json");
            sendResponseHeaders(EnumHttpCode.OK);
            sendResponseBody(userApiBody);
        } else if (sourceUrl.matches("^\\/"+ Utils.UUID_REGEX)) {
            UUID userUid = UUID.fromString(sourceUrl.replace("/", ""));
            UserApiBody userApiBody = userService.getUser(userUid);
            addHeader("Content-Type", "application/json");
            sendResponseHeaders(EnumHttpCode.OK);
            sendResponseBody(userApiBody);
        } else if (completeQuery.matches("^\\/search\\?.*$") || completeQuery.matches("^\\/search\\?null$")) {
            List<UserApiBody> output;

            output = userService.searchUsers(this.exchange.getRequestURI().getQuery());

            addHeader("Content-Type", "application/json");
            sendResponseHeaders(EnumHttpCode.OK);
            sendResponseBody(output);
        } else {
            handleBadRequest();
        }
    }

    @Override
    public void handlePATCH() throws IOException, SQLException, ProcessingException {
        if(getHandlerEndpoint().equals("/me")) {
            UserApiBody body = getRequestBodyObject();
            // Check if required body parameters are present, throw ProcessingException if some are missing
            requireBodyElements(body, "displayName");

            UserApiBody updatedUser = userService.updateDisplayName(body, sourceUser);

            addHeader("Content-Type", "application/json");
            sendResponseHeaders(EnumHttpCode.OK);
            sendResponseBody(updatedUser);
        } else {
            handleBadRequest();
        }
    }

    // Hotfix, client does not support PATCH method, this method allows to access PATCH handler witch PUT method
    @Override
    protected void handlePUT() throws IOException, SQLException, ProcessingException {
        if(getHandlerEndpoint().equals("/me")) {
            handlePATCH();
        } else {
            handleUnsupportedMethod();
        }
    }

}
