package sjtu.ipads.wtune.stmt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.SqlSupport;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan.PlanSupport;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlNode.MySQL;

public class RealWorldStmtsTest {
  @Test
  @DisplayName("[Stmt] parsing all schema")
  void testParseSchema() {
    for (App app : App.all()) app.schema("base");
  }

  @Test
  @DisplayName("[Stmt] parsing sql for all statements")
  void testParseSql() {
    SqlSupport.muteParsingError();
    final List<Statement> stmts = Statement.findAll();

    for (Statement stmt : stmts) {
      final String dbType = stmt.app().dbType();
      final String sql0 = stmt.rawSql();
      final SqlNode ast0 = SqlSupport.parseSql(dbType, sql0);
      if (ast0 == null) {
        System.out.println("skipped: " + stmt);
        continue;
      }

      final String sql1 = ast0.toString();
      assertFalse(sql1.contains("<??>"));
      final SqlNode ast1 = SqlSupport.parseSql(dbType, sql1);
      assertNotNull(ast1);
      assertEquals(sql1, ast1.toString());
    }
  }

  @Test
  @DisplayName("[Stmt] build plan for all statements")
  void testAssemblePlan() {
    SqlSupport.muteParsingError();
    final List<Statement> stmts = Statement.findAll();
    final String latch = "";
    boolean started = latch.isEmpty();

    for (Statement stmt : stmts) {
      if (latch.equals(stmt.toString())) started = true;
      if (!started) continue;

      final String dbType = stmt.app().dbType();
      final String sql0 = stmt.rawSql();
      final SqlNode ast = SqlSupport.parseSql(dbType, sql0);
      if (ast == null || !PlanSupport.isSupported(ast)) {
        System.out.println("skipped: " + stmt);
        continue;
      }

      final PlanContext plan = PlanSupport.assemblePlan(ast, stmt.app().schema("base"));
      assertNotNull(plan);
    }
  }

  @Test
  void test() {
    final String sql = "Select a.i, max(a.j) from a group by a.i";
    final SqlNode ast = SqlSupport.parseSql(MySQL, sql);
    final Schema schema = App.of("test").schema("base");
    final PlanContext plan = PlanSupport.assemblePlan(ast, schema);
    System.out.println(plan);
  }
}
