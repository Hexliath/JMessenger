package entities.api;

public class AuthorizationPushBody extends GenericApiEntity {

    private String token;

    public AuthorizationPushBody() {}

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
