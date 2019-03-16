package handlers;

import entities.api.ComplexArchiveQueryApiBody;
import entities.api.MessageApiBody;
import entities.enums.EnumHttpCode;
import exceptions.ProcessingException;
import services.ArchiveService;
import system.DatabaseConnection;
import system.Utils;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ArchiveHandler extends GenericHandler<ComplexArchiveQueryApiBody> {

    private ArchiveService archiveService;

    private List<UUID> channels;
    private Date since;


    public ArchiveHandler(String endpointPath, DatabaseConnection databaseConnection) {
        super(endpointPath, ComplexArchiveQueryApiBody.class, databaseConnection);
        this.archiveService = new ArchiveService(databaseConnection);
    }

    @Override
    public void handlePOST() throws IOException, SQLException, ProcessingException {
        if(getHandlerEndpoint().equals("/load")) {
            ComplexArchiveQueryApiBody body = getRequestBodyObject();
            // Check if required body parameters are present, throw ProcessingException if some are missing
            requireBodyElements(body, "channels", "since");

            Map<UUID, List<MessageApiBody>> output = archiveService.loadMessages(body, sourceUser);

            addHeader("Content-Type", "application/json");
            sendResponseHeaders(EnumHttpCode.OK);
            sendResponseBody(output);
        } else {
            handleBadRequest();
        }
    }

    @Override
    public void handleGET() throws IOException, SQLException, ProcessingException {
        String completeQuery = getHandlerEndpoint() + "?" + this.exchange.getRequestURI().getQuery();

        if(completeQuery.matches("^\\/"+ Utils.UUID_REGEX+"\\/search\\?since=[0-9]{1,13}$")) {

            UUID channelUid = getUidFromSourceUri();
            Date limitDate = Date.from(Instant.ofEpochMilli(Long.valueOf(this.exchange.getRequestURI().getQuery().replace("since=", ""))));

            List<MessageApiBody> output = archiveService.loadChannelHistory(channelUid, limitDate, sourceUser);

            addHeader("Content-Type", "application/json");
            sendResponseHeaders(EnumHttpCode.OK);
            sendResponseBody(output);
        } else if (getHandlerEndpoint().matches("^\\/export\\/"+ Utils.UUID_REGEX)) {
            UUID channelUid = getUidFromSourceUri();

            String xmlOutput = archiveService.getExportXml(channelUid, sourceUser);

            addHeader("Content-Type", "application/xml");
            sendResponseHeaders(EnumHttpCode.OK);
            sendResponseBody(xmlOutput);
        } else {
            handleBadRequest();
        }
    }


}
