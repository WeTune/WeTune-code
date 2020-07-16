package sjtu.ipads.wtune.bootstrap;

import sjtu.ipads.wtune.stmt.context.AppContext;
import sjtu.ipads.wtune.stmt.scriptgen.ScriptUtils;

public class ScriptGen implements Task {

  @Override
  public void doTasks(String... appNames) {
    ScriptUtils.copyResources();
    for (String appName : appNames) ScriptUtils.genSchema(AppContext.of(appName));
  }
}
