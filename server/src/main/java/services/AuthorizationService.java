package services;

import entities.User;
import entities.api.AuthorizationTokenApiBody;
import entities.api.LoginApiBody;
import entities.api.LoginResultApiBody;
import entities.api.UserApiBody;
import entities.enums.EnumChannelRole;
import entities.enums.EnumCustomErrorCode;
import exceptions.ProcessingException;
import org.mindrot.jbcrypt.BCrypt;
import repositories.AuthorisationRepository;
import repositories.ChannelUserRepository;
import repositories.IpFilterRepository;
import repositories.UserRepository;
import system.DatabaseConnection;
import system.Logger;
import system.TokenGenerator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

public class AuthorizationService extends GenericService {

    protected static final Logger log = new Logger(AuthorizationService.class);

    private final UserRepository userRepository;
    private final AuthorisationRepository authorisationRepository;
    private final IpFilterRepository ipFilterRepository;
    private final ChannelUserRepository channelUserRepository;

    public static int ATTEMPT_LIMIT = 3;
    public static int TOKEN_VALIDITY = 5; // hours
    public static int BLOCK_TIME = 15; // minutes
    public static int SIMULTANEOUS_CONNECTION_LIMIT = 5;
    public static UUID DEFAULT_CHANNEL_ID = UUID.fromString("f2f62cc7-da3c-49b0-b7aa-79f7001c6afc");

    public AuthorizationService(DatabaseConnection databaseConnection) {
        super(databaseConnection);
        this.userRepository = new UserRepository(databaseConnection.get());
        this.authorisationRepository = new AuthorisationRepository(databaseConnection.get());
        this.ipFilterRepository = new IpFilterRepository(databaseConnection.get());
        this.channelUserRepository = new ChannelUserRepository(databaseConnection.get());
    }

    public UserApiBody createAccount(LoginApiBody loginApiBody) throws SQLException, ProcessingException {

        ResultSet rsUser = userRepository.getUserByLogin(loginApiBody.getLogin());
        if (rsUser.next()) {
            throw new ProcessingException(EnumCustomErrorCode.ALREADY_EXIST, "This login is already taken by someone else");
        }

        UUID newUid = UUID.randomUUID();
        String displayName = loginApiBody.getLogin();
        if(loginApiBody.getDisplayName() != null && loginApiBody.getDisplayName().length() >= 3){
            displayName = loginApiBody.getDisplayName();
        }

        // Save new user
        userRepository.saveNewUser(
                newUid,
                loginApiBody.getLogin(),
                BCrypt.hashpw(loginApiBody.getPassword(), BCrypt.gensalt()),
                java.sql.Date.valueOf(LocalDate.now()),
                loginApiBody.getLogin(),
                ""
        );

        // Add new user to default channel
        channelUserRepository.saveNewRelation(newUid, DEFAULT_CHANNEL_ID, EnumChannelRole.MEMBER, Date.from(Instant.now()));

        return new UserApiBody(newUid, loginApiBody.getLogin(), displayName);
    }

