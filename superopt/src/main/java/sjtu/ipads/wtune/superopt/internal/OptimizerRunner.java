package sjtu.ipads.wtune.superopt.internal;

import static sjtu.ipads.wtune.stmt.support.Workflow.normalize;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.superopt.optimizer.Optimizer;
import sjtu.ipads.wtune.superopt.optimizer.SubstitutionBank;
import sjtu.ipads.wtune.superopt.profiler.Profiler;

public class OptimizerRunner {
  private final SubstitutionBank bank;

  public OptimizerRunner(SubstitutionBank bank) {
    this.bank = bank;
  }

  public List<ASTNode> optimize(Statement stmt) {
    final App app = stmt.app();
    final ASTNode ast = stmt.parsed();
    final Schema schema = app.schema("base", true);
    ast.context().setSchema(schema);
    normalize(ast);
    final List<ASTNode> candidates = Optimizer.make(bank, schema).optimize(ast);
    return Profiler.make(app.dbProps()).pickOptimized(ast, candidates);
  }
}
