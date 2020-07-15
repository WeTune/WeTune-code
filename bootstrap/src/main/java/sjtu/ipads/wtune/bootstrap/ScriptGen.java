package sjtu.ipads.wtune.bootstrap;

import sjtu.ipads.wtune.stmt.context.AppContext;
import sjtu.ipads.wtune.stmt.scriptgen.ScriptUtils;

import java.util.List;

public class ScriptGen implements Task {

  @Override
  public void doTask(List<String> appNames) {
    ScriptUtils.copyResources();
    for (String appName : appNames) ScriptUtils.genSchema(AppContext.of(appName));
  }
}
