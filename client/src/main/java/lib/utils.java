package lib;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import controller.ChannelsHandler;
import controller.ClientHandler;
import controller.ConnectionHandler;
import model.Client;

import java.io.IOException;
import java.nio.charset.Charset;

public class utils {

    public static void dbg(String text){
        System.out.println("dbg:: " + text);
    }
    public static void error(String text){
        System.out.println("error:: " + text);
    }

    public static void getInfos(ClientHandler client, ChannelsHandler channel, ConnectionHandler conn){
        System.out.println("\n---------- INFOS --------------");
        System.out.println("Client name : "  + client.getClient().getDisplayName());
        System.out.println("Token : " + conn.seeToken());
        System.out.println("Actual channel : " + channel.getChannel().getName() + ", " + channel.getChannel().getId());
        System.out.println("-----------------------------");
    }
    public static String escapeString(String str) {
        String data = null;
        if (str != null && str.length() > 0) {
            str = str.replace("\\", "\\\\");
            str = str.replace("'", "\\'");
            str = str.replace("\0", "\\0");
            str = str.replace("\n", "\\n");
            str = str.replace("\r", "\\r");
            str = str.replace("\"", "\\\"");
            str = str.replace("\\x1a", "\\Z");
            data = str;
        }
        return data;
    }


    /**
     * Create the json representation of an object
     * @param obj The object to turn into json
     * @return String containing the json result
     */
    public static  String createJson(Object obj){

        String jsonInString = new String("");

        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        try {
            jsonInString =mapper.writeValueAsString(obj);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return jsonInString;
    }


    /**
     *
     */
    public static Object parseJson(String json, Object obj){
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);


        try {
            obj =  mapper.readValue(json, obj.getClass());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return obj;
    }

}


