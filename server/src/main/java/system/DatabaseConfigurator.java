package system;

import entities.enums.EnumChannelType;
import exceptions.ConfigurationException;
import repositories.ChannelRepository;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.Date;
import java.util.*;

/**
 * Database configurator / installer
 * This class checks the database structure and runs sql script to initialise tables if needed
 */
public class DatabaseConfigurator {
    private static final Logger log = new Logger(DatabaseConfigurator.class);

    private final String host;
    private final int port;
    private final String database;
    private final String schema;
    private final String user;
    private final String password;
    private final UUID defaultChannelUid;

    private List<String> expectedTables;

    // Setup configs
    public DatabaseConfigurator() {
        Map<String, String> appConfig = Utils.getConfigMap();
        this.host = appConfig.get("sql_host");
        this.port = Integer.valueOf(appConfig.get("sql_port"));
        this.database = appConfig.get("sql_database");
        this.schema = appConfig.get("sql_schema");
        this.user = appConfig.get("sql_user");
        this.password = appConfig.get("sql_password");
        this.defaultChannelUid = UUID.fromString(appConfig.get("default_channel_uid"));

        this.expectedTables = new ArrayList<>();
        expectedTables.add("attachments");
        expectedTables.add("auth");
        expectedTables.add("channel_users");
        expectedTables.add("channels");
        expectedTables.add("ip_filter");
        expectedTables.add("messages");
        expectedTables.add("sockets");
        expectedTables.add("users");
    }

    // Provide database connection
    public DatabaseConnection getDatabase() throws SQLException, ConfigurationException {

        // Create connection
        DatabaseConnection databaseConnection = new DatabaseConnection(host, port, database, schema, user, password);

        // Check if connection established
        if(databaseConnection.get() == null) {
            throw new ConfigurationException("Unable to contact the database");
        }
        log.debug("Connected to database '"+database+"'");

        // Set the user default schema
        setUserDefaultSchema(databaseConnection.get(), user, schema);
        log.debug("Default schema for '"+user+"' set to '"+schema+"'");

        // Restart the connection to apply user schema change
        databaseConnection.get().close();
        databaseConnection = new DatabaseConnection(host, port, database, schema, user, password);

        // If no tables in schema then create them
        if( !isAnyTableInSchema(databaseConnection.get(), schema) ) {
            log.info("Installing database in schema '"+schema+"'");
            try {
                initDatabase(databaseConnection.get());
            } catch (IOException e) {
                log.error(e);
                throw new ConfigurationException("Unable to create tables");
            }
        }

        // Check if the default channel exist
        ChannelRepository channelRepository = new ChannelRepository(databaseConnection.get());
        ResultSet rsChannel = channelRepository.getChannelById(defaultChannelUid);
        if(!rsChannel.next()) {
            channelRepository.saveNewChannel(defaultChannelUid, "General", EnumChannelType.PUBLIC, Date.from(Instant.now()));
            log.debug("Creating default channel");
        }

        // Check for tables presence
        for (String tableName: expectedTables) {
            if ( !isTablePresent(databaseConnection.get(), schema, tableName) ) {
                throw new ConfigurationException("Table "+tableName+" does not exist in "+schema+". Add it manually or create a fresh database to get it created automatically");
            }
        }

        // Notify success
        log.debug("Database ready");
        return databaseConnection;

    }

    // Sets user default schema to given one, needed to make queries independent from schema name
    private void setUserDefaultSchema(Connection connection, String user, String schema) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("ALTER ROLE "+user+" SET search_path TO "+schema);
        stmt.execute();
    }

    // Checks is given scheme exists in database
    private boolean isSchemaPresent(Connection connection, String schemaName) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT schema_name FROM information_schema.schemata WHERE schema_name = ?");
        stmt.setString(1, schemaName);
        return stmt.executeQuery().next();
    }

    // Checks is given table exists in schema
    private boolean isTablePresent(Connection connection, String schemaName, String tableName) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(" SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_name = ?");
        stmt.setString(1, schemaName);
        stmt.setString(2, tableName);
        return stmt.executeQuery().next();
    }

    // Checks if any table exist in schema
    private boolean isAnyTableInSchema(Connection connection, String schemaName) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(" SELECT table_name FROM information_schema.tables WHERE table_schema = ?");
        stmt.setString(1, schemaName);
        return stmt.executeQuery().next();
    }

    // Run the initialisation sql script
    private void initDatabase(Connection connection) throws IOException, SQLException {
        // Get file and split lines by ';'
        String[] inst = Utils.getFileAsString("db_init.sql").split(";");
        // Create new statement
        Statement st = connection.createStatement();
        for (String anInst : inst) {
            // Skip empty statements
            if (!anInst.trim().equals("")) {
                st.executeUpdate(anInst);
                log.debug(anInst);
            }
        }
    }
}
