package sjtu.ipads.wtune.testbed.profile;

import gnu.trove.list.TIntList;
import java.lang.System.Logger;
import sjtu.ipads.wtune.stmt.Statement;

public interface Profiler {
  Logger LOG = System.getLogger("Profiler");

  Statement statement();

  TIntList seeds();

  Metric metric();

  ParamsGen paramsGen();

  void setSeeds(TIntList seeds);

  boolean prepare();

  boolean run();

  void close();

  static Profiler make(Statement stmt, ProfileConfig config) {
    return new ProfilerImpl(stmt, config);
  }
}
