package sjtu.ipads.wtune.stmt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.stmt.TestHelper.fastRecycleIter;
import static sjtu.ipads.wtune.stmt.mutator.Mutation.*;
import static sjtu.ipads.wtune.stmt.resolver.Resolution.*;

public class AllStatementsTest {
  @Test
  @DisplayName("[Stmt] parsing all schema")
  void testSchema() {
    Statement.findAll().forEach(Statement::app);
    for (App app : App.all()) app.schema("base");
  }

  @Test
  @DisplayName("[Stmt] parsing all statements")
  void testStatement() {
    final List<Statement> stmts = Statement.findAll();

    for (Statement stmt : fastRecycleIter(stmts)) {
      final ASTNode parsed = stmt.parsed();
      if (parsed == null) {
        System.out.println(stmt.toString());
        continue;
      }
      assertFalse(parsed.toString().contains("<??>"));
      final Statement stmt1 = Statement.make(stmt.appName(), stmt.parsed().toString(), null);
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
      stmt.parsed().context().setSchema(stmt.app().schema("base"));

      clean(stmt.parsed());
      normalizeBool(stmt.parsed());
      normalizeTuple(stmt.parsed());
      normalizeConstantTable(stmt.parsed());

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
        resolveBoolExpr(stmt.parsed());
        resolveParamSimple(stmt.parsed());
        assertFalse(stmt.parsed().toString().contains("<??>"));
      }
    }
  }

  @Test
  @DisplayName("[Stmt] resolve all statements")
  void testResolveFull() {
    final List<Statement> stmts = Statement.findAll();
    for (Statement stmt : fastRecycleIter(stmts)) {
      if (stmt.parsed() != null) {
        resolveParamFull(stmt.parsed());
        assertFalse(stmt.parsed().toString().contains("<??>"));
      }
    }
  }
}
