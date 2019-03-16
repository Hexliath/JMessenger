package controller;

import lib.utils;
import model.Config;
import model.Connection;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.*;


public class ConnectionHandler {

//Attributs
    private Connection conn = new Connection();
    private final String USER_AGENT = "Mozilla/5.0";
    public void setToken(String token){
        conn.setToken(token);
    }

//Methods

    /**
     * Set the server address
      * @param config server config
     */
    public void setServer(Config config){
        utils.dbg("config =>" + config.getUrl());
        conn.setUrl(config.getUrl());
        conn.setConfig(config);
        try{
            String result = getRequest("","ping", false);
            if(result.contains("pong")) {
                conn.setConnected(true);
                utils.dbg("CONNECTED");
            }
            else{
                conn.setConnected(false);
            }
        }
        catch (Exception e){
            conn.setConnected(false);
        }
    }

    public String getPushURL(){
        return conn.getUrl(true);
    }

    /**
     * Verify if the API is online
     * @return true if connection is alive
     */
    public boolean isConnected(){
        return conn.isConnected();
    }



    /**
     * Make a PATCH request to the API
     * @param data the json string to send
     * @param path the targeted path
     * @param token_required true if authentication required
     * @return String containing the request result
     * @throws IOException
     * @throws NullPointerException
     */
    public String patchRequest(String data, String path, boolean token_required) throws Exception {

        Field methodsField = HttpURLConnection.class.getDeclaredField("methods");
        methodsField.setAccessible(true);
        // get the methods field modifiers
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        // bypass the "private" modifier
        modifiersField.setAccessible(true);

        // remove the "final" modifier
        modifiersField.setInt(methodsField, methodsField.getModifiers() & ~Modifier.FINAL);

        /* valid HTTP methods */
        String[] methods = {
                "GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE", "PATCH"
        };
        // set the new methods - including patch
        methodsField.set(null, methods);


        StringBuffer response = new StringBuffer();

        utils.dbg("My data" + data);


        URL obj = new URL(conn.getUrl(false) + "/" + path);
        utils.dbg(conn.getUrl(false) + "/" + path);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        //add reuqest header
        con.setRequestMethod("PUT");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setRequestProperty("Authorization", conn.getToken());
        try {


            utils.dbg(data);
            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.write(data.getBytes());
            wr.flush();
            wr.close();


            int responseCode = con.getResponseCode();
            utils.dbg("\nSending 'PAST' request to URL : " + conn.getUrl(false) + "/" + path);
            utils.dbg("Patch parameters : " + data);
            utils.dbg("Response Code : " + responseCode);

            InputStream _is;
            if (con.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
                _is = con.getInputStream();
            } else {
                /* error from server */
                _is = con.getErrorStream();
            }
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(_is));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        }
        catch(IOException e){
            if(e.toString().contains("429")){
                utils.error("Error with beeceptor. Limit reached.");
            }

        }
        utils.dbg(response.toString());
        return response.toString();

    }

    /**
     * Make a GET request to the API
     * @param data the json string to send
     * @param path the targeted path
     * @param token_required true if authentication required
     * @return String containing the request result
     * @throws IOException
     * @throws NullPointerException
     */
    public String getRequest(String data, String path, boolean token_required) throws IOException,NullPointerException  {



        StringBuffer response = new StringBuffer();
        try {
            URL obj = new URL(conn.getUrl(false) + "/" + path);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            //add request header
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Authorization", conn.getToken());


            int responseCode = con.getResponseCode();
            utils.dbg("\nSending 'GET' request to URL : " + conn.getUrl(false) + "/" + path);
            utils.dbg("Response Code : " + responseCode);
            InputStream _is;
            if (con.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
                _is = con.getInputStream();
            } else {
                /* error from server */
                _is = con.getErrorStream();
            }

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(_is));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        }
             catch(IOException e){
                if(e.toString().contains("429")){
                    utils.error("Error with beeceptor. Limit reached.");
                }

        }
        return response.toString();
    }

    /**
     * Get the token
     * @return String containing the token
     */
    public String seeToken(){
       return conn.getToken();
    }



    /**
     * Make a POST request to the API
     * @param data the json string to send
     * @param path the targeted path
     * @param token_required true if authentication required
     * @return String containing the request result
     * @throws IOException
     * @throws NullPointerException
     */
    public String postRequest(String data, String path, boolean token_required) throws NullPointerException, IOException {


        StringBuffer response = new StringBuffer();


        URL obj = new URL(conn.getUrl(false) + "/" + path);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Content-Type", "text/html");
        con.setRequestProperty("Authorization", conn.getToken());

        try {
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.write(data.getBytes());
            wr.flush();
            wr.close();
            int responseCode = con.getResponseCode();
            utils.dbg("\nSending 'POST' request to URL : " + conn.getUrl(false) + "/" + path);
            utils.dbg("Post parameters : " + data);
            utils.dbg("Response Code : " + responseCode);

            InputStream _is;
            if (con.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
                _is = con.getInputStream();
            } else {
                /* error from server */
                _is = con.getErrorStream();
            }

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(_is));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        }
        catch(IOException e){
            if(e.toString().contains("429")){
                utils.error("Error with beeceptor. Limit reached.");
            }

        }
        utils.dbg(response.toString());
        return response.toString();

    }

}
