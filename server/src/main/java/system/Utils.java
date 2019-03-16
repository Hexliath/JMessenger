package system;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Utils {
    protected static final Logger log = new Logger(Utils.class);

    public static String UUID_REGEX = "[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}";
    public static String UUID_PATTERN = "\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}";

    public static Map<String, String> getConfigMap() {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> configMap;
        TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {};
        try {
            configMap = mapper.readValue(getFileAsString("app_config.json"), typeRef);
        } catch (IOException e) {
            log.error(e);
            return Collections.emptyMap();
        }
        return configMap;
    }

    public static String getFileAsString(String file) throws IOException {
        ClassLoader classLoader = Utils.class.getClassLoader();
        //File file = new File(classLoader.getResource("app_config.json").getFile());

        InputStream stream = classLoader.getResourceAsStream(file);
        String output = convertStreamToString(stream);
        stream.close();

        return output;

    }

    public static String convertStreamToString(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
