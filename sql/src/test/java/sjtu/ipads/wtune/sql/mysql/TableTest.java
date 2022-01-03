package sjtu.ipads.wtune.sql.mysql;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sql.SqlSupport;
import sjtu.ipads.wtune.sql.schema.Column;
import sjtu.ipads.wtune.sql.schema.Schema;
import sjtu.ipads.wtune.sql.schema.Table;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.sql.ast.ASTNode.MYSQL;
import static sjtu.ipads.wtune.sql.schema.Column.Flag.*;

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
    final Schema schema = SqlSupport.parseSchema(MYSQL, createTable);
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
      assertTrue(col0.isFlag(PRIMARY));
      assertTrue(col0.isFlag(FOREIGN_KEY));
      assertFalse(col0.isFlag(AUTO_INCREMENT));
      assertFalse(col0.isFlag(HAS_CHECK));
    }

    {
      final Column col1 = table.column("j");
      assertFalse(col1.isFlag(PRIMARY));
      assertTrue(col1.isFlag(NOT_NULL));
      assertTrue(col1.isFlag(HAS_DEFAULT));
      assertTrue(col1.isFlag(INDEXED));
      assertTrue(col1.isFlag(UNIQUE));
    }

    {
      final Column col2 = table.column("k");
      assertFalse(col2.isFlag(PRIMARY));
      assertTrue(col2.isFlag(HAS_CHECK));
      assertTrue(col2.isFlag(AUTO_INCREMENT));
      assertTrue(col2.isFlag(FOREIGN_KEY));
    }
  }
}
