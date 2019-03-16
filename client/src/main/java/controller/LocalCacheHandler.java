package controller;

import lib.utils;
import model.Config;
import model.LocalCache;
import view.LocalCacheView;

import javax.swing.plaf.nimbus.State;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class LocalCacheHandler {
    private Connection conn = null;

    private LocalCache cache = new LocalCache();
    private LocalCacheView view = new LocalCacheView();

    public LocalCacheHandler() {
        cache.setDb_url("jdbc:sqlite:cache.db");
        this.connect();
        createTables();
    }

    /**
     * Verify if config exists and accurate. Else, create it.
     */
    public void checkConfig(){
        if(!(count("SELECT COUNT(*) AS COUNT FROM configuration") == 1)){
            String request = "DELETE FROM configuration";
            utils.dbg(request);
            Statement sql = null;
            try {
                connect();
                sql = conn.createStatement();
                sql.executeUpdate(request);
                insert("INSERT INTO configuration (id, url,server_name,api,push) VALUES (1,'http://game-box.pl/', 'jmessenger_default',6100,6101)");
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
        disconnect();
    }

    /**
     * If another user is connecting or server configuration is modified, this function reset the local database
     * @return true if reset was successful
     */
    public boolean reset() {
        connect();
        boolean success = true;
        List<String> tables = new ArrayList<>();
        tables.add("messages");
        tables.add("users");
        tables.add("channels");
        tables.add("client");

        for(String table: tables) {
            String request = String.format("DELETE FROM %s;", table);
            utils.dbg(request);
            Statement sql = null;
            try {
                sql = conn.createStatement();
                sql.executeUpdate(request);
                sql.close();

            } catch (SQLException e) {
                e.printStackTrace();
                success = false;
            }
        }
        disconnect();
        return success;
    }

    /**
     * Deletes a row in a table
     * @param table the selected table
     * @param id identifier of the row
     * @return true if operation succeeded
     */
    public boolean delete(String table, String id){
        connect();
        boolean success = true;

            String request = String.format("DELETE FROM %s WHERE id = '%s';", table, id);
            utils.dbg(request);
            Statement sql = null;
            try {
                sql = conn.createStatement();
                sql.executeUpdate(request);
                sql.close();

            } catch (SQLException e) {
                e.printStackTrace();
                success = false;
            }
        disconnect();
        return success;

    }

    /**
     * Get and return the config from the database
     * @return config
     */
    public Config getConfig() {
            Config config = new Config();
            ResultSet rs = select("SELECT * FROM configuration WHERE id='1'");
            try {
                while (rs.next()) {
                    config.setLast_channel_id(rs.getString("last_channel_id"));
                   config.setServer_name(rs.getString("server_name"));
                   config.setDefaultUsername(rs.getString("default_username"));
                   config.setApi(rs.getString("api"));
                   config.setPush(rs.getString("push"));
                    config.setUrl(rs.getString("url"));
                }
                rs.close();
            }
            catch(Exception e){
                e.printStackTrace();
                utils.error("Error while getting the configuration");
            }
            disconnect();
            return config;
    }


    /**
     * Allows to save the config in the local database
     * @param config The config to save
     */
    public void setConfig(Config config){
        if(config.getDefaultUsername() == null){
            config.setDefaultUsername("");
        }
        try {
            update(String.format("UPDATE configuration SET default_username = '%s', url = '%s', api = '%s', push = '%s' WHERE id=1",
                    config.getDefaultUsername(),
                    config.getUrl(),
                    config.getApi(),
                    config.getPush()
            ));
        }
        catch(Exception e){
            utils.dbg("Error while setting server default configuration…");
        }
    }


    /**
     * Execute a select request
     * @param request SQL request with SELECT keyword
     * @return ResultSet containing the data
     */
    public ResultSet select(String request ) {
        connect();
        ResultSet rs = null;
        if (request.startsWith("SELECT")){

            utils.dbg(request);
            Statement sql = null;
            try {
                sql = conn.createStatement();
                rs = sql.executeQuery(request);


            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return rs;
    }

    /**
     * Close the connection to the database
     */
    public void disconnect(){
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the entries numbers in the table
     * @param request SQL request with COUNT keyword
     * @return total number of entries
     */
    public Integer count(String request){
        Integer number = -1;
        this.connect();
        if (request.startsWith("SELECT COUNT(*)")) {
            Statement sql = null;
            try {
                sql = conn.createStatement();
                ResultSet rs = sql.executeQuery(request);
                utils.dbg(request);

                while (rs.next()) {
                    number = rs.getInt(1);
                }
                rs.close();
                sql.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        disconnect();
        return number;
    }

    /**
     * Insert values inside a table
     * @param request SQL request with INSERT keyword
     * @return true if request was executed
     */
    public boolean insert(String request){
        connect();
        boolean success = false;
        if (request.startsWith("INSERT")){

            utils.dbg(request);
            Statement sql = null;
            utils.dbg(request);
            try {
                sql = conn.createStatement();
                sql.executeUpdate(request);
                sql.close();
                success = true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        disconnect();
        return success;
    }


    /**
     * Update values inside a table
     * @param request SQL request with UPDATE keyword
     * @return true if request was executed
     */
    public boolean update(String request){
        connect();
        boolean success = false;
        if (request.startsWith("UPDATE")){

            utils.dbg(request);
            Statement sql = null;
            try {
                sql = conn.createStatement();
                sql.executeUpdate(request);
                sql.close();
                success = true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
            disconnect();
        }
        return success;
    }



    private void createTables() {
        connect();
        List<String> requests = new ArrayList<String>();

        requests.add("CREATE TABLE IF NOT EXISTS client (\n"
                + "	id text PRIMARY KEY,\n"
                + "	display_name text NOT NULL,\n"
                + " login text NOT NULL,\n"
                + " avatar text,\n"
                + " token text\n"
                + ");");

        requests.add("CREATE TABLE IF NOT EXISTS users (\n"
                + "	id text PRIMARY KEY,\n"
                + "	display_name text NOT NULL,\n"
                + " login text NOT NULL,\n"
                + " avatar text,\n"
                + " connected boolean\n"
                + ");");

        requests.add("CREATE TABLE IF NOT EXISTS logs (\n"
                + "	id text PRIMARY KEY,\n"
                + "	category text NOT NULL,\n"
                + " description text NOT NULL,\n"
                + "	date text\n"
                + ");");

        requests.add("CREATE TABLE IF NOT EXISTS configuration (\n"
                + "	id int PRIMARY KEY,\n"
                + "	url text NOT NULL,\n"
                + "	server_name text NOT NULL,\n"
                + " last_channel_id text,\n"
                + " api text,\n"
                + " push text,\n"
                + " default_username text\n"
                + ");");

        requests.add("CREATE TABLE IF NOT EXISTS channels (\n"
                + "	id text PRIMARY KEY,\n"
                + "	name text NOT NULL,\n"
                + " role text NOT NULL,\n"
                + " joined boolean NOT NULL,\n"
                + " type text NOT NULL,\n"
                + "	joined_date timestamp,\n"
                + " created_date timestamp\n"
                + ");");

        requests.add("CREATE TABLE IF NOT EXISTS bookmarks (\n"
                + "	id text PRIMARY KEY,\n"
                + "	name text NOT NULL\n"
                + ");");

        requests.add("CREATE TABLE IF NOT EXISTS messages (\n"
                + "	id text PRIMARY KEY,\n"
                + "	author text NOT NULL,\n"
                + "	content text NOT NULL,\n"
                + "	channel_id text NOT NULL,\n"
                + "foreign key(author) references users,\n"
                + "foreign key(channel_id) references channels\n"
                + ");");


        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            for(String sql: requests){
                //utils.dbg(sql);
                stmt.execute(sql);
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        disconnect();
            }


    /**
     * Conect to the database
     */
    public void connect() {
        try {
            conn = DriverManager.getConnection(cache.getDb_url());
            view.connected();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }


}

