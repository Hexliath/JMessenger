package view;


import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lib.utils;
import model.*;
import view.MainPanel;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginPanel extends Application implements Initializable {


    private GridPane root;
    private Stage primaryStage;

    private UI ui;

    @FXML    private TextField username_tf;
    @FXML    private PasswordField password_tf;
    @FXML    private Button login_btn;
    @FXML    private Label help_lbl;
    @FXML    private Label forgot_lbl;
    @FXML    private Label register_help_lbl;
    @FXML    private TextField register_username_tf;
    @FXML    private PasswordField register_password_tf;
    @FXML    private PasswordField register_password_verif_tf;
    @FXML    private Button register_btn;
    @FXML    private TextField api_port_tf;
    @FXML    private TextField push_port_tf;
    @FXML    private TextField default_username_tf;
    @FXML    private TextField server_url_tf;




    @FXML
    void handleButtonRegisterAction(ActionEvent event) {
        utils.dbg(ui.toString());
        String username = register_username_tf.getText();
        String password = register_password_tf.getText();
        String verif = register_password_verif_tf.getText();

        if (username.isEmpty() || password.isEmpty() || verif.isEmpty()) {
            register_help_lbl.setText("Please enter your informations !");
        }
        else if(!password.equals(verif)){
            register_help_lbl.setText("Passwords must match !");
        }
        else {
            utils.dbg(((Boolean)ui.getConn().isConnected()).toString());
            if (ui.getConn().isConnected()) {
                String result = ui.getClient().register(username,password);
                if (result== "OK") {
                    try {
                        Stage appStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                        appStage.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    if (result.contains("ALREADY_EXIST")) {
                        register_help_lbl.setText("This username is already used...");
                    }
                }
            }
            else{
                help_lbl.setText("You are offline, please turn on Internet or check the server confguration");
            }
        }
    }

    public LoginPanel() {
        ui = new UI();
        loadConfig();
        utils.dbg(ui.toString());
    }

    public void loadConfig(){
        ui.getCache().connect();
        ui.getCache().checkConfig();
        ui.setUp();
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("JMessenger -  Login");
        initRootLayout();

    }

    public void initRootLayout() {
        try {
            FXMLLoader loader = new FXMLLoader();
            Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("login.fxml"));
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handleLblRegister() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);

        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("registration.fxml"));
        try {
            Parent root = loader.load();
            dialog.setTitle("Registration");
            dialog.setScene(new Scene(root));
            dialog.show();
            help_lbl.setText("Successfully registered, please log in !");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    private void handleButtonCancelButton(ActionEvent event){
        Stage appStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        appStage.close();
    }

    @FXML
    private void handleButtonSaveConfig(ActionEvent event){
        Config config = new Config();
        config.setUrl(server_url_tf.getText());
        config.setApi(api_port_tf.getText());
        config.setDefaultUsername(default_username_tf.getText());
        config.setPush(push_port_tf.getText());
        ui.getCache().setConfig(config);
        loadConfig();
        Stage appStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        appStage.close();
   }

    @FXML
    void handleServerConfigBtn() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);

        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("configuration.fxml"));
        try {
            Parent root = loader.load();
            dialog.setTitle("Configuration");
            dialog.setScene(new Scene(root));
            dialog.show();
            help_lbl.setText("Configuration updated !");



        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void switchToMain(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("main.fxml"));
        MainPanel mainPanel = new MainPanel(ui);
        loader.setController(mainPanel);
        Parent blah = (Parent) loader.load();
        Scene scene = new Scene(blah);
        Stage appStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        appStage.setTitle("JMessenger");

        appStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                System.out.println("Stage is closing");
                ui.getClient().logout(true);
            }
        });
        appStage.setScene(scene);
        appStage.show();
    }


    public boolean authenticate(String login, String password)  {
        help_lbl.setText("");
        boolean success = false;
        try {
            String message = ui.getClient().authenticate(login,password);
            if(message == "OK"){
                success = true;
            }
            else if(message.contains("locked")){
                help_lbl.setText("Your account is locked. Please retry later.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return success;
    }





    @FXML
    public void handleButtonLoginAction(ActionEvent event) throws IOException {


        utils.dbg(ui.toString());
        String username = username_tf.getText();
        String password = password_tf.getText();
        if (username.isEmpty() || password.isEmpty()) {
            help_lbl.setText("Please enter your credentials !");
        }
        else {

            utils.dbg(((Boolean)ui.getConn().isConnected()).toString());
            if (ui.getConn().isConnected()) {
                if (authenticate(username, password)) {
                    try {
                        switchToMain(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    if (!help_lbl.getText().contains("locked")) {
                        help_lbl.setText("Invalid credentials, please check !");
                    }
                }
            }
            else{
                help_lbl.setText("You are offline, please turn on Internet or check the server confguration");
            }
        }
    }



    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    try{
            Config config = ui.getCache().getConfig();
            utils.dbg(config.getUrl());
            server_url_tf.setText(config.getUrl());
            default_username_tf.setText(config.getDefaultUsername());
            api_port_tf.setText(config.getApi());
            push_port_tf.setText(config.getPush());

        }
    catch (Exception e){


    }
    try{
        username_tf.setText(ui.getCache().getConfig().getDefaultUsername());
    }
    catch (Exception e){

    }
    }
        @FXML
    public void handleLblForgotAction(){
        help_lbl.setText("Not implemented yet");
    }
//
}
