package entities.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

public class UserApiBody extends GenericApiEntity {

    private UUID id;
    private String login;
    private String displayName;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean online;

    public UserApiBody(UUID id, String login, String displayName) {
        this.id = id;
        this.login = login;
        this.displayName = displayName;
    }

    public UserApiBody(UUID id, String login, String displayName, boolean online) {
        this.id = id;
        this.login = login;
        this.displayName = displayName;
        this.online = online;
    }

    public UserApiBody() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Boolean isOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }
}
