package model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

public class User {
    private String login;
    private String id;
    private Boolean online;
    private String display_name;
    @JsonIgnore
    private boolean connected;


    public Boolean getOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public String getLogin() {
        return login;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return display_name;
    }

    public void setDisplayName(String display_name) {
        this.display_name = display_name;
    }
}
