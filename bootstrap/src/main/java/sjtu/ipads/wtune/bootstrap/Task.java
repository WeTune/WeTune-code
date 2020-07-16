package sjtu.ipads.wtune.bootstrap;

public interface Task {
  default void setArgs(String... args) {}

  default void doTasks(String... appNames) {
    for (String appName : appNames) doTask(appName);
  }

  default void doTask(String appName) {
    doTasks(appName);
  }
}