    public LoginResultApiBody getAuthorizationToken(LoginApiBody loginApiBody, String ipAddress) throws SQLException, ProcessingException {

        // Check if user is blocked
        int userAttempts = 0;
        ipFilterRepository.removeUserExpiredBlocks(ipAddress);
        ResultSet rsAuth = ipFilterRepository.getAuthByIp(ipAddress);
        if (rsAuth.next()) {
            userAttempts = rsAuth.getInt("attempts");
            if(userAttempts >= ATTEMPT_LIMIT) {
                return new LoginResultApiBody(EnumCustomErrorCode.TOO_MANY_ATTEMPTS, false, rsAuth.getInt("attempts"), true, "", null,"You are temporary blocked for to many false login attempts. Try again later");
            }
        }

        // Get user by requested login
        ResultSet rsUser = userRepository.getUserByLogin(loginApiBody.getLogin());

        // Check if user exists
        if (!rsUser.next()) {
            return new LoginResultApiBody(EnumCustomErrorCode.USER_NOT_FOUND,false, -1, false, "", null, "This username doesn't exist");
        }

        // Check if password matches
        if (!BCrypt.checkpw(loginApiBody.getPassword(), rsUser.getString("password"))) {
            ipFilterRepository.saveFailedAttempt(ipAddress, BLOCK_TIME);
            return new LoginResultApiBody(EnumCustomErrorCode.WRONG_PASSWORD,false, userAttempts+1, false, "", null, "Wrong password");
        }

        // Generate a token that isn't being used by someone else
        String newToken = "";
        for (int i = 0; i < 10; i++) {
            newToken = new TokenGenerator(64).nextString();
            if(authorisationRepository.isTokenInFreeToUse(newToken)) {
                break;
            }
        }
        if(newToken.equals("")) {
            return new LoginResultApiBody(EnumCustomErrorCode.TRY_AGAIN_LATER,false, -1, false, "", null, "Unable to create a new unique token. Try again later");
        }

        // Get user uid
        UUID userId = UUID.fromString(rsUser.getString("id"));

        // Remove expired tokens
        authorisationRepository.removeUserExpiredTokens(userId, Date.from(Instant.now()));

        // Check if simultaneous connections limit has been reached
        if(authorisationRepository.getOpenConnectionsCount(userId) >= SIMULTANEOUS_CONNECTION_LIMIT ) {
            return new LoginResultApiBody(EnumCustomErrorCode.TOO_MANY_CONNECTIONS,true, -1, false, "", userId, "Simultaneous sessions limit exceeded");
        }

        // Set new auth line
        authorisationRepository.saveNewRecord(userId, newToken, Date.from(Instant.now().plus(TOKEN_VALIDITY, ChronoUnit.HOURS)), ipAddress);

        return new LoginResultApiBody(EnumCustomErrorCode.SUCCESS, true, 1, false, newToken, userId);
    }

    public void deleteAuthorizationToken(AuthorizationTokenApiBody authorizationTokenApiBody) throws SQLException {
        authorisationRepository.destroyToken(authorizationTokenApiBody.getToken());
    }

    public void deleteAllAuthorizationToken(AuthorizationTokenApiBody authorizationTokenApiBody) throws SQLException, ProcessingException {
        User sourceUser = getUserFromToken(authorizationTokenApiBody.getToken());
        authorisationRepository.destroyAllUserTokens(sourceUser.getId());
    }

    public boolean isTokenValid(String authorizationToken) throws SQLException {
        // Check if token exist and isn't expired
        ResultSet rsAuth = authorisationRepository.getAuthByToken(authorizationToken);
        if (rsAuth.next()) {
            Date tokenValidity = rsAuth.getTimestamp("validity");
            if(tokenValidity.after(Date.from(Instant.now()))){
                // Extend token validity
                authorisationRepository.renewTokenValidity(authorizationToken, Date.from(Instant.now().plus(TOKEN_VALIDITY, ChronoUnit.HOURS)));
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public User getUserFromToken(String authorizationToken) throws SQLException, ProcessingException {
        UUID userId;
        Date tokenValidity;

        // Get user uid by token
        ResultSet rsAuth = authorisationRepository.getAuthByToken(authorizationToken);
        if (rsAuth.next()) {
            userId = UUID.fromString(rsAuth.getString("user_id"));
            tokenValidity = rsAuth.getTimestamp("validity");
        } else {
            throw new ProcessingException(EnumCustomErrorCode.ELEMENT_NOT_FOUND, "No authorisation record for given token");
        }

        // Get user by uid
        ResultSet rsUser = userRepository.getUserByUid(userId);

        if (rsUser.next()) {
            User user = new User(
                    userId,
                    rsUser.getString("login"),
                    rsUser.getString("display_name"),
                    new AuthorizationTokenApiBody(authorizationToken, tokenValidity)
            );
            log.debug("Token has been decoded to user: "+user.toString());
            return user;
        } else {
            throw new ProcessingException(EnumCustomErrorCode.USER_NOT_FOUND, "No user found for given id");
        }
    }

}
