package sjtu.ipads.wtune.superopt;

import sjtu.ipads.wtune.sqlparser.ASTParser;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.assemblePlan;
import static sjtu.ipads.wtune.stmt.support.Workflow.normalize;

class TestHelper {
  private static SubstitutionBank bank;

  static JoinNode mkJoin(String join) {
    return ((JoinNode) mkPlan("Select a.i From " + join).predecessors()[0]);
  }

  static PlanNode mkPlan(String sql) {
    final Statement stmt = Statement.mk("test", sql, null);
    final ASTNode ast = stmt.parsed();
    return assemblePlan(ast, stmt.app().schema("base"));
  }

  static PlanNode mkPlan(String sql, String schemaSQL) {
    final Schema schema = Schema.parse(MYSQL, schemaSQL);
    final ASTNode ast = ASTParser.mysql().parse(sql);
    return assemblePlan(ast, schema);
  }

  static SubstitutionBank getBank() {
    if (bank != null) return bank;

    try {
      //      bank = SubstitutionSupport.loadBank(Paths.get("wtune_data", "substitutions"));
      bank = SubstitutionSupport.loadBank(Paths.get("wtune_data", "test_substitutions"));
      //      bank = SubstitutionSupport.loadBank(Paths.get("wtune_data", "test.txt"));
    } catch (IOException ioe) {
      throw new UncheckedIOException(ioe);
    }

    return bank;
  }

  static Set<ASTNode> optimizeStmt(Statement stmt) {
    final ASTNode ast = stmt.parsed();
    final Schema schema = stmt.app().schema("base", true);
    ast.context().setSchema(schema);
    normalize(stmt.parsed());

    return OptimizerSupport.optimize(getBank(), schema, ast);
  }
}
