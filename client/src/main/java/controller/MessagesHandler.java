package controller;

import lib.utils;
import model.Channel;
import model.Client;
import model.Message;
import model.MessagesPool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MessagesHandler {

    private MessagesPool pool;


    ConnectionHandler conn;
    LocalCacheHandler cache;



    public MessagesHandler(ConnectionHandler conn, LocalCacheHandler cache) {
        this.conn= conn;
        this.cache = cache;
    }

    /**
     * Appends the specified message to the local database
     * @param message the message
     * @return
     */
    public String appendToLocal(Message message){
        String text = utils.escapeString(message.getContent());
        String author = utils.escapeString(message.getAuthor());
        String id = utils.escapeString(message.getChannelId());

        String sql = String.format("INSERT INTO messages (author, content,channel_id) VALUES ('%s', '%s','%s');",
                author,text,id);
        cache.insert(sql);
        return author + ": " + text;
    }


    /**
     * Get local messages for specific channel from the database
     * @param id the channel id
     * @return List containing messages
     * @throws SQLException
     */
    public List<String> getLocal(String id) throws SQLException {
        List<String> list = new ArrayList<String>();
        ResultSet rs = cache.select(String.format("SELECT * FROM messages WHERE channel_id = '%s';",id));
        while (rs.next()){
            list.add(rs.getString("author") + ":" + rs.getString("content"));
        }
        rs.close();
        cache.disconnect();
        return list;
    }


    /**
     * This function send the message to the right channel after some verifications
     * @return True if message has been moved to the right channel
     */
    private boolean dispatch(){
        return true;
    }



}
