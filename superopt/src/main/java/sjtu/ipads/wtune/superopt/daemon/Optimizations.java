package sjtu.ipads.wtune.superopt.daemon;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.stmt.Statement;

public interface Optimizations {
  void register(Statement stmt, ASTNode optimized);

  boolean contains(Statement stmt);
}
