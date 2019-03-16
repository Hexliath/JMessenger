package entities.api;

import entities.enums.EnumCustomErrorCode;
import entities.enums.EnumHttpCode;

public class DefaultApiResponse extends GenericApiEntity {

    private EnumHttpCode code;
    private String message;
    private EnumCustomErrorCode exception;

    public DefaultApiResponse(EnumHttpCode code, EnumCustomErrorCode exception, String message) {
        this.code = code;
        this.exception = exception;
        this.message = message;
    }

    public DefaultApiResponse(EnumCustomErrorCode exception, String message) {
        this.code = EnumHttpCode.CUSTOM;
        this.exception = exception;
        this.message = message;
    }

    public int getCode() {
        return code.getValue();
    }

    public void setCode(EnumHttpCode code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public EnumCustomErrorCode getException() {
        return exception;
    }

    public void setException(EnumCustomErrorCode exception) {
        this.exception = exception;
    }
}
