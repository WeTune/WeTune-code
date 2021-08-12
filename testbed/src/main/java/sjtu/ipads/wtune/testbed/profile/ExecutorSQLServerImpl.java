package sjtu.ipads.wtune.testbed.profile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ExecutorSQLServerImpl extends ExecutorImpl{

    public ExecutorSQLServerImpl(Connection conn, String sql) {
        //TODO regex rewrite sql
//        sql = sql.replace();
        super(conn, sql);
    }


}
