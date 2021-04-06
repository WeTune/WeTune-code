package sjtu.ipads.wtune.superopt.daemon;

import java.lang.System.Logger;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.Statement;

public interface DaemonContext {
  Logger LOG = System.getLogger("WeTune");

  App appOf(String contextName);

  Registration registrationOf(String contextName);

  ASTNode optimize(Statement stmt);

  void run();

  void stop();
}
