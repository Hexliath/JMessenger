package system;

import entities.enums.EnumLogLevel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger utility
 * Gives more control on System out streams, adds formatting, additional data and reporting levels to system messages.
 */
public class Logger {

    // ANSI colors for messages
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";
    private static final String ANSI_BLACK_BACKGROUND = "\u001B[40m";

    // Attached class
    private Class instanceClass;
    // Desired log level for this instance
    public static EnumLogLevel logLevel = EnumLogLevel.DEBUG;

    private final int CLASS_INFO_LENGTH = 36;

    // Constructor
    public Logger(Class instanceClass) {
        this.instanceClass = instanceClass;
    }

    // Returns formatted date string of current time
    private String getDate(){
        LocalDateTime today = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        return today.format(formatter);
    }

    // Returns canonical name of attached class
    private String getClassInfo(){
        return instanceClass.getCanonicalName();
    }

    // Returns the pattern for messages with places for color, type and message
    private String getPattern(){
        String classInfo = getClassInfo();
        int firstIndex = classInfo.length()-CLASS_INFO_LENGTH;
        if(firstIndex < 0) {
            classInfo = spaceString(classInfo, " ", -firstIndex);
            firstIndex = 0;
        }
        classInfo = classInfo.substring(firstIndex);
        String pattern = String.format("%s\t THISTHIS%s\t [%s%s%s]\t : THIS", getDate(), ANSI_RESET, ANSI_CYAN, classInfo, ANSI_RESET);
        return pattern.replace("THIS", "%s");
    }

    // Prints fatal error message
    public void fatal(String message){
        if(logLevel.ordinal() >=1) {
            System.out.println(
                    String.format(getPattern(), ANSI_RED + ANSI_BLACK_BACKGROUND, "FATAL", message)
            );
        }
    }

    // Prints error message
    public void error(String message){
        if(logLevel.ordinal() >=2) {
            System.out.println(
                    String.format(getPattern(), ANSI_RED, "ERROR", message)
            );
        }
    }

    // Prints stack trace
    public void error(Exception exception){
        if(logLevel.ordinal() >=2) {
            System.err.println(
                    String.format(getPattern(), ANSI_RED, "ERROR", exception.getClass().getCanonicalName())
            );
            exception.printStackTrace();
        }
    }

    // Prints warn message
    public void warn(String message){
        if(logLevel.ordinal() >=3) {
            System.out.println(
                    String.format(getPattern(), ANSI_YELLOW, "WARN", message)
            );
        }
    }

    // Prints warn stack trace
    public void warn(Exception exception){
        if(logLevel.ordinal() >=3) {
            System.err.println(
                    String.format(getPattern(), ANSI_YELLOW, "WARN", exception.getClass().getCanonicalName())
            );
            exception.printStackTrace();
        }
    }

    // Prints info message
    public void info(String message){
        if(logLevel.ordinal() >=4) {
            System.out.println(
                    String.format(getPattern(), ANSI_GREEN, "INFO", message)
            );
        }
    }

    // Prints debug message
    public void debug(String message){
        if(logLevel.ordinal() >=5) {
            System.out.println(
                    String.format(getPattern(), ANSI_YELLOW, "DEBUG", message)
            );
        }
    }

    // Prints debug stack trace
    public void debug(Exception exception){
        if(logLevel.ordinal() >=5) {
            System.err.println(
                    String.format(getPattern(), ANSI_YELLOW, "DEBUG", exception.getClass().getCanonicalName())
            );
            exception.printStackTrace();
        }
    }

    private static String spaceString(String input, String fill, int count) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            stringBuilder.append(fill);
        }
        stringBuilder.append(input);
        return stringBuilder.toString();
    }

    public void logo(String subTitle, String serverVersion, String apiVersion) {
        System.out.println(
                "  _____  _____ ______ _   _   ___   ___  __  ___  \n" +
                " |_   _|/ ____|  ____| \\ | | |__ \\ / _ \\/_ |/ _ \\ \n" +
                "   | | | (___ | |__  |  \\| |    ) | | | || | (_) |\n" +
                "   | |  \\___ \\|  __| | . ` |   / /| | | || |\\__, |\n" +
                "  _| |_ ____) | |____| |\\  |  / /_| |_| || |  / / \n" +
                " |_____|_____/|______|_| \\_| |____|\\___/ |_| /_/  "
        );
        System.out.println(String.format(
                "%-11s%s %s%s%s - API %s%s%s",
                "",
                subTitle,
                ANSI_GREEN,
                serverVersion,
                ANSI_RESET,
                ANSI_CYAN,
                apiVersion,
                ANSI_RESET

        ));
        System.out.println("\n\n");
    }

    public void spacing() {
        System.out.println();
    }
}
