package controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import lib.utils;
import model.*;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.IOException;
import java.lang.reflect.Executable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import static javafx.application.Platform.exit;
import static javafx.application.Platform.runLater;

public class ChannelsHandler {

    private ConnectionHandler conn;
    private LocalCacheHandler cache;
    private Channel channel;
    private String path = "channels";
    private String client_id;
    private MessagesHandler messages_handler;

    public ChannelsHandler(ConnectionHandler conn, LocalCacheHandler cache, String client_id) {
        this.conn = conn;
        this.cache = cache;
        messages_handler = new MessagesHandler(conn,cache);
        this.client_id = client_id;
        select_start_channel();
    }



    private void select_start_channel(){
        updateChannels();
//        utils.dbg(((Boolean) cache.get_config().getLast_channel_id().isEmpty()).toString());
        boolean empty_config = true;
        try{
            empty_config = cache.getConfig().getLast_channel_id().isEmpty();
        }
        catch (Exception e){
            empty_config = false;

        }
        if(empty_config == false){
            String id = cache.getConfig().getLast_channel_id();
            if(cache.count(String.format("SELECT COUNT(*) AS COUNT FROM channels WHERE id='%s'",
                    id)) == 1){
                set(id);
            }
            else{
                try {
                    for(Channel c:  getAll(Channel.Type.PUBLIC)){
                        if(c.getName().contains("General")){
                            utils.dbg("Is joined" + c.getName());
                            set(c.getId());
                            break;
                        }
                    }
                }
                catch (NoSuchElementException e){
                    utils.error("No default channel… Server error.");
                    System.exit(1);
                    }
            }
         }
        else{
            try {
                String firt_public_channel_id = getAll(Channel.Type.PUBLIC).iterator().next().getId();
                set(firt_public_channel_id);
            }
            catch (NoSuchElementException e){
                utils.error("No default channel… Server error.");
                System.exit(1);
            }        }
        utils.dbg(channel.getName());
    }


    public String joinChannel(){
       String message = "OK";
        try{

           String result =  conn.postRequest("", "allocator/join/" + channel.getId(), true);
           if(result.contains("ALREADY_JOINED")){
               message = "ALREADY_JOINED";
           }
            cache.update(String.format("UPDATE channels SET joined ='%s' WHERE id='%s'", "true",channel.getId()));

        } catch (Exception e) {
            message = "NOK";
            e.printStackTrace();
        }
        return message;
    }

    public String leaveChannel(){
        String message = "OK";
        try{

           String result =  conn.postRequest("", "allocator/leave/" + channel.getId(), true);
           if(result.contains("PRIVILEGE_CONFLICT")){
               message = "PRIVILEGE_CONFLICT";
           }
           else {
               cache.update(String.format("UPDATE channels SET joined ='%s' WHERE id='%s'", "false", channel.getId()));
           }

        } catch (Exception e) {
            e.printStackTrace();
            message = "NOK";
        }
        return message;
    }


