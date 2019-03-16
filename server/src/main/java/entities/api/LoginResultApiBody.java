package entities.api;

import entities.enums.EnumCustomErrorCode;

import java.util.UUID;

public class LoginResultApiBody extends GenericApiEntity {

    private EnumCustomErrorCode code;
    private boolean success;
    private int attempt;
    private boolean locked;
    private String token;
    private String message;
    private UUID id;

    public LoginResultApiBody(EnumCustomErrorCode code, boolean success, int attempt, boolean locked, String token, UUID id) {
        this.code = code;
        this.success = success;
        this.attempt = attempt;
        this.locked = locked;
        this.token = token;
        this.id = id;
    }

    public LoginResultApiBody(EnumCustomErrorCode code, boolean success, int attempt, boolean locked, String token, UUID id, String message) {
        this.code = code;
        this.success = success;
        this.attempt = attempt;
        this.locked = locked;
        this.token = token;
        this.id = id;
        this.message = message;
    }

    public EnumCustomErrorCode getCode() {
        return code;
    }

    public void setCode(EnumCustomErrorCode code) {
        this.code = code;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getAttempt() {
        return attempt;
    }

    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
