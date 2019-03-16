package entities.enums;

public enum EnumHttpCode {
    CUSTOM(0),
    OK(200),
    BAD_REQUEST(400),
    UNAUTHORISED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    INTERNAL_SERVER_ERROR(500),
    BAD_GATEWAY(502),
    SERVICE_UNAVAILABLE(503),
    GATEWAY_TIMEOUT(504);


    private final int value;

    EnumHttpCode(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
}
