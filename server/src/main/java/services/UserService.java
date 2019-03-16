package services;

import entities.User;
import entities.api.UserApiBody;
import entities.enums.EnumCustomErrorCode;
import exceptions.ProcessingException;
import repositories.SocketRepository;
import repositories.UserRepository;
import system.DatabaseConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class UserService extends GenericService {

    private final UserRepository userRepository;
    private final SocketRepository socketRepository;

    public UserService(DatabaseConnection databaseConnection) {
        super(databaseConnection);
        this.userRepository = new UserRepository(databaseConnection.get());
        this.socketRepository = new SocketRepository(databaseConnection.get());
    }

    public UserApiBody getSelfUser(User sourceUser) throws SQLException {
        ResultSet rsUser = userRepository.getUserByUid(sourceUser.getId());
        rsUser.next();
        return new UserApiBody(UUID.fromString(rsUser.getString("id")), rsUser.getString("login"), rsUser.getString("display_name"));
    }

    public UserApiBody getUser(UUID userUid) throws SQLException, ProcessingException {
        return getUserApiBody(userUid);
    }

    public List<UserApiBody> getUsers() throws SQLException {
        List<UserApiBody> output = new ArrayList<>();
        ResultSet rsUsers = userRepository.getAllUsers();
        while(rsUsers.next()) {
            output.add(new UserApiBody(
                    UUID.fromString(rsUsers.getString("id")),
                    rsUsers.getString("login"),
                    rsUsers.getString("display_name")
            ));
        }
        return output;
    }

    private List<UserApiBody> getUsersByName(String name) throws SQLException {
        List<UserApiBody> output = new ArrayList<>();
        ResultSet rsUsers = userRepository.searchUsersByName(name);
        while(rsUsers.next()) {
            output.add(new UserApiBody(
                    UUID.fromString(rsUsers.getString("id")),
                    rsUsers.getString("login"),
                    rsUsers.getString("display_name"),
                    false
            ));
        }
        return output;
    }

    public List<UserApiBody> searchUsers(String query) throws SQLException, ProcessingException {

        String name = null;
        boolean online = false;

        // Decompose url query
        if(query != null) {
            String[] params = query.split("&");
            // Check if parameters valid
            for (String param : params) {
                if (param.matches("name=.{3,30}")) {
                    name = param.split("=")[1];
                } else if (param.matches("online=true")) {
                    online = true;
                } else if (param.matches("online=false")) {
                    online = false;
                } else {
                    throw new ProcessingException(EnumCustomErrorCode.BAD_QUERY, "At least one of query parameters has not been recognised or is invalid");
                }
            }
        }

        // Get online users
        Map<UUID, UserApiBody> onlineUsers = new HashMap<>();
        ResultSet rsOnlineUsers = socketRepository.getAllOnlineUsers();
        while (rsOnlineUsers.next()) {
            UserApiBody newUser = new UserApiBody(
                    UUID.fromString(rsOnlineUsers.getString("user_id")),
                    rsOnlineUsers.getString("login"),
                    rsOnlineUsers.getString("display_name"),
                    true
            );
            onlineUsers.put(UUID.fromString(rsOnlineUsers.getString("user_id")), newUser);
        }

        // Select the right output
        if(name == null && !online) { // name but no online
            List<UserApiBody> allUsers = getUsers();
            return addUserOnlineStatus(allUsers, onlineUsers);
        } else if (name != null && !online) { // no name and no online
            List<UserApiBody> filteredUsers = getUsersByName(name);
            return addUserOnlineStatus(filteredUsers, onlineUsers);
        } else if (name == null) { // no name and no online
            return new ArrayList<>(onlineUsers.values());
        } else { // name and online
            List<UserApiBody> filteredUsers = getUsersByName(name);
            return innerJoin(filteredUsers, onlineUsers);
        }

    }

    public UserApiBody updateDisplayName(UserApiBody body, User sourceUser) throws ProcessingException, SQLException {
        // Check if displayName is present
        String newDisplayName = body.getDisplayName();
        if(newDisplayName == null) {
            throw new ProcessingException(EnumCustomErrorCode.ELEMENT_NOT_FOUND, "You must specify a new display name");
        }

        // Check if new name has valid length
        if(newDisplayName.length() <= 3 || newDisplayName.length() > 30) {
            throw new ProcessingException(EnumCustomErrorCode.WRONG_LENGTH, "This display name is too short or too long");
        }

        // Check if new name is available
        if( userRepository.getUserByDisplayName(newDisplayName).next() ){
            throw new ProcessingException(EnumCustomErrorCode.NAME_UNAVAILABLE, "This name is already being used by someone else");
        }

        // Change display name
        userRepository.updateDisplayName(sourceUser.getId(), newDisplayName);

        return getUserApiBody(sourceUser.getId());
    }

    private List<UserApiBody> addUserOnlineStatus(List<UserApiBody> users, Map<UUID, UserApiBody> onlineUsers) {
        for (UserApiBody user: users) {
            if( onlineUsers.containsKey(user.getId()) ) {
                user.setOnline(true);
            } else {
                user.setOnline(false);
            }
        }
        return users;
    }

    private List<UserApiBody> innerJoin(List<UserApiBody> users, Map<UUID, UserApiBody> onlineUsers) {
        List<UserApiBody> output = new ArrayList<>();
        for (UserApiBody user: users) {
            if( onlineUsers.containsKey(user.getId()) ) {
                user.setOnline(true);
                output.add(user);
            }
        }
        return output;
    }

    private List<UserApiBody> outerJoin(List<UserApiBody> users, Map<UUID, UserApiBody> onlineUsers) {
        List<UserApiBody> output = new ArrayList<>();
        for (UserApiBody user: users) {
            if( !onlineUsers.containsKey(user.getId()) ) {
                user.setOnline(true);
                output.add(user);
            }
        }
        return output;
    }
}
