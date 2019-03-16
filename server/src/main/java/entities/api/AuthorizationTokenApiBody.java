package entities.api;

import java.util.Date;

public class AuthorizationTokenApiBody extends GenericApiEntity {

    private String token;
    private Date validity;

    public AuthorizationTokenApiBody(String token, Date validity) {
        this.token = token;
        this.validity = validity;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getValidity() {
        return validity;
    }

    public void setValidity(Date validity) {
        this.validity = validity;
    }
}
