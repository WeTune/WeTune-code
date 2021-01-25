package sjtu.ipads.wtune.sqlparser.mysql;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.rel.Column;
import sjtu.ipads.wtune.sqlparser.rel.Schema;
import sjtu.ipads.wtune.sqlparser.rel.Table;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.sqlparser.ast.SQLNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.rel.Column.Flag.*;

public class TableTest {
  @Test
  @DisplayName("[Stmt.Table] from CREATE TABLE")
  void test() {
    final String createTable =
        ""
            + "create table `public`.t ("
            + "`i` int(10) primary key references b(x),"
            + "j varchar(512) NOT NULL DEFAULT 'a',"
            + "k int AUTO_INCREMENT CHECK (k < 100),"
            + "index (j(100)),"
            + "unique (j DESC) using rtree,"
            + "constraint fk_cons foreign key fk (k) references b(y)"
            + ") ENGINE = 'myisam';";
    final Schema schema = Schema.parse(MYSQL, createTable);
    final Table table = schema.table("t");

    {
      assertEquals("public", table.schema());
      assertEquals("t", table.name());
      assertEquals("myisam", table.engine());
      assertEquals(3, table.columns().size());
      assertEquals(7, table.constraints().size());
    }

    {
      final Column col0 = table.column("i");
      assertTrue(col0.isFlagged(PRIMARY));
      assertTrue(col0.isFlagged(FOREIGN_KEY));
      assertFalse(col0.isFlagged(AUTO_INCREMENT));
      assertFalse(col0.isFlagged(HAS_CHECK));
    }

    {
      final Column col1 = table.column("j");
      assertFalse(col1.isFlagged(PRIMARY));
      assertTrue(col1.isFlagged(NOT_NULL));
      assertTrue(col1.isFlagged(HAS_DEFAULT));
      assertTrue(col1.isFlagged(INDEXED));
      assertTrue(col1.isFlagged(UNIQUE));
    }

    {
      final Column col2 = table.column("k");
      assertFalse(col2.isFlagged(PRIMARY));
      assertTrue(col2.isFlagged(HAS_CHECK));
      assertTrue(col2.isFlagged(AUTO_INCREMENT));
      assertTrue(col2.isFlagged(FOREIGN_KEY));
    }
  }
}
