package sjtu.ipads.wtune.systhesis.operators;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.resovler.BoolPrimitiveResolver;
import sjtu.ipads.wtune.stmt.resovler.ColumnResolver;
import sjtu.ipads.wtune.stmt.resovler.IdResolver;
import sjtu.ipads.wtune.stmt.statement.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemovePredicateTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[Synthesis.Operator.RemovePredicate]")
  void test() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    {
      stmt.setRawSql(
          "select 1 from a where a.i = 1 and a.j = 3 "
              + "or a.k in (select 1 from b where b.x = 5)");
      stmt.resolve(IdResolver.class);
      stmt.resolve(BoolPrimitiveResolver.class);
      stmt.resolve(ColumnResolver.class);

      stmt.setParsed(stmt.parsed().copy());


      RemovePredicate.build(16L).apply(stmt.parsed());
      RemovePredicate.build(8L).apply(stmt.parsed());

      assertEquals("SELECT 1 FROM `a` WHERE `a`.`j` = 3", stmt.parsed().toString());
    }
  }
}
