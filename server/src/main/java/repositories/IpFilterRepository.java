package repositories;

import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class IpFilterRepository extends GenericRepository {

    public IpFilterRepository(Connection connection) {
        super(connection);
    }

    public ResultSet getAuthByIp(String ip) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("SELECT * FROM ip_filter WHERE user_ip = ? ");
        stmt.setString(1, ip);
        return stmt.executeQuery();
    }

    public void removeUserExpiredBlocks(String ip) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("DELETE FROM ip_filter WHERE user_ip = ? AND validity < ? ");
        stmt.setString(1, ip);
        stmt.setTimestamp(2, Timestamp.from(Instant.now()));
        stmt.execute();
    }

    public void saveFailedAttempt(String ipAddress, int block_time) throws SQLException {
        PreparedStatement stmt1 = connection.prepareStatement("INSERT INTO ip_filter VALUES (?,?,?) ON CONFLICT (user_ip) DO NOTHING");
        stmt1.setString(1, ipAddress);
        stmt1.setTimestamp(2, java.sql.Timestamp.from(Instant.now().plus(block_time, ChronoUnit.MINUTES)));
        stmt1.setInt(3, 0);

        PreparedStatement stmt = connection.prepareStatement("UPDATE ip_filter SET attempts = attempts + 1 WHERE user_ip = ?");
        stmt.setString(1, ipAddress);

        stmt1.execute();
        stmt.execute();
    }

}
