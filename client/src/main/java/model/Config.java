package model;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

public class Config {
    private String url = "";
    private String server_name = "";
    private String last_channel_id = "";
    private String last_user_id = "";
    private String api = "";
    private String push = "";
    private String default_username = "";

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = StringEscapeUtils.escapeSql(api);
    }

    public String getPush() {
        return push;
    }

    public void setPush(String push) {
        this.push = StringEscapeUtils.escapeSql(push);
    }

    public String getDefaultUsername() {
        return default_username;
    }

    public void setDefaultUsername(String default_username) {
        this.default_username = StringEscapeUtils.escapeSql(default_username);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
       this.url = (StringUtils.removeEnd(url,"/"));
    }

    public String getServer_name() {
        return server_name;
    }

    public void setServer_name(String server_name) {
        this.server_name = server_name;
    }

    public String getLast_channel_id() {
        return last_channel_id;
    }

    public void setLast_channel_id(String last_channel_id) {
        this.last_channel_id = last_channel_id;
    }

    public String getLast_user_id() {
        return last_user_id;
    }

    public void setLast_user_id(String last_user_id) {
        this.last_user_id = last_user_id;
    }
}


