package model;

import controller.*;
import lib.utils;
import sun.applet.Main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UI {
    private ClientHandler client;
    private LocalCacheHandler cache = new LocalCacheHandler();
    private ConnectionHandler conn =  new ConnectionHandler();
    private UsersHandler users_handler;
    private ChannelsHandler channel_handler;



    public UI(){
        utils.dbg("A new one");
    }
    public void setUp(){
        conn.setServer(cache.getConfig());
        client = new ClientHandler(conn,cache);
    }

    public void setMainInterface() throws IOException {
        channel_handler = new ChannelsHandler(conn, cache, client.getClient().getId());
        users_handler = new UsersHandler(conn,cache);


    }

    public ChannelsHandler getChannelsHandler() {
        return channel_handler;
    }

    public void setChannelsHandler(ChannelsHandler channel_handler) {
        this.channel_handler = channel_handler;
    }

    public ClientHandler getClient() {
        return client;
    }

    public void setClient(ClientHandler client) {
        this.client = client;
    }

    public LocalCacheHandler getCache() {
        return cache;
    }

    public void setCache(LocalCacheHandler cache) {
        this.cache = cache;
    }

    public ConnectionHandler getConn() {
        return conn;
    }

    public void setConn(ConnectionHandler connection) {
        this.conn = connection;
    }

    public UsersHandler getUsers_handler() {
        return users_handler;
    }


}
