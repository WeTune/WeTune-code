package sjtu.ipads.wtune.systhesis.operators;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.resovler.ColumnResolver;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_COLUMN_REF;

class ReplaceColumnRefTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[Synthesis.Operator.ReplaceColumnRef]")
  void test() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    {
      stmt.setRawSql("select a.i, b.x from a join b on a.j = b.y where i = 1 order by i");
      stmt.resolve(ColumnResolver.class);
      final List<SQLNode> selectItems = stmt.parsed().get(QUERY_BODY).get(QUERY_SPEC_SELECT_ITEMS);
      final ColumnRef target = selectItems.get(0).get(SELECT_ITEM_EXPR).get(RESOLVED_COLUMN_REF);

      final Operator op = ReplaceColumnRef.build(target, "b", "x");
      op.apply(stmt.parsed());
      assertEquals(
          "SELECT `b`.`x`, `b`.`x` FROM `a` INNER JOIN `b` ON `a`.`j` = `b`.`y` "
              + "WHERE `b`.`x` = 1 ORDER BY `b`.`x`",
          stmt.parsed().toString());
    }
  }
}
