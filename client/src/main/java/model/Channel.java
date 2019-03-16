package model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

public class Channel {
    private  String type;
    private String name = new String();


    @JsonIgnore
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Map<User,Rights> righs_and_members = new HashMap<>();

    @JsonIgnore
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<Map<User,String>> members = new ArrayList<>();
    private String id;
    private String join_time = "";
    private User owner;
    private boolean joined;

    @JsonIgnore
    public List<Map<User, String>> getMembers() {
        return members;
    }

    @JsonIgnore
    public void setMembers(List<Map<User, String>> members) {
        this.members = members;
    }

    public boolean isJoined() {
        return joined;
    }

    public void setJoined(boolean joined) {
        this.joined = joined;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getJoinTime() {
        return join_time;
    }

    public void setJoinTime(String join_time) {
        this.join_time = join_time;
    }

    public enum Type {
        PUBLIC ("Public channel"),
        PRIVATE ("Private channel"),
        GROUP ("Group channel");
        private String typename = "";
        Type(String name){
            this.typename = name;
        }
        public String toString(){
            return typename;
        }
    }


    public String getType() {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public ArrayList<ChannelMember> getRightsAndMembers() {
        ArrayList<ChannelMember> array = new ArrayList<ChannelMember>();
        String members_string = "";
        for(Map.Entry<User, Rights> entry : righs_and_members.entrySet()) {
            User cle = entry.getKey();
            Rights valeur = entry.getValue();
            ChannelMember member = new ChannelMember(cle,valeur);
            array.add(member);
        }
        return array;
    }
    @JsonIgnore
    public void setRighsAndMembers(Map<User, Rights> members) {
        if(this.righs_and_members.isEmpty()) {
            this.righs_and_members = members;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    class ChannelMember{
        private User user;
        private Rights role;

        public ChannelMember(User user, Rights role) {
            this.user = user;
            this.role = role;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public Rights getRole() {
            return role;
        }

        public void setRole(Rights role) {
            this.role = role;
        }
    }
}


