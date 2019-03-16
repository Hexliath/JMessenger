package system;

import exceptions.ConfigurationException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database connector
 * Provides database connection from given credentials
 */
public class DatabaseConnection {
    protected static final Logger log = new Logger(DatabaseConnection.class);

    private Connection connection;

    public DatabaseConnection(String host, int port, String database, String schema, String user, String password) throws ConfigurationException {
        Properties connectionProps = new Properties ();
        connectionProps.put("user", user);
        connectionProps.put("password", password);
        try {
            this.connection = DriverManager.getConnection(String.format(
                    "jdbc:postgresql://%s:%d/%s?currentSchema=%s",
                    host,
                    port,
                    database,
                    schema
            ), connectionProps);
        } catch (SQLException e) {
            log.error(e);
            throw new ConfigurationException("Unable to create database connection");
        }
    }

    public Connection get() {
        return connection;
    }
}
