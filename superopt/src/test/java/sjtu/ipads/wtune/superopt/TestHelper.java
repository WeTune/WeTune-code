package sjtu.ipads.wtune.superopt;

import sjtu.ipads.wtune.common.utils.Lazy;
import sjtu.ipads.wtune.sqlparser.ASTParser;
import sjtu.ipads.wtune.sqlparser.SqlSupport;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNode;
import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan1.PlanSupport;
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
import static sjtu.ipads.wtune.sqlparser.ast1.SqlNode.MySQL;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.assemblePlan;
import static sjtu.ipads.wtune.stmt.support.Workflow.normalize;

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

  static JoinNode mkJoin(String join) {
    return ((JoinNode) mkPlan("Select a.i From " + join).predecessors()[0]);
  }

  static PlanNode mkPlan(String sql) {
    final Statement stmt = Statement.mk("test", sql, null);
    final ASTNode ast = stmt.parsed();
    return assemblePlan(ast, stmt.app().schema("base"));
  }

  static PlanNode mkPlan(String sql, String schemaSQL) {
    final Schema schema = SqlSupport.parseSchema(MYSQL, schemaSQL);
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
