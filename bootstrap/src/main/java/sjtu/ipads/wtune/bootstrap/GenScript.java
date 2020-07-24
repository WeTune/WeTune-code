package sjtu.ipads.wtune.bootstrap;

import sjtu.ipads.wtune.stmt.scriptgen.ScriptUtils;

public class GenScript implements Task {

  @Override
  public void doTasks(String... appNames) {
    ScriptUtils.copyResources();
    for (String appName : appNames) {
      System.out.println("[Script] generate script for " + appName);
      ScriptUtils.genSchema(appName, "base");
      ScriptUtils.genSchema(appName, "opt");
      ScriptUtils.genWorkload(appName, "base");
      ScriptUtils.genWorkload(appName, "index");
      ScriptUtils.genWorkload(appName, "opt");
    }
  }
}
