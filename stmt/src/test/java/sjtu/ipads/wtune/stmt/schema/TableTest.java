package sjtu.ipads.wtune.stmt.schema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.SQLParser;

import static org.junit.jupiter.api.Assertions.*;

public class TableTest {
  @Test
  @DisplayName("from CREATE TABLE")
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
    final Table table = new Table().fromCreateTable(SQLParser.mysql().parse(createTable));

    {
      assertEquals("public", table.schemaName());
      assertEquals("t", table.tableName());
      assertEquals("myisam", table.engine());
      assertEquals(3, table.columns().size());
      assertEquals(7, table.constraints().size());
    }

    {
      final Column col0 = table.getColumn("i");
      assertTrue(col0.primaryKeyPart());
      assertTrue(col0.foreignKeyPart());
      assertFalse(col0.autoIncrement());
      assertFalse(col0.hasCheck());
    }

    {
      final Column col1 = table.getColumn("j");
      assertFalse(col1.primaryKeyPart());
      assertTrue(col1.notNull());
      assertTrue(col1.hasDefault());
      assertTrue(col1.indexPart());
      assertTrue(col1.uniquePart());
    }

    {
      final Column col2 = table.getColumn("k");
      assertFalse(col2.primaryKeyPart());
      assertTrue(col2.hasCheck());
      assertTrue(col2.autoIncrement());
      assertTrue(col2.foreignKeyPart());
    }
  }
}
