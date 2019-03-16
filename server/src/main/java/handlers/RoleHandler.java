package handlers;

import entities.api.ChannelUserApiBody;
import entities.enums.EnumHttpCode;
import exceptions.ProcessingException;
import services.RoleService;
import system.DatabaseConnection;
import system.Utils;

import java.io.IOException;
import java.sql.SQLException;

public class RoleHandler extends GenericHandler<ChannelUserApiBody> {

    private final RoleService roleService;

    public RoleHandler(String endpointPath, DatabaseConnection databaseConnection) {
        super(endpointPath, ChannelUserApiBody.class, databaseConnection);
        this.roleService = new RoleService(databaseConnection);
    }

    @Override
    public void handlePATCH() throws IOException, SQLException, ProcessingException {
        if(getHandlerEndpoint().matches("^\\/"+ Utils.UUID_REGEX)) {
            ChannelUserApiBody body = getRequestBodyObject();
            // Check if required body parameters are present, throw ProcessingException if some are missing
            requireBodyElements(body, "user", "role");

            ChannelUserApiBody output = roleService.setUserRole(body, this.sourceUser, getUidFromSourceUri());

            addHeader("Content-Type", "application/json");
            sendResponseHeaders(EnumHttpCode.OK);
            sendResponseBody(output);
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
