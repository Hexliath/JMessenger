import javafx.application.Application;
import view.LoginPanel;


public class Main {

    public static void main(String[] args) throws Exception {
        System.out.print("JMessenger client. v0.1. Welcome !");
        Application.launch(LoginPanel.class, args);
    }
}