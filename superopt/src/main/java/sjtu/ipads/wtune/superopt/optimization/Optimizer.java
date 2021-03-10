package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.superopt.optimization.internal.OptimizerImpl;

import java.util.List;

public interface Optimizer {
  List<PlanNode> optimize(PlanNode root);

  List<ASTNode> optimize(ASTNode root);

  static Optimizer make(SubstitutionBank repo, Schema schema) {
    return new OptimizerImpl(repo, schema);
  }
}
