package entities.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import entities.enums.EnumChannelType;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ChannelApiBody extends GenericApiEntity {

    private UUID id;
    private String name;
    private EnumChannelType type;
    private List<ChannelUserApiBody> members;
    private UserApiBody owner;
    private Date joinTime;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean joined;

    public ChannelApiBody(UUID id, String name, EnumChannelType type, List<ChannelUserApiBody> members, UserApiBody owner, Date joinTime) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.members = members;
        this.owner = owner;
        this.joinTime = joinTime;
    }

    public ChannelApiBody(UUID id, String name, EnumChannelType type, List<ChannelUserApiBody> members, UserApiBody owner, Date joinTime, Boolean joined) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.members = members;
        this.owner = owner;
        this.joinTime = joinTime;
        this.joined = joined;
    }

    public ChannelApiBody() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EnumChannelType getType() {
        return type;
    }

    public void setType(EnumChannelType type) {
        this.type = type;
    }

    public List<ChannelUserApiBody> getMembers() {
        return members;
    }

    public void setMembers(List<ChannelUserApiBody> members) {
        this.members = members;
    }

    public UserApiBody getOwner() {
        return owner;
    }

    public void setOwner(UserApiBody owner) {
        this.owner = owner;
    }

    public Date getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(Date joinTime) {
        this.joinTime = joinTime;
    }

    public Boolean getJoined() {
        return joined;
    }

    public void setJoined(Boolean joined) {
        this.joined = joined;
    }
}
