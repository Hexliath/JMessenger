package handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import entities.User;
import entities.api.DefaultApiResponse;
import entities.api.GenericApiEntity;
import entities.enums.EnumCustomErrorCode;
import entities.enums.EnumHttpCode;
import exceptions.ProcessingException;
import push.PushServer;
import services.AuthorizationService;
import system.DatabaseConnection;
import system.Logger;
import system.Utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract Handler class
 * Provides essential methods to children handlers. It handles body mapping, response sending and method recognition.
 * Evey request is routed to a specific method depending on HTTP method and authorization provided.
 * If a children handler wants to handle one specific case, all it needs to do is override the corresponding method.
 * One handler supports one type T of body load.
 * @param <T> type of input body objects
 */
public abstract class GenericHandler<T> implements HttpHandler {
    protected final Logger log;
    // HttpExchange object delivered by HttpServer
    protected HttpExchange exchange;
    // Handler api path
    protected final String endpointPath;
    // Attached class
    private final Class<T> entityClass;
    // Authenticated user
    protected User sourceUser;
    // IP of incoming request
    protected String ipAddress;
    // Push server to send notifications
    protected PushServer pushServer;
    // Database connection
    private final DatabaseConnection databaseConnection;

    private final AuthorizationService authorizationService;

    public GenericHandler(String endpointPath, Class<T> entityClass, DatabaseConnection databaseConnection) {
        this.log = new Logger(this.getClass());
        this.endpointPath = endpointPath;
        this.entityClass = entityClass;
        this.authorizationService = new AuthorizationService(databaseConnection);
        this.databaseConnection = databaseConnection;
    }

