package sjtu.ipads.wtune.superopt;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan1.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.stmt.Statement;

import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.assemblePlan;

class TestHelper {
  static JoinNode mkJoin(String join) {
    return ((JoinNode) mkPlan("Select a.i From " + join).predecessors()[0]);
  }

  static PlanNode mkPlan(String sql) {
    final Statement stmt = Statement.mk("test", sql, null);
    final ASTNode ast = stmt.parsed();
    return assemblePlan(ast, stmt.app().schema("base"));
  }
}
