package sjtu.ipads.wtune.testbed.profile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ExecutorSQLServerImpl extends ExecutorImpl{

    public ExecutorSQLServerImpl(Connection conn, String sql) {
        //TODO regex rewrite sql
        super(conn, sql);
    }

//    @Override
//    protected PreparedStatement statement() throws SQLException {
//        if (stmt != null) return stmt;
//        return stmt = conn.prepareStatement(sql);
//    }
}
