package model;

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

public class Stats {
    private String registered_users;
    private String messages_number;
    private String creation_date;
    private String joined_date;
    private String channel_number;

    public String getRegistered_users() {
        return registered_users;
    }

    public void setRegistered_users(String registered_users) {
        this.registered_users = registered_users;
    }

    public String getMessagesNumber() {
        return messages_number;
    }

    public String getChannelNumber() {
        return channel_number;
    }

    public void setChannelNumber(String channel_number) {
        this.channel_number = channel_number;
    }

    public void setMessagesNumber(String messages_number) {
        this.messages_number = messages_number;
    }

    public String getCreation_date() {
        return creation_date;
    }

    public void setCreation_date(String creation_date) {
        this.creation_date = creation_date;
    }

    public String getJoined_date() {
        return joined_date;
    }

    public void setJoined_date(String joined_date) {
        this.joined_date = joined_date;
    }
}


