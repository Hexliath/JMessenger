package controller;

import lib.utils;
import model.Channel;
import model.Client;
import model.Message;
import model.MessagesPool;

import java.sql.PreparedStatement;
import java.util.UUID;

public class SingleMessageHandler {

    Message message = new Message();
    String path = "messages";

    ConnectionHandler conn;
    LocalCacheHandler cache;


    public SingleMessageHandler(ConnectionHandler conn, LocalCacheHandler cache, String text, Channel channel, Client client) {
        this.conn= conn;
        this.cache = cache;
        message.setContent(text);
        message.setChannelId(channel.getId());
        message.setAuthor(client.getDisplayName());
    }

    /**
     * Tries to post the message
     */
    public String send(){
        String success = "NOK";
        try {
            String result = conn.postRequest(utils.createJson(message), path, true);
           if(result.contains(message.getContent().split("\n")[0])){
                    Object msg = utils.parseJson(result, message);
                    this.cache(((Message) msg));
                    success = "OK";
            }
            else if(result.contains("NOT_ENOUGH_PRIVILEGES")){
                success = "NOT_ENOUGH_PRIVILEGES";
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return success;
    }

    /**
     * Saves the message inside the local cache DB
     * @param msg the message to send
     * @return True if message was sent
     */
    private boolean cache(Message msg){
        try {
            String text = utils.escapeString(msg.getContent());
            String author = utils.escapeString(message.getAuthor());
            String id = utils.escapeString(message.getChannelId());
            String sql = String.format("INSERT INTO messages (author, content,channel_id) VALUES ('%s', '%s','%s');",
                    author, text, id);
            cache.insert(sql);
        }
        catch(Exception e){
            return false;
        }
        return true;
    }




}
