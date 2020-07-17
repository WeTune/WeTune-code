package sjtu.ipads.wtune.bootstrap;

import sjtu.ipads.wtune.stmt.scriptgen.ScriptUtils;

public class GenScript implements Task {

  @Override
  public void doTasks(String... appNames) {
    //    ScriptUtils.copyResources();
    for (String appName : appNames) {
      //      ScriptUtils.genSchema(appName);
      ScriptUtils.genWorkload(appName, "base", true);
    }
  }
}
