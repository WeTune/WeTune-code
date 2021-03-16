package sjtu.ipads.wtune.superopt.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.superopt.optimizer.Optimizer;
import sjtu.ipads.wtune.superopt.optimizer.SubstitutionBank;
import sjtu.ipads.wtune.superopt.profiler.Profiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class OptimizerRunner {
  private final SubstitutionBank bank;

  private OptimizerRunner(SubstitutionBank bank) {
    this.bank = bank;
  }

  public static OptimizerRunner make(String bankFile, boolean skipCheck) throws IOException {
    return new OptimizerRunner(
        SubstitutionBank.make().importFrom(Files.readAllLines(Paths.get(bankFile))));
  }

  public List<ASTNode> optimize(Statement stmt) {
    final App app = stmt.app();
    final ASTNode ast = stmt.parsed();
    final Schema schema = app.schema("base", true);
    final List<ASTNode> candidates = Optimizer.make(bank, schema).optimize(ast);
    return Profiler.make(app.dbProps()).pickOptimized(ast, candidates);
  }
}
