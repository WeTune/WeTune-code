package sjtu.ipads.wtune.superopt.profiler;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

import java.util.List;
import java.util.Properties;

public interface Profiler {
  List<ASTNode> pickOptimized(ASTNode baseline, List<ASTNode> candidates);

  static Profiler make(Properties connProps) {
    return new CostBasedProfiler(connProps);
  }
}
