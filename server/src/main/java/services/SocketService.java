package services;

import repositories.SocketRepository;
import system.DatabaseConnection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class SocketService extends GenericService {

    private final SocketRepository socketRepository;

    public SocketService(DatabaseConnection databaseConnection) {
        super(databaseConnection);
        this.socketRepository = new SocketRepository(databaseConnection.get());
    }

    public void saveSocket(String userToken, UUID socketId) throws SQLException {
        socketRepository.saveNewSocket(userToken, socketId);
    }

    public boolean isTokenOccupied(String userToken) throws SQLException {
        ResultSet rsSocket = socketRepository.getSocketByToken(userToken);
        return rsSocket.next();
    }

    public void removeSocket(UUID socketId) throws SQLException {
        socketRepository.removeSocket(socketId);
    }

    public void clearSocketTable() throws SQLException {
        socketRepository.removeAllSockets();
    }

}
