package entities.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import entities.enums.EnumCustomErrorCode;
import entities.enums.EnumHttpCode;
import exceptions.ProcessingException;
import handlers.AuthorizationHandler;
import system.Logger;

/**
 * Abstract Api Entity class
 * This class adds to all of its children the ability to map their fields to json string
 */
public abstract class GenericApiEntity {
    protected static final Logger log = new Logger(AuthorizationHandler.class);

    // Object -> String
    // Only public access fields (or via public getter)
    public String toJson() throws ProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.error(e);
            throw new ProcessingException(EnumCustomErrorCode.SERVER_ERROR, "The server was unable to map entity "+this.getClass().getSimpleName());
        }
    }

}
