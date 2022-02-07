package sjtu.ipads.wtune.superopt;

import sjtu.ipads.wtune.common.utils.Lazy;
import sjtu.ipads.wtune.common.utils.SetSupport;
import sjtu.ipads.wtune.sql.SqlSupport;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.sql.plan.PlanSupport;
import sjtu.ipads.wtune.sql.schema.Schema;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.superopt.optimizer.OptimizationStep;
import sjtu.ipads.wtune.superopt.optimizer.Optimizer;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.sql.ast.SqlNode.MySQL;
import static sjtu.ipads.wtune.sql.plan.PlanSupport.translateAsAst;
import static sjtu.ipads.wtune.sql.support.action.NormalizationSupport.normalizeAst;
import static sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport.dumpTrace;

public abstract class TestHelper {
  private static final String TEST_SCHEMA =
      ""
          + "CREATE TABLE a ( i INT PRIMARY KEY, j INT, k INT );"
          + "CREATE TABLE b ( x INT PRIMARY KEY, y INT, z INT );"
          + "CREATE TABLE c ( u INT PRIMARY KEY, v CHAR(10), w DECIMAL(1, 10) );"
          + "CREATE TABLE d ( p INT, q CHAR(10), r DECIMAL(1, 10), UNIQUE KEY (p), FOREIGN KEY (p) REFERENCES c (u) );";
  private static final Lazy<Schema> SCHEMA =
      Lazy.mk(() -> SqlSupport.parseSchema(MySQL, TEST_SCHEMA));

  private static SubstitutionBank bank;

  public static SqlNode parseSql(String sql) {
    return SqlSupport.parseSql(MySQL, sql);
  }

  public static PlanContext parsePlan(String sql) {
    return PlanSupport.assemblePlan(parseSql(sql), SCHEMA.get());
  }

  public static SubstitutionBank bankForTest() {
    if (bank != null) return bank;

    try {
      bank = SubstitutionSupport.loadBank(Paths.get("wtune_data", "prepared", "rules.txt.1"));
    } catch (IOException ioe) {
      throw new UncheckedIOException(ioe);
    }

    return bank;
  }

  public static Set<SqlNode> optimizeStmt(Statement stmt) {
    final SqlNode ast = stmt.ast();
    final Schema schema = stmt.app().schema("base", true);
    ast.context().setSchema(schema);
    normalizeAst(ast);
    final PlanContext plan = PlanSupport.assemblePlan(ast, schema);
    final Optimizer optimizer = Optimizer.mk(bankForTest());
    optimizer.setTimeout(5000);
    optimizer.setTracing(true);
    final Set<PlanContext> optimized = optimizer.optimize(plan);
    for (PlanContext opt : optimized) dumpTrace(optimizer, opt);
    return SetSupport.map(optimized, it -> translateAsAst(it, it.root(), false));
  }

  public static Set<SqlNode> optimizeQuery(String appName, SqlNode ast) {
    final App app = App.of(appName);
    normalizeAst(ast);
    final PlanContext plan = PlanSupport.assemblePlan(ast, app.schema("base", true));
    final Optimizer optimizer = Optimizer.mk(bankForTest());
    final Set<PlanContext> optimized = optimizer.optimize(plan);
    return SetSupport.map(optimized, it -> translateAsAst(it, it.root(), false));
  }
}
