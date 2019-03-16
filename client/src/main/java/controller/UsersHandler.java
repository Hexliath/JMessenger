package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import lib.utils;
import model.Channel;
import model.User;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class UsersHandler {

    private ConnectionHandler conn;
    private LocalCacheHandler cache;
    private String path = "users";

    public UsersHandler(ConnectionHandler conn, LocalCacheHandler cache) throws IOException {
        this.conn = conn;
        this.cache = cache;
        this.updateUsers();
    }


    public void updateUsers() throws IOException {
        String result = conn.getRequest("",path + "/search", true);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

        try {
            List<User> users = mapper.readValue(result, mapper.getTypeFactory().
                    constructCollectionType(List.class, User.class));
            for(User user: users){
                String name = user.getDisplayName();
                String login = user.getLogin();
                String id = user.getId();

                if(cache.count(String.format("SELECT COUNT(*) AS COUNT FROM users WHERE id='%s'",id) ) ==  1){
                    cache.update(String.format("UPDATE users SET display_name = '%s', login = '%s' WHERE id = '%s'",
                            name,login, id));
                }
                else{
                    cache.insert(String.format("INSERT INTO users (id, display_name, login) VALUES ('%s', '%s', '%s');",
                            id, name,login));

                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Get all users
     * @param connected specify if users have to be connected
     * @return users list
     */
    public List<User> getAll(boolean connected){
        List<User> users = new ArrayList<>();
        ResultSet rs;
        if(connected){
             rs = cache.select(String.format("SELECT * FROM users WHERE connected = '%s'",connected));
        }
        else {
             rs = cache.select("SELECT * FROM users");
        }

        try {
            while (rs.next()) {
                User tmp = new User();
                tmp.setId(rs.getString("id"));
                tmp.setDisplayName(rs.getString("display_name"));
                tmp.setLogin(rs.getString("login"));
                users.add(tmp);
            }
        }
        catch (Exception e){
            utils.error("Error while getting  user.");
            e.printStackTrace();
        }
        return users;
    }







}
