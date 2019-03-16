package entities;

import entities.api.AuthorizationTokenApiBody;

import java.util.UUID;

/**
 * User entity
 * For server internal user, contains UserApiBody fields and authorisation token.
 * Isn't supposed to be returned by api, so doesn't extend GenericApiEntity, no json mapping.
 */
public class User {

    private UUID id;
    private String login;
    private String displayName;
    private AuthorizationTokenApiBody authorizationToken;

    public User(UUID id, String login, String displayName, AuthorizationTokenApiBody authorizationToken) {
        this.id = id;
        this.login = login;
        this.displayName = displayName;
        this.authorizationToken = authorizationToken;
    }

    public User(UUID id) {
        this.id = id;
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

    public AuthorizationTokenApiBody getAuthorizationToken() {
        return authorizationToken;
    }

    public void setAuthorizationToken(AuthorizationTokenApiBody authorizationToken) {
        this.authorizationToken = authorizationToken;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", login='" + login + '\'' +
                ", displayName='" + displayName + '\'' +
                ", authorizationToken=" + authorizationToken +
                '}';
    }
}
