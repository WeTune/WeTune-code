package sjtu.ipads.wtune.superopt.daemon;

import java.lang.System.Logger;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.superopt.internal.OptimizerRunner;

public interface DaemonContext {
  Logger LOG = System.getLogger("WeTune");

  App appOf(String contextName);

  OptimizerRunner optimizer();

  Optimizations optimizationsOf(String contextName);

  void run();

  void stop();
}
