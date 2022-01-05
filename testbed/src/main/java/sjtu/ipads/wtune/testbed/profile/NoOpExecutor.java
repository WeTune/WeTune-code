package sjtu.ipads.wtune.testbed.profile;

import sjtu.ipads.wtune.sql.support.resolution.ParamDesc;

import java.sql.ResultSet;
import java.util.Map;

public class NoOpExecutor implements Executor {
  @Override
  public int getAndForwardIndex() {
    return 0;
  }

  @Override
  public boolean installParams(Map<ParamDesc, Object> params) {
    for (var pair : params.entrySet())
      System.out.println(pair.getKey() + ": '" + pair.getValue() + "'");
    return true;
  }

  @Override
  public long execute() {
    return 0L;
  }

  @Override
  public ResultSet getResultSet() {
    return null;
  }

  @Override
  public void endOne() {}

  @Override
  public void close() {}
}
