package view;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controller.NotifySocket;
import controller.SingleMessageHandler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lib.utils;
import model.*;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

public class MainPanel implements Initializable {


    //    public void handleChannelChangeAction(ActionEvent event){
//        ui.getChannelsHandler().set("");
//    }
    @FXML    private Button logout_btn;
    @FXML    private Label actual_channel_lbl;
    @FXML    private TextArea message_text;
    @FXML    private ListView<String> messages_lstv;
    @FXML    private ListView<String> channels_lstv;
    @FXML    private ListView<String> users_lstv;

    @FXML    private ListView<String> bookmarks_lstv;
    @FXML    private UI ui;
    @FXML    private Button hide_leftpanel_btn;
    @FXML    private Button create_channel_btn;
    @FXML    private Button personal_config_btn;
    @FXML    private Accordion leftpanel_acc;
    @FXML    private Button join_btn;
    @FXML    private Label client_username_lbl;
    @FXML    private Label register_help_lbl;
    @FXML    private TextField channel_name_tf;
    @FXML    private CheckBox public_chb;
    @FXML    private Button config_cancel_btn;
    @FXML    private Button channel_add_btn;
    @FXML    private Label total_messages_lbl;
    @FXML    private Label total_channels_lbl;
    @FXML    private Label online_users_lbl;
    @FXML    private Button modify_channel_btn;
    @FXML    private ComboBox<String> channel_owner_cb;
    @FXML    private Button modify_cancel_btn;
    @FXML    private Button channel_modify_btn;






    private List<Channel> channels;
    private NotifySocket notify;


    @FXML
    public void handleButtonBookmark(ActionEvent event){
        if(ui.getChannelsHandler().addBookmarks()){
            bookmarks_lstv.getItems().add(ui.getChannelsHandler().getChannel().getName());
            messages_lstv.getItems().add("Channel successfully added to bookmarks !");
        }
        else{
            ui.getChannelsHandler().deleteBookmarks();
            messages_lstv.getItems().add(ui.getChannelsHandler().getChannel().getName() + "  removed from bookmarks !");
        }
        updateBookmarks();
    }

    @FXML
    public void handleButtonUpdateChannel(ActionEvent event){
        User new_owner = new User();
        String username = channel_owner_cb.getSelectionModel().getSelectedItem();
        for(User u:ui.getUsers_handler().getAll(false)){
            if(u.getDisplayName().equals(username)){
                ui.getChannelsHandler().editChannel(channel_name_tf.getText(),u);
                break;
            }
        }
        setChannel();
        updateChannels();
        Stage appStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        appStage.close();
    }


