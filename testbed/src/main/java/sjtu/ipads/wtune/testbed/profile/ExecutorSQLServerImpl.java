package sjtu.ipads.wtune.testbed.profile;

import java.sql.Connection;

import static sjtu.ipads.wtune.testbed.util.SQLServerStmtRewriteHelper.regexRewriteForSQLServer;

public class ExecutorSQLServerImpl extends ExecutorImpl{

    ExecutorSQLServerImpl(Connection conn, String sql) {
        super(conn, regexRewriteForSQLServer(sql));
    }
}
