package sjtu.ipads.wtune.superopt;

import sjtu.ipads.wtune.common.utils.Lazy;
import sjtu.ipads.wtune.sql.SqlSupport;
import sjtu.ipads.wtune.sql.ast.ASTNode;
import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.sql.plan.PlanSupport;
import sjtu.ipads.wtune.sql.schema.Schema;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

import static sjtu.ipads.wtune.sql.ast1.SqlNode.MySQL;

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
    // TODO
    return Collections.emptySet();
  }
}