    @FXML
    public void handleButtonModifyChannel(ActionEvent event){
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("modify_channel.fxml"));
        try {
            loader.setController(this);
            Parent root = loader.load();
            dialog.setTitle("Modify channel");
            dialog.setScene(new Scene(root));
            dialog.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    public void handleButtonAddChannel(ActionEvent event) {
        Channel.Type type = Channel.Type.GROUP;
        Map<User, Rights> users = new HashMap<>();

        if(public_chb.isSelected()){

            type = Channel.Type.PUBLIC;
        }

        ui.getChannelsHandler().add(channel_name_tf.getText(),users, type);
        ui.getChannelsHandler().updateChannels();
        updateChannels();
        Stage appStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        appStage.close();
    }

    @FXML
    public void handleButtonCancel(ActionEvent event){
        Stage appStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        appStage.close();

    }





    public MainPanel(UI ui) throws IOException {
        this.ui = ui;
        ui.setMainInterface();
        utils.dbg(ui.toString());
    }

    @FXML
    public void handleSelectedChannel() {
        String channel = channels_lstv.getSelectionModel().getSelectedItems().get(0);
        if(channel == null){
            channel =  bookmarks_lstv.getSelectionModel().getSelectedItems().get(0);
        }

        if (!(channel.equals(ui.getChannelsHandler().getChannel().getName()))) {
            utils.dbg(channel + "/" + ui.getChannelsHandler().getChannel().getName());


            messages_lstv.getItems().clear();
            for (Channel c : channels) {
                utils.dbg(String.valueOf(c.getName().equals(channel)));
                if (channel.equals(c.getName())) {
                    utils.dbg("target : " + c.getName() + c.getId());
                    ui.getChannelsHandler().set(c.getId());
                    updateMessages();

                    setChannel();
                    if (!ui.getChannelsHandler().isMember()) {
                        join_btn.setText("Join");
                    }
                    else
                    {
                        join_btn.setText("Leave");
                    }
                    break;
                }
            }
        }
    }


    @FXML
    public void handleButtonJoin() {
        if(join_btn.getText().equals("Join")) {
            String result = ui.getChannelsHandler().joinChannel();
            if (result == "OK" || result == "ALREADY_JOINED") {
                join_btn.setText("Leave");
                messages_lstv.getItems().add("Welcome in channel " + ui.getChannelsHandler().getChannel().getName() + " !");
                SingleMessageHandler message = new SingleMessageHandler(ui.getConn(),
                        ui.getCache(), " has join the channel !", ui.getChannelsHandler().getChannel(), ui.getClient().getClient());
                message.send();
            }
        }
        else{
            String result = ui.getChannelsHandler().leaveChannel();
            if (result == "OK") {
                join_btn.setText("Join");
                messages_lstv.getItems().add("See you soon in " + ui.getChannelsHandler().getChannel().getName() + " !");
            }
            else if(result == "PRIVILEGE_CONFLICT"){
                messages_lstv.getItems().add("You are the owner of this channel. Edit channel to set a new owner before leaving");
            }

        }
    }


    /**
     * Reloads the messages
     */
    private void updateMessages() {
        List<String> messages = null;
        try {
            messages = ui.getChannelsHandler().getMessages();
            for (String msg : messages) {
                messages_lstv.getItems().add(msg);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add a new message to the messages list
     * @param msg the string to display
     */
    public void appendMessage(String msg) {
        messages_lstv.getItems().add(msg);
    }


    /**
     * Reloads the stats
     */
    public void updateStats(){
        Stats stats = ui.getChannelsHandler().getStats();
        online_users_lbl.setText("0");
        total_channels_lbl.setText(stats.getChannelNumber());
        total_messages_lbl.setText(stats.getMessagesNumber());
    }


    /**
     * Reload the channels
     */
    public void updateChannels(){
        channels_lstv.getItems().clear();
        channels = ui.getChannelsHandler().getAll(Channel.Type.PUBLIC);
        int i = 0;
        for (Channel c : channels) {
            channels_lstv.getItems().add(i, c.getName());
            i++;
        }
    }


    @FXML
    public void handleButtonSendAction(ActionEvent event) throws IOException {
        utils.dbg(ui.toString());
        String text = message_text.getText();
        SingleMessageHandler message = new SingleMessageHandler(ui.getConn(),
                ui.getCache(), text, ui.getChannelsHandler().getChannel(), ui.getClient().getClient());

        if (!text.equals("")) {
            message_text.clear();
            String result = message.send();
            if (result == "OK") {
                messages_lstv.getItems().add(ui.getClient().getClient().getDisplayName() + ": " + text);
                updateStats();
            } else if (result == "NOT_ENOUGH_PRIVILEGES") {
                messages_lstv.getItems().add("You must join the channel to participate. Click on the join button. ");
            } else {
                messages_lstv.getItems().add("An error happened while proccessing the message. Please retry.");
            }
        }
    }

    @FXML
    public void handleButtonLogout(ActionEvent event) throws IOException {
        Stage stage = (Stage) logout_btn.getScene().getWindow();
        ui.getClient().logout(true);

        stage.close();
        FXMLLoader loader = new FXMLLoader();
        Parent blah = (Parent) loader.load(getClass().getClassLoader().getResource("login.fxml"));
        Scene scene = new Scene(blah);
        Stage appStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        appStage.setScene(scene);
        appStage.show();
    }

    @FXML
    public void handleButtonHideLPAction(ActionEvent event) {
        if (leftpanel_acc.isVisible()) {
            leftpanel_acc.setVisible(false);
        } else {
            leftpanel_acc.setVisible(true);
        }
    }

    @FXML
    public void handleCreateChannelBtn(Event event) throws IOException {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("new_channel.fxml"));
        try {
            loader.setController(this);
            Parent root = loader.load();
            dialog.setTitle("Add channel");
            dialog.setScene(new Scene(root));
            dialog.show();
            updateStats();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateBookmarks(){
        bookmarks_lstv.getItems().clear();
        int i=0;
        List<Bookmark> bookmarks = ui.getChannelsHandler().getBookmarks();
        i = 0;
        for (Bookmark b: bookmarks) {
            bookmarks_lstv.getItems().add(i, b.getName());
            i++;
        }
    }

    public void setChannel(){
        actual_channel_lbl.setText(ui.getChannelsHandler().getChannel().getName());
        client_username_lbl.setText(ui.getClient().getClient().getDisplayName());
        if(ui.getChannelsHandler().getChannel().isJoined()) {
            join_btn.setText("Leave");
        }
        try{
        if(ui.getChannelsHandler().getRole().equals("OWNER")){
            modify_channel_btn.setDisable(false);
        }
        else{
            modify_channel_btn.setDisable(true);

        }
        }
        catch (Exception e){
            e.printStackTrace();
        }


    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if(!service.isRunning()) {
            updateMessages();
            updateBookmarks();
            updateChannels();
            updateStats();
            setChannel();

            int i=0;
            List<User> users = ui.getUsers_handler().getAll(false);
            i = 0;
            for (User c : users) {
                users_lstv.getItems().add(i, c.getDisplayName());
                i++;
            }
            try {
                notify =  new NotifySocket(ui.getConn().getPushURL(), 6101);
            } catch (IOException e) {
                e.printStackTrace();
            }

            service.start();
            service.setOnSucceeded(event -> updateStats());
            service.setOnFailed(event -> updateStats());
        }
        try{
            channel_name_tf.setText(ui.getChannelsHandler().getChannel().getName());
            List<User> users = ui.getUsers_handler().getAll(false);
            List<String> usernames = new ArrayList<String>();
            for(User user:users){
                if(!user.getId().equals(ui.getClient().getClient().getId()))
                usernames.add(user.getDisplayName());

            }

            ObservableList<String> list = FXCollections.observableArrayList(usernames);
            channel_owner_cb.setItems(list);
        }
        catch (Exception e){

        }
        try{
            setChannel();
        }
        catch (Exception e){

        }

       utils.dbg("push" + ui.getConn().getPushURL());
    }


    /**
     * Background service for push notifications.
     */
    Service service = new Service() {

        @Override
        protected Task createTask() {
            try {
                return new Task() {
                    @Override
                    protected Void call() throws Exception {
                        String token = null;
                        token = notify.sendMessage("{\"token\": \"" + ui.getConn().seeToken() + "\"}");
                        utils.dbg(token);
                        String lastInput = "";
                        while (true) {
                            try {
                                while ((lastInput = notify.getIn().readLine()) != null) {
                                    if (lastInput.contains("NEW_MESSAGE")) // When a new message is received
                                    {
                                        Message message = new Message();
                                        ObjectMapper mapper = new ObjectMapper();
                                        JsonNode actualObj = mapper.readTree(lastInput);
                                        Object msg = utils.parseJson(actualObj.get("body").toString(), message);
                                        message = (Message) msg;
                                        actualObj = mapper.readTree(actualObj.get("body").toString());
                                        User user = new User();
                                        msg = utils.parseJson(actualObj.get("author").toString(), user);
                                        user = (User) msg;
                                        message.setAuthor(user.getDisplayName());
                                        if (ui.getChannelsHandler().getChannel().getId().equals(message.getChannelId())) {
                                            if(message.getContent().equals("has join the channel !")){
                                                appendMessage(user.getDisplayName() + " " + message.getContent() + " (info)");
                                            }
                                            else{
                                                appendMessage(user.getDisplayName() + ": " + message.getContent());
                                            }
                                        }
                                        utils.dbg("new message !!!!");
                                        ui.getChannelsHandler().getMessagesHandler().appendToLocal(message);

                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
            } catch (Exception e) {

            }
            return null;
        }

    };
    }


