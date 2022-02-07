package wtune.testbed.profile;

import wtune.testbed.util.SQLServerStmtRewriteHelper;

import java.sql.Connection;

public class ExecutorSQLServerImpl extends ExecutorImpl{

    ExecutorSQLServerImpl(Connection conn, String sql) {
        super(conn, SQLServerStmtRewriteHelper.regexRewriteForSQLServer(sql));
    }
}
