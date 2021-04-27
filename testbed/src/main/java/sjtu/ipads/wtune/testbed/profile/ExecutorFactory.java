package sjtu.ipads.wtune.testbed.profile;

public interface ExecutorFactory {
  Executor make(String sql);

  default void close() {}
}
