package sjtu.ipads.wtune.stmt;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.stmt.mutator.Mutation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.stmt.TestHelper.fastRecycleIter;
import static sjtu.ipads.wtune.stmt.analyzer.Analysis.inferForeignKey;
import static sjtu.ipads.wtune.stmt.mutator.Mutation.*;
import static sjtu.ipads.wtune.stmt.resolver.Resolution.*;

public class AllStatementsTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[Stmt] parsing all schema")
  void testSchema() {
    Statement.findAll().forEach(Statement::appContext);
    for (App app : App.all()) app.schema("base");
  }

  @Test
  @DisplayName("[Stmt] parsing all statements")
  void testStatement() {
    final List<Statement> stmts = Statement.findAll();

    for (Statement stmt : fastRecycleIter(stmts)) {
      final SQLNode parsed = stmt.parsed();
      if (parsed == null) {
        System.out.println(stmt.toString());
        continue;
      }
      assertFalse(parsed.toString().contains("<??>"));
      final Statement stmt1 = Statement.build(stmt.appName(), stmt.parsed().toString(), null);
      assertNotNull(stmt1.parsed(), stmt.toString());
      assertEquals(stmt.parsed().toString(), stmt1.parsed().toString(), stmt.toString());
    }
  }

  @Test
  @DisplayName("[Stmt] mutate all statements")
  void testMutate() {
    final List<Statement> stmts = Statement.findAll();
    for (Statement stmt : fastRecycleIter(stmts)) {
      if (stmt.parsed() == null) continue;

      final String original = stmt.parsed().toString();

      clean(stmt.parsed());
      normalizeBool(stmt.parsed());
      normalizeTuple(stmt.parsed());

      final String modified = stmt.parsed().toString();

      assertFalse(modified.contains("<??>"));
      assertTrue(!original.contains("1 = 1") || !modified.contains("1 = 1"));
      assertTrue(!original.contains("1 = 0") || !modified.contains("1 = 0"));
    }
  }

  @Test
  @DisplayName("[Stmt] resolve all statements")
  void testResolve() {
    final List<Statement> stmts = Statement.findAll();
    for (Statement stmt : fastRecycleIter(stmts)) {
      if (stmt.parsed() != null) {
        clean(stmt.parsed());
        normalizeBool(stmt.parsed());
        normalizeTuple(stmt.parsed());
        resolveBoolExpr(stmt);
        resolveQueryScope(stmt);
        resolveTable(stmt);
        // TODO
        //        resolveColumnRef(stmt);
        //        resolveJoinCondition(stmt);
        //        inferForeignKey(stmt.parsed());
        assertFalse(stmt.parsed().toString().contains("<??>"));
      }
    }
  }
}