    // Override handle method to store the exchange element and run handler dispatcher and catch exceptions
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            this.ipAddress = getIpAddress(exchange);
            this.exchange = exchange;
            selectHandleSwitchingMethod();
        } catch (ProcessingException e) {
            try {
                if (e.hasHttpCode()) {
                    handleCustomError(e.getCustomErrorCode(), e.getMessage(), e.getHttpCode());
                } else {
                    handleCustomError(e.getCustomErrorCode(), e.getMessage());
                }
            } catch (ProcessingException ignored) {
                sendResponseHeaders(EnumHttpCode.INTERNAL_SERVER_ERROR);
                sendResponseBody();
            }
        } catch (Exception e) {
            log.error(e);
            try {
                handleInternalServerError();
            } catch (ProcessingException ignored) {
                sendResponseHeaders(EnumHttpCode.INTERNAL_SERVER_ERROR);
                sendResponseBody();
            }
        }
    }

    private String getIpAddress(HttpExchange exchange) {
        String ipAddress = exchange.getRequestHeaders().getFirst("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = exchange.getRemoteAddress().toString();
        }
        return ipAddress;
    }

    // Get Authorisation header
    private String getAuthorizationHeader() {
        Headers requestHeaders = exchange.getRequestHeaders();
        if(requestHeaders.containsKey("Authorization")) {
            return requestHeaders.getFirst("Authorization");
        } else {
            return "";
        }
    }

    // Add a single header string to response
    protected void addHeader(String key, String value) {
        exchange.getResponseHeaders().set(key, value);
    }

    // Add multiple headers to response
    protected void addHeaders(Map<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            exchange.getResponseHeaders().set(entry.getKey(), entry.getValue());
        }
    }

    // Send empty response body
    protected void sendResponseBody() throws IOException {
        exchange.getResponseBody().close();
    }

    // Send raw response body
    protected void sendResponseBody(String body) throws IOException {
        exchange.getResponseBody().write(body.getBytes());
        exchange.getResponseBody().close();
    }

    // Convert object to Json String and send
    protected void sendResponseBody(GenericApiEntity genericApiEntity) throws IOException, ProcessingException {
        sendResponseBody(genericApiEntity.toJson());
    }

    // Convert list of objects to Json String and send
    protected void sendResponseBody(List genericApiEntityList) throws IOException, ProcessingException {
        String stringBody;
        ObjectMapper mapper = getObjectMapper();
        try {
            stringBody = mapper.writeValueAsString(genericApiEntityList);
            sendResponseBody(stringBody);
        } catch (JsonProcessingException e) {
            log.error(e);
            handleInternalServerError();
        }
    }

    // Convert map of objects to Json String and send
    protected void sendResponseBody(Map genericApiEntityMap) throws IOException, ProcessingException {
        ObjectMapper mapper = getObjectMapper();
        try {
            sendResponseBody(mapper.writeValueAsString(genericApiEntityMap));
        } catch (JsonProcessingException e) {
            log.error(e);
            handleInternalServerError();
        }
    }

    // Send response headers
    protected void sendResponseHeaders(EnumHttpCode httpCode) throws IOException {
        exchange.sendResponseHeaders(httpCode.getValue(), 0);
    }

    // Map request body to given object, return empty if body is not a correct object mapping
    protected T getRequestBodyObject() throws ProcessingException {
        ObjectMapper mapper = getObjectMapper();
        try {
            return mapper.readValue(exchange.getRequestBody(), entityClass);
        } catch (IOException e) {
            log.warn(e.getMessage());
            throw new ProcessingException(EnumCustomErrorCode.REST_EXCEPTION, "Request body is invalid, please refer to api documentation. Required entity is: "+entityClass.getSimpleName().replace("ApiBody", ""), EnumHttpCode.BAD_REQUEST);
        }
    }

    // Send back an error message thrown by the process, default http code
    private void handleCustomError(EnumCustomErrorCode code, String message) throws IOException, ProcessingException {
        addHeader("Content-Type", "application/json");
        sendResponseHeaders(EnumHttpCode.BAD_REQUEST);
        sendResponseBody(new DefaultApiResponse(EnumHttpCode.CUSTOM, code, message));
    }

    // Send back an error message thrown by the process, custom http code
    private void handleCustomError(EnumCustomErrorCode code, String message, EnumHttpCode httpCode) throws IOException, ProcessingException {
        addHeader("Content-Type", "application/json");
        sendResponseHeaders(EnumHttpCode.BAD_REQUEST);
        sendResponseBody(new DefaultApiResponse(httpCode, code, message));
    }

    // Send back information about bad request
    protected void handleBadRequest() throws IOException, ProcessingException {
        addHeader("Content-Type", "application/json");
        sendResponseHeaders(EnumHttpCode.BAD_REQUEST);
        sendResponseBody(new DefaultApiResponse(EnumHttpCode.BAD_REQUEST, EnumCustomErrorCode.REST_EXCEPTION, "Bad request"));
    }

    // Send back information about unsupported method
    protected void handleUnsupportedMethod() throws IOException, ProcessingException {
        addHeader("Content-Type", "application/json");
        sendResponseHeaders(EnumHttpCode.METHOD_NOT_ALLOWED);
        sendResponseBody(new DefaultApiResponse(EnumHttpCode.METHOD_NOT_ALLOWED, EnumCustomErrorCode.REST_EXCEPTION, "["+exchange.getRequestMethod()+"] method is not allowed on this endpoint"));
    }

    // Send back information about forbidden method
    protected void handleForbiddenMethod() throws IOException, ProcessingException {
        addHeader("Content-Type", "application/json");
        sendResponseHeaders(EnumHttpCode.FORBIDDEN);
        sendResponseBody(new DefaultApiResponse(EnumHttpCode.FORBIDDEN, EnumCustomErrorCode.REST_EXCEPTION, "You have to provide valid Authorization to access this resource"));
    }

    // Send back information method not found
    protected void handleNotFoundMethod() throws IOException, ProcessingException {
        addHeader("Content-Type", "application/json");
        sendResponseHeaders(EnumHttpCode.NOT_FOUND);
        sendResponseBody(new DefaultApiResponse(EnumHttpCode.NOT_FOUND, EnumCustomErrorCode.REST_EXCEPTION, "There is no such service found on this server"));
    }

    // Send back information about an internal server error
    protected void handleInternalServerError() throws IOException, ProcessingException {
        addHeader("Content-Type", "application/json");
        sendResponseHeaders(EnumHttpCode.INTERNAL_SERVER_ERROR);
        sendResponseBody(new DefaultApiResponse(EnumHttpCode.INTERNAL_SERVER_ERROR, EnumCustomErrorCode.REST_EXCEPTION, "The server was unable to complete the request"));
    }

    // Handle GET with Authorization
    protected void handleGET() throws IOException, ProcessingException, SQLException {
        handleUnsupportedMethod();
    }
    // Handle GET without Authorization
    protected void handleUnsignedGET() throws IOException, ProcessingException, SQLException {
        handleForbiddenMethod();
    }

    // Handle POST with Authorization
    protected void handlePOST() throws IOException, ProcessingException, SQLException {
        handleUnsupportedMethod();
    }

    // Handle POST without Authorization
    protected void handleUnsignedPOST() throws IOException, ProcessingException, SQLException {
        handleForbiddenMethod();
    }

    // Handle PUT with Authorization
    protected void handlePUT() throws IOException, ProcessingException, SQLException {
        handleUnsupportedMethod();
    }

    // Handle PATCH with Authorization
    protected void handlePATCH() throws IOException, ProcessingException, SQLException {
        handleUnsupportedMethod();
    }

    // Handle DELETE with Authorization
    protected void handleDELETE() throws IOException, ProcessingException, SQLException {
        handleUnsupportedMethod();
    }

    // Select the switching method depending on token provided
    private void selectHandleSwitchingMethod() throws IOException, ProcessingException, SQLException {
        try {
            if(authorizationService.isTokenValid(getAuthorizationHeader())) {
                this.sourceUser = authorizationService.getUserFromToken(getAuthorizationHeader());
                switchHandleMethod();
            } else {
                this.sourceUser = null;
                switchUnsignedHandleMethod();
            }
        } catch (SQLException e) {
            log.error(e);
            handleInternalServerError();
        }
    }

    // Select correct handle method for requests when no, or invalid token
    private void switchUnsignedHandleMethod() throws IOException, ProcessingException, SQLException {
        String requestMethod = exchange.getRequestMethod().toUpperCase();
        switch (requestMethod) {
            case "GET":
                handleUnsignedGET();
                break;
            case "POST":
                handleUnsignedPOST();
                break;
            default:
                handleForbiddenMethod();
        }
    }

    // Select correct handle method when token is valid
    private void switchHandleMethod() throws IOException, ProcessingException, SQLException {
        String requestMethod = exchange.getRequestMethod().toUpperCase();
        switch (requestMethod) {
            case "GET":
                handleGET();
                break;
            case "POST":
                handlePOST();
                break;
            case "PUT":
                handlePUT();
                break;
            case "PATCH":
                handlePATCH();
                break;
            case "DELETE":
                handleDELETE();
                break;
            default:
                handleUnsupportedMethod();
        }
    }

    // Return handler endpoint without handler prefix
    protected String getHandlerEndpoint() {
        return exchange.getRequestURI().getPath().replace(endpointPath, "");
    }

    // Retrieve UUID from URL
    protected UUID getUidFromSourceUri() {
        Matcher m = Pattern.compile(Utils.UUID_PATTERN).matcher(getHandlerEndpoint());
        if(m.find())
        {
            return UUID.fromString(m.group());
        }
        return null;
    }

    // Object mapper provider, to customise the strategy in a single place
    private ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return objectMapper;
    }

    // Checks if specified fields are present, throws an exception if one or more are missing
    public void requireBodyElements(GenericApiEntity body, String... fieldNames) throws ProcessingException {
        List<String> missingFields = getMissingFieldsNames(body, fieldNames);
        if(missingFields.size() == 0) {
            return;
        }
        String missing = String.join(", ", missingFields);
        throw new ProcessingException(
                EnumCustomErrorCode.MISSING_FIELD,
                "Provided "+body.getClass().getSimpleName().replace("ApiBody", "")+" body is invalid. Field(s) missing: " + missing
        );
    }

    // Checks if specified fields are present, throws an exception if one or more are missing
    public void requireAtLeastOneOfBodyElements(GenericApiEntity body, String... fieldNames) throws ProcessingException {
        List<String> missingFields = getMissingFieldsNames(body, fieldNames);
        if(fieldNames.length - missingFields.size() > 0) {
            return;
        }
        throw new ProcessingException(
                EnumCustomErrorCode.MISSING_FIELD,
                "Provided "+body.getClass().getSimpleName().replace("ApiBody", "")+" body is invalid. You have to provide at least one of those fields: " + fieldNames
        );
    }

    // Checks all passed parameters to be not null, allows to check if all required parameters are present in body
    private static List<String> getMissingFieldsNames(GenericApiEntity body, String... fieldNames) throws ProcessingException {
        List<String> missingFieldNames = new ArrayList<>();
        Class bodyClass = body.getClass();
        for (String fieldName: fieldNames) {
            try {
                Field field = bodyClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                if(field.get(body) == null) {
                    missingFieldNames.add(fieldName);
                }
                field.setAccessible(false);
            } catch (IllegalAccessException | NoSuchFieldException e ) {
                new Logger(GenericHandler.class).error("Bad body field check: '"+fieldName+"' doesn't exist in "+bodyClass.getSimpleName());
                throw new ProcessingException(
                        EnumCustomErrorCode.BODY_CHECK_FAIL,
                        "This is a server side error, due to some recent changes. This endpoint won't work until manually fixed. We are sorry for the inconvenience. " +
                                "[NO SUCH FILED: "+bodyClass.getSimpleName()+"->"+fieldName+"]",
                        EnumHttpCode.INTERNAL_SERVER_ERROR
                );
            }
        }
        return missingFieldNames;
    }

    // Set the push notification server if handler is supposed to send notifications
    public void setPushServer(PushServer pushServer) {
        this.pushServer = pushServer;
    }

    // Returns the full endpoint path
    public String getEndpointPath() {
        return endpointPath;
    }
}
