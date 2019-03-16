package model;

import lib.utils;
import org.apache.commons.lang.StringEscapeUtils;

import java.net.MalformedURLException;
import java.net.URL;

public class Connection {
    private boolean connected;
    private String ip;
    private String url = new String();
    private String token = "";
    private Config config;


    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }


    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }



    public String getUrl(boolean host) {
        if(host){
            try {
                final URL tmp_url = new URL(url);
                utils.dbg("HOST" + tmp_url.getHost());
                return tmp_url.getHost();
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return url + ":" + config.getApi() + "/api/v1";
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
