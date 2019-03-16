package exceptions;

import entities.enums.EnumCustomErrorCode;
import entities.enums.EnumHttpCode;

public class ProcessingException extends Exception {

    private EnumCustomErrorCode error;
    private EnumHttpCode httpCode;

    public ProcessingException(String message) {
        super(message);
    }

    public ProcessingException(EnumCustomErrorCode error) {
        super("Server failed to resolve this query with no explicit reason given. Try again later");
        this.error = error;
    }

    public ProcessingException(EnumCustomErrorCode error, String message) {
        super(message);
        this.error = error;
    }

    public ProcessingException(EnumCustomErrorCode error, String message, EnumHttpCode httpCode) {
        super(message);
        this.error = error;
        this.httpCode = httpCode;
    }

    public EnumCustomErrorCode getCustomErrorCode() {
        return error;
    }

    public boolean hasHttpCode() {
        return httpCode != null;
    }

    public EnumHttpCode getHttpCode() {
        return httpCode;
    }
}
