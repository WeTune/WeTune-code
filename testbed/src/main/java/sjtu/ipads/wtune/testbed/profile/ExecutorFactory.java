package sjtu.ipads.wtune.testbed.profile;

public interface ExecutorFactory {
  Executor mk(String sql);

  default void close() {}
}
