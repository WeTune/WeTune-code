package sjtu.ipads.wtune.stmt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sjtu.ipads.wtune.stmt.mutator.Mutation.clean;
import static sjtu.ipads.wtune.stmt.mutator.Mutation.normalizeBool;
import static sjtu.ipads.wtune.stmt.mutator.Mutation.normalizeConstantTable;
import static sjtu.ipads.wtune.stmt.mutator.Mutation.normalizeTuple;
import static sjtu.ipads.wtune.stmt.resolver.Resolution.resolveBoolExpr;
import static sjtu.ipads.wtune.stmt.resolver.Resolution.resolveParamFull;
import static sjtu.ipads.wtune.stmt.resolver.Resolution.resolveParamSimple;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sql.ast.ASTNode;

public class AllStatementsTest {
  @Test
  @DisplayName("[Stmt] parsing all schema")
  void testSchema() {
    for (App app : App.all()) app.schema("base");
  }

  @Test
  @DisplayName("[Stmt] parsing all statements")
  void testStatement() {
    final List<Statement> stmts = Statement.findAll();

    for (Statement stmt : stmts) {
      final ASTNode parsed = stmt.parsed();
      if (parsed == null) {
        System.out.println(stmt);
        continue;
      }
      assertFalse(parsed.toString().contains("<??>"));
      final Statement stmt1 = Statement.mk(stmt.appName(), stmt.parsed().toString(), null);
      assertNotNull(stmt1.parsed(), stmt.toString());
      assertEquals(stmt.parsed().toString(), stmt1.parsed().toString(), stmt.toString());
    }
  }

  @Test
  @DisplayName("[Stmt] mutate all statements")
  void testMutate() {
    final List<Statement> stmts = Statement.findAll();
    for (Statement stmt : stmts) {
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
    for (Statement stmt : stmts) {
      if (stmt.parsed() != null) {
        stmt.parsed().context().setSchema(stmt.app().schema("base", false));
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
    for (Statement stmt : stmts) {
      if (stmt.parsed() != null) {
        stmt.parsed().context().setSchema(stmt.app().schema("base", false));
        resolveParamFull(stmt.parsed());
        assertFalse(stmt.parsed().toString().contains("<??>"));
      }
    }
  }

  @Test
  void test() {
    final Statement stmt = Statement.findOne("pybbs", 66);
    stmt.parsed().context().setSchema(stmt.app().schema("base", false));
    resolveParamFull(stmt.parsed());
  }
}
