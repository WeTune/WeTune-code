package sjtu.ipads.wtune.bootstrap;

import sjtu.ipads.wtune.stmt.context.AppContext;
import sjtu.ipads.wtune.stmt.statement.Timing;

import static sjtu.ipads.wtune.stmt.statement.Statement.*;

public class UpdateDb implements Task {
  @Override
  public void doTask(String appName) {
    AppContext.of(appName).timing(TAG_BASE).forEach(Timing::save);
    AppContext.of(appName).timing(TAG_INDEX).forEach(Timing::save);
    AppContext.of(appName).timing(TAG_OPT).forEach(Timing::save);
  }
}
