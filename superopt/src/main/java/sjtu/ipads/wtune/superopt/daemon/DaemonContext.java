package sjtu.ipads.wtune.superopt.daemon;

import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.Statement;

import java.lang.System.Logger;

public interface DaemonContext {
  Logger LOG = System.getLogger("WeTune");

  App appOf(String contextName);

  Registration registrationOf(String contextName);

  SqlNode optimize(Statement stmt);

  void run();

  void stop();
}
