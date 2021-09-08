package sjtu.ipads.wtune.testbed.profile;

import static sjtu.ipads.wtune.testbed.util.SQLServerStmtRewriteHelper.*;

import java.sql.Connection;

public class ExecutorSQLServerImpl extends ExecutorImpl{

    ExecutorSQLServerImpl(Connection conn, String sql) {
        super(conn, regexRewriteForSQLServer(sql));
    }
}
