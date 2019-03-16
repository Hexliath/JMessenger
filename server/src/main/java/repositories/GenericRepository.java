package repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Abstract database repository
 * This class provides sql connection element to all children.
 */
public abstract class GenericRepository {

    protected final Connection connection;

    public GenericRepository(Connection connection) {
        this.connection = connection;
    }

    // Count rows in a result set
    protected int getRowCount(ResultSet rs) throws SQLException {
        int count = 0;
        while(rs.next()) {
            count++;
        }
        return count;
    }


}
