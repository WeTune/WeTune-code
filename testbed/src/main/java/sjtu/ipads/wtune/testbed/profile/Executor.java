package sjtu.ipads.wtune.testbed.profile;

import sjtu.ipads.wtune.sql.support.resolution.ParamDesc;
import sjtu.ipads.wtune.testbed.common.Actuator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Map;

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
