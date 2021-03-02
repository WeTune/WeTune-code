package sjtu.ipads.wtune.superopt.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ToASTTranslator;
import sjtu.ipads.wtune.sqlparser.plan.ToPlanTranslator;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.superopt.optimization.SubstitutionBank;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class OptimizationRunner {
  public static final System.Logger LOG = System.getLogger("optimizer");

  private final SubstitutionBank repo;

  public OptimizationRunner(SubstitutionBank repo) {
    this.repo = repo;
  }

  public static OptimizationRunner build(Path substitutionFiles) throws IOException {
    return build(SubstitutionBank.make().importFrom(Files.readAllLines(substitutionFiles)));
  }

  public static OptimizationRunner build(SubstitutionBank repo) {
    return new OptimizationRunner(repo);
  }

  public List<ASTNode> optimize(Statement stmt) {
    final ASTNode ast = stmt.parsed();
    final PlanNode plan = ToPlanTranslator.toPlan(ast);
    return listMap(
        ToASTTranslator::toAST, Optimizer.make(repo, stmt.app().schema("base")).optimize(plan));
  }
}
