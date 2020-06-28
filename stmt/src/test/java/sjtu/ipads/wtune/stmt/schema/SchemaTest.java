package sjtu.ipads.wtune.stmt.schema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLParser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class SchemaTest {
  @Test
  @DisplayName("[stmt.schema] from CREATE TABLE")
  void test() {
    final String createTable =
        "create table `public`.t ("
            + "`i` int(10) primary key references b(x),"
            + "j varchar(512) NOT NULL DEFAULT 'a',"
            + "k int AUTO_INCREMENT CHECK (k < 100),"
            + "index (j(100)),"
            + "unique (j DESC) using rtree,"
            + "constraint fk_cons foreign key fk (k) references b(y)"
            + ") ENGINE = 'myisam';"
            + "create table b (x int(10), y int);";

    final SQLParser parser = SQLParser.mysql();
    final Schema schema = new Schema();

    listMap(parser::parse, SQLParser.splitSql(createTable)).forEach(schema::addDefinition);
    schema.buildRefs();

    assertEquals(2, schema.tables().size());

    final Table table0 = schema.getTable("t");
    final Table table1 = schema.getTable("b");
    {
      final Column col0 = table0.getColumn("i");
      final Constraint constraint = col0.foreignKeyConstraint();
      assertSame(table1, constraint.refTable());

      final List<Column> refCols = constraint.refColumns();
      assertSame(table1.getColumn("x"), refCols.get(0));
    }
    {
      final Column col0 = table0.getColumn("k");
      final Constraint constraint = col0.foreignKeyConstraint();
      assertSame(table1, constraint.refTable());

      final List<Column> refCols = constraint.refColumns();
      assertSame(table1.getColumn("y"), refCols.get(0));
    }
  }
}
