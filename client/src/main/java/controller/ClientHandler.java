package controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lib.utils;
import model.Client;
import model.Connection;
import view.ClientView;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ClientHandler {
    private Client client = new Client();
    private ClientView view = new ClientView();
    private ConnectionHandler conn;
    private LocalCacheHandler cache;
    private String path = "auth";

    public ClientHandler(ConnectionHandler conn, LocalCacheHandler cache) {
        this.conn = conn;
        this.cache = cache;
        init();
    }

    private void init(){
    view.show(client);
    }

    /**
     * Try to athenticate the client with the given credentials
     * @param login client’s identifier
     * @param password client's access key
     * @return Message containing the result
     * @throws Exception
     */
    public String authenticate(String login, String password) throws Exception {
        String message = "NOK";
        String data = "{\n" +
                "  \"login\": \""+ utils.escapeString(login)+"\",\n" +
                "  \"password\": \"" + utils.escapeString(password) + "\"\n" +
                "}\n";
        try {
            String result = conn.postRequest(data, path + "/login", false);
            if(!(result.isEmpty())) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode actualObj = mapper.readTree(result);
                if (actualObj.get("locked").toString() == "false") {
                    if (actualObj.get("success").toString() == "true") {
                        utils.dbg(result);
                        String id = (actualObj.get("id").textValue());
                        client.setId(id);
                        if(actualObj.get("message").toString().contains("sessions limit")){
                            ResultSet rs = cache.select(String.format("SELECT token,id from client where id='%s'",client.getId()));
                            while(rs.next()){
                                String token = rs.getString("token");
                               conn.setToken(token);
                               utils.dbg("token : " + token);
                            }
                        }
                        else{
                        conn.setToken(actualObj.get("token").textValue());
                        }

                        result = conn.getRequest("", "users/me", true);
                        actualObj = mapper.readTree(result);
                        utils.dbg(result);

                        utils.dbg(id);

                        client.setDisplayName((actualObj.get("display_name").textValue()));
                        client.setLogin((actualObj.get("login").textValue()));


                        if(!verifyUserHaveEverBeenLogged(id)){
                            createLocalClientBackup();
                        }

                        cache.update(String.format("UPDATE client SET token = ('%s') WHERE id = '%s'",
                                conn.seeToken(),client.getId()));




                        message = "OK";
                    } else {
                        message = actualObj.get("attempt").toString();
                    }
                }
                else{
                    message = "locked";
                }
            }

        } catch (IOException e) {
            utils.error("Connection to servor error !");
        }
        catch (NullPointerException e){
            utils.error("Server error.");
        }
        return message;
    }


    private void createLocalClientBackup(){
        cache.reset();
        cache.insert(String.format("INSERT INTO client (id,display_name,login) VALUES ('%s', '%s', '%s')",
                client.getId(), client.getDisplayName(),client.getLogin()));

    }

    private boolean verifyUserHaveEverBeenLogged(String client_id){
        boolean already_logged = false;
        try{
        ResultSet rs = cache.select(String.format("SELECT * FROM client WHERE id='%s'",client_id));
        while(rs.next()){
                if((rs.getString("id")).equals(client_id)){
                    already_logged = true;
                }
        }
        rs.close();

        }
        catch (Exception e){
            e.printStackTrace();
            utils.error("Error while verifying if user is already in the local database");

        }
        return already_logged;
    }



    public String register(String login, String password){
        String msg = "NOK";
        String data = "{\n" +
                "  \"login\": \""+ utils.escapeString(login)+"\",\n" +
                "  \"password\": \"" + utils.escapeString(password) + "\"\n" +
                "}\n";
        try {
            String result = conn.postRequest(data, path + "/register", false);
            if(result.contains("display_name")){
                msg = "OK";
            }
            else if(result.contains("ALREADY_EXIST")){
                msg = "ALREADY_EXIST";
            }
            utils.dbg(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return msg;
    }


//
//    public boolean modifyName(String new_name){
//        try {
//            conn.postRequest("","users/me", true);
//            cache.update(String.format("UPDATE users SET display_name = '%s' WHERE id='%s'",new_name,getClient().getId()));
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return true;
//    }



    public Client getClient(){
        return client;
    }


    /**
     * De-auth the client
     * @param everywhere If true, kill all open sessions for the client
     */
    public void logout(boolean everywhere){
        String complete_path = "/logout";
        if(everywhere){
            complete_path = complete_path + "/all";
        }
        try {
            conn.postRequest("",path + complete_path, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
