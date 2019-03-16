import com.sun.net.httpserver.HttpServer;
import entities.enums.EnumLogLevel;
import exceptions.ConfigurationException;
import handlers.*;
import push.PushServer;
import services.AuthorizationService;
import services.MessageService;
import system.DatabaseConfigurator;
import system.DatabaseConnection;
import system.Logger;
import system.Utils;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;


/**
 * Main Application class
 * To run the services this class initiates the database, Push Service, HttpServer and all context handlers.
 * Performs also a config load from configuration file to classes
 */
public class Application {
    private static final Logger log = new Logger(Application.class);

    private static final String serverVersion = "1.0.1";
    private static final String apiVersion = "1.0.1";


    public static void main(String[] args) throws Exception {
        // Get config
        Map<String, String> config = Utils.getConfigMap();
        // Set logger at first, from config or default
        try {
            Logger.logLevel = EnumLogLevel.valueOf(config.get("system_log_level"));
        } catch (Exception e) {
            Logger.logLevel = EnumLogLevel.DEBUG;
        }

        // Print logo and version
        log.logo("Java Messenger Server", serverVersion, apiVersion);

        // Load other properties
        int serverApiPort = Integer.valueOf(config.get("server_api_port"));
        int serverPushPort = Integer.valueOf(config.get("server_push_port"));
        String apiPath = config.get("server_api_path");
        loadAppConfig(config);


        // New http server
        HttpServer server = HttpServer.create(new InetSocketAddress(serverApiPort), 0);

        DatabaseConnection databaseConnection;
        try {
            databaseConnection = new DatabaseConfigurator().getDatabase();
        } catch (ConfigurationException e) {
            log.error(e);
            log.fatal("Unable to configure database");
            return;
        }

        PushServer push = new PushServer(serverPushPort, databaseConnection, UUID.fromString(config.get("default_channel_uid")));
        push.start();

        startHandler(server, new HeathCheckHandler( apiPath + "/ping", databaseConnection), push);
        startHandler(server, new AuthorizationHandler(apiPath + "/auth", databaseConnection), push);
        startHandler(server, new UserHandler(apiPath + "/users", databaseConnection));
        startHandler(server, new ChannelHandler(apiPath + "/channels", databaseConnection), push);
        startHandler(server, new RoleHandler(apiPath + "/roles", databaseConnection));
        startHandler(server, new AllocatorHandler(apiPath + "/allocator", databaseConnection), push);
        startHandler(server, new ArchiveHandler(apiPath + "/archive", databaseConnection));
        startHandler(server, new MessageHandler(apiPath + "/messages", databaseConnection), push);

        server.setExecutor(null);
        server.start();

        log.spacing();
        log.info("API  service is online, listening on port " + serverApiPort);
        log.info("PUSH service is online, listening on port " + serverPushPort);
        log.spacing();
    }

    private static void startHandler(HttpServer server, GenericHandler handler) {
        server.createContext(handler.getEndpointPath(), handler);
        //log.info(handler.getClass().getSimpleName()+"\t\tstarted at\t\t\t" + handler.getEndpointPath());
        log.info(String.format(
                "%-25s started at %-5s%s",
                handler.getClass().getSimpleName(),
                "",
                handler.getEndpointPath()
        ));
    }

    private static void startHandler(HttpServer server, GenericHandler handler, PushServer pushServer) {
        handler.setPushServer(pushServer);
        startHandler(server, handler);
    }

    private static void loadAppConfig(Map<String, String> config) {
        // AuthorizationService
        AuthorizationService.ATTEMPT_LIMIT = Integer.valueOf(config.get("auth_attempt_limit"));
        AuthorizationService.TOKEN_VALIDITY = Integer.valueOf(config.get("auth_token_validity_hours"));
        AuthorizationService.BLOCK_TIME = Integer.valueOf(config.get("auth_ip_block_duration_minutes"));
        AuthorizationService.SIMULTANEOUS_CONNECTION_LIMIT = Integer.valueOf(config.get("auth_simultaneous_connection_limit"));
        AuthorizationService.DEFAULT_CHANNEL_ID = UUID.fromString(config.get("default_channel_uid"));

        //Message service
        MessageService.MAX_MESSAGE_LENGTH = Integer.valueOf(config.get("message_max_length"));

    }

}
