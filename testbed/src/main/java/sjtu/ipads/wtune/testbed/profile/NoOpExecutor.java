package sjtu.ipads.wtune.testbed.profile;

import java.sql.ResultSet;
import java.util.Map;
import sjtu.ipads.wtune.stmt.resolver.ParamDesc;

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
  public boolean execute() {
    return true;
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