    public void removeUser(User user){
        try{

            conn.postRequest("", "allocator/kick-from/" + channel.getId(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addUser(User user){
        try{

        conn.postRequest("", "allocator/add-to/" + channel.getId(), true);
    } catch (Exception e) {
        e.printStackTrace();
    }
    }

    public void modifyUserRights(User user){
        try {
//            Query example :
//            PATCH /roles/123e4567-e89b-12d3-a456-556642440000
//            {
//                "user": User
//                "role": ADMIN
//            }
            //TODO
            conn.patchRequest("", "roles/" + channel.getId(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void replicateRights(User receiver, Rights rights){
        try{
        conn.patchRequest("", "roles/" + channel.getId(), true);
    } catch (Exception e) {
        e.printStackTrace();
    }
        //TODO get client right and copy to another
    }

    public void editChannel(String name, User owner){
        try {
            Channel tmp_channel = this.channel;
            tmp_channel.setName(name);
            tmp_channel.setOwner(owner);

            tmp_channel.setType("PUBLIC");

            String result = utils.createJson(tmp_channel);


            conn.patchRequest(result, path + "/" + channel.getId(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gives the statistics of the actual channel
     * @return Stats model
     */
    public Stats getStats(){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Stats stats = new Stats();
        stats.setCreation_date(timestamp.toString());
        stats.setJoined_date(timestamp.toString());
        stats.setMessagesNumber(cache.count("SELECT COUNT(*) AS COUNT FROM messages").toString());
        stats.setChannelNumber(cache.count("SELECT COUNT(*) AS COUNT FROM channels").toString());
        utils.dbg(stats.getMessagesNumber());
        cache.disconnect();
        return stats;
    }


    /*
     * Create a new channel from given informations
     * @return : the created channel
     */
    public Channel add(String name, Map<User, Rights> users, Channel.Type type){
        Channel new_channel = new Channel();
        new_channel.setName(name);
        new_channel.setRighsAndMembers(users);
        new_channel.setType(type.name());
    try {
            String result = conn.postRequest(utils.createJson(new_channel), path, true);
            utils.dbg(result);
            if(!result.contains("UNAVAILABLE_CHANNEL_NAME")) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode actualObj = mapper.readTree(result);
                new_channel.setId(actualObj.get("id").textValue());
                new_channel.setJoinTime(actualObj.get("join_time").textValue());
                utils.dbg("ici");
                cache.insert(String.format("INSERT INTO channels (name, id, role, type) VALUES ('%s', '%s', 'OWNER', '%s');",
                        utils.escapeString(StringEscapeUtils.escapeJava(new_channel.getName())), new_channel.getId(), new_channel.getType()));
                cache.disconnect();
            }
            else{
                new_channel = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new_channel;
    }

    /**
     * Set the current channel and save it in the configuration for the next startup
     * @param uuid Channel’s id
     * @return true if the channel was set
     */
    public void set(String uuid){

        channel = new Channel();
        ResultSet rs = cache.select("SELECT * FROM channels WHERE id='" + uuid + "'");
        try {
            while(rs.next()){
                channel.setName(rs.getString("name"));
                channel.setId(rs.getString("id"));
            }
            cache.update(String.format("UPDATE configuration SET last_channel_id = '%s' WHERE id=1;", channel.getId()));


        } catch (SQLException e) {
            e.printStackTrace();
        }
        cache.disconnect();
    }

    public boolean addBookmarks(){
        List<Bookmark> bookmarks = getBookmarks();
        for(Bookmark b:bookmarks){
            if(b.getId().equals(channel.getId())){
                return false;
            }
        }
        cache.insert(String.format("INSERT INTO bookmarks (name, id) VALUES ('%s', '%s');",
                utils.escapeString(StringEscapeUtils.escapeJava(channel.getName())),channel.getId()));
        return true;
    }

    public void deleteBookmarks(){
            cache.delete("bookmarks",channel.getId());
    }

    public List<Bookmark> getBookmarks(){

        List<Bookmark> bookmarks = new ArrayList<>();
        ResultSet rs = cache.select("SELECT * FROM bookmarks");
        try {
            while (rs.next()) {
                Bookmark tmp = new Bookmark();
                tmp.setId(rs.getString("id"));
                tmp.setName(rs.getString("name"));
                bookmarks.add(tmp);
            }
        }
        catch (Exception e){
            utils.error("Error while getting bookmarks.");
        }
        return bookmarks;
    }


    /**
     * Get messages from database
     * @return the messages
     * @throws SQLException
     */
    public List<String> getMessages() throws SQLException {
        return messages_handler.getLocal(channel.getId());
    }





    public Channel getChannel(){
        return channel;
    }


    /**
     * Update the channels and saves into local database
     */
    public void updateChannels(){
        String result = null;
        try {
            result = conn.getRequest("",path, true);
            utils.dbg(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        utils.dbg(result);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);


        try {
            List<Channel> channels = mapper.readValue(result, mapper.getTypeFactory().
                    constructCollectionType(List.class, Channel.class));
            for(Channel ch: channels){
                String name = ch.getName();
                String type = utils.escapeString(ch.getType());
                String id = ch.getId();
                String join_time = ch.getJoinTime();
                Boolean joined = ch.isJoined();
                String role = "MEMBER";
                utils.dbg(joined.toString());
                try {
                    utils.dbg(client_id);
                    utils.dbg(ch.getOwner().getId());
                    utils.dbg(ch.getOwner().getDisplayName());

                  if(ch.getOwner().getId().equals(client_id)){
                      joined = true;
                      utils.dbg(joined.toString() + ch.getOwner().getDisplayName());
                      role = "OWNER";
                  }
                }
                catch (Exception e){

                }

                utils.dbg("final " + joined.toString());
                if(cache.count(String.format("SELECT COUNT(*) AS COUNT FROM channels WHERE id='%s'",id) ) ==  1){
                    cache.update(String.format("UPDATE channels SET name = '%s', type = '%s', role = '%s', joined ='%s' WHERE id='%s'",
                            name,type,role, joined.toString(),id));
                }
                else{

                    cache.insert(String.format("INSERT INTO channels (id, name, type, role, joined) VALUES ('%s', '%s', '%s', '%s', '%s');",
                            id, StringEscapeUtils.escapeSql(name),type, role, joined.toString()));

                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if user is member of the current channel
     * @return true if is member
     */
    public boolean isMember(){
        boolean member = false;
        ResultSet rs = cache.select("SELECT joined FROM channels WHERE id='" + channel.getId() + "'");
        try {
            while (rs.next()) {
                if(rs.getString("joined").equals("true")){
                    member = true;
                }
            }
        }
        catch(Exception e){

        }
        return member;
    }

    /**
     * Get the role of the client for this channel
     * @return the role
     */
    public String getRole(){
        String role = "";
        ResultSet rs = cache.select("SELECT role FROM channels WHERE id='" + channel.getId() + "'");
        try {
            while (rs.next()) {
                role = rs.getString("role");
            }
        }
        catch(Exception e){

        }
        return role;
    }


    /**
     * Get all channels
     * @param type the channels type to get
     * @return channels list
     */
   public List<Channel> getAll(Channel.Type type){
        List<Channel> channels = new ArrayList<>();
        ResultSet rs = cache.select(String.format("SELECT * FROM channels WHERE type = '%s'",type.name()));
        try {
            while (rs.next()) {
                Channel tmp = new Channel();
                tmp.setId(rs.getString("id"));
                tmp.setName(rs.getString("name"));
                channels.add(tmp);
            }
        }
        catch (Exception e){
            utils.error("Error while getting "+ type.name() + " channels.");
        }
        return channels;
    }

    public MessagesHandler getMessagesHandler() {
        return messages_handler;
    }

    /*
     * Check if locals channels are up to date
     * @return : False if any channel isn’t up to date
     */
    boolean compareLocalChannels(){
        return true;
    }

}
