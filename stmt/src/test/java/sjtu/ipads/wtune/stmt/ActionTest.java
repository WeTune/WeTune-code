package sjtu.ipads.wtune.stmt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.stmt.mutator.Mutation;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ActionTest {

  @Test
  @DisplayName("[Stmt.Mutator] remove bool constant")
  void testSimple() {
    final Function<String, Statement> makeStmt = sql -> Statement.build("test", sql, null);
    {
      final String sql = "select a from t where 1=1";
      final Statement stmt = makeStmt.apply(sql);
      Mutation.clean(stmt.parsed());
      assertEquals("SELECT `a` FROM `t`", stmt.parsed().toString());
    }
    {
      final String sql = ("select a from t where true and b");
      final Statement stmt = makeStmt.apply(sql);
      Mutation.clean(stmt.parsed());
      assertEquals("SELECT `a` FROM `t` WHERE `b`", stmt.parsed().toString());
    }
    {
      final String sql = ("select a from t where 1+1 = 2 and (c or 1 between 0 and 2)");
      final Statement stmt = makeStmt.apply(sql);
      Mutation.clean(stmt.parsed());
      assertEquals("SELECT `a` FROM `t` WHERE `c`", stmt.parsed().toString());
    }
    {
      final String sql = ("select a from t where now() = 0 or rand() = 10 or field(k, 1, 2) = 3");
      final Statement stmt = makeStmt.apply(sql);
      Mutation.clean(stmt.parsed());
      assertEquals(
          "SELECT `a` FROM `t` WHERE RAND() = 10 OR FIELD(`k`, 1, 2) = 3",
          stmt.parsed().toString());
    }
  }

  @Test
  @DisplayName("[Stmt.Mutator] join text constant")
  void testConcat() {
    final Function<String, Statement> makeStmt = sql -> Statement.build("test", sql, null);
    {
      final String sql = ("select a from t where a like concat('%', concat('1', '%'))");
      final Statement stmt = makeStmt.apply(sql);
      Mutation.clean(stmt.parsed());
      assertEquals("SELECT `a` FROM `t` WHERE `a` LIKE '%1%'", stmt.parsed().toString());
    }
  }

  @Test
  @DisplayName("[Stmt.Mutator] normalize tuple")
  void testNormalizeTuple() {
    final Function<String, Statement> makeStmt = sql -> Statement.build("test", sql, null);
    {
      final String sql = "select a from t where (a, b) in ((1,2),(3,4))";
      final Statement stmt = makeStmt.apply(sql);
      Mutation.normalizeTuple(stmt.parsed());
      assertEquals("SELECT `a` FROM `t` WHERE (`a`, `b`) IN (?)", stmt.parsed().toString());
    }
  }

  @Test
  @DisplayName("[Stmt.Mutator] normalize bool")
  void testNormalizeBool() {
    final Statement stmt =
        Statement.build(
            "test", "select * from a where a.i or (a.j is false and (a.j = a.i) is false)", null);
    Mutation.normalizeBool(stmt.parsed());
    assertEquals(
        "SELECT * FROM `a` WHERE `a`.`i` IS TRUE OR NOT `a`.`j` IS TRUE AND NOT `a`.`j` = `a`.`i`",
        stmt.parsed().toString());
  }
}
