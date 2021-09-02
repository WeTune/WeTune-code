package sjtu.ipads.wtune.testbed.profile;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Map;
import sjtu.ipads.wtune.stmt.resolver.ParamDesc;
import sjtu.ipads.wtune.testbed.common.Actuator;

public interface Executor extends Actuator {
  boolean installParams(Map<ParamDesc, Object> params);

  long execute();

  ResultSet getResultSet();

  void endOne();

  void close();

  static Executor make(Connection connection, String sql) {
    return new ExecutorImpl(connection, sql);
  }
}
