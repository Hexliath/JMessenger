package handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import entities.api.DefaultApiResponse;
import system.DatabaseConnection;
import system.Logger;

import java.io.IOException;
import java.io.OutputStream;

public class HeathCheckHandler extends GenericHandler<DefaultApiResponse> implements HttpHandler {
    private static final Logger log = new Logger(HeathCheckHandler.class);

    public HeathCheckHandler(String endpointPath, DatabaseConnection databaseConnection) {
        super(endpointPath, DefaultApiResponse.class, databaseConnection);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String response = "pong";
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
