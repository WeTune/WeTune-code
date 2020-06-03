package sjtu.ipads.wtune.sqlparser.mysql;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.SQLNode;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.Type.CREATE_TABLE;
import static sjtu.ipads.wtune.sqlparser.SQLNode.Type.REFERENCES;
import static sjtu.ipads.wtune.sqlparser.mysql.MySQLASTParser.parse;

public class MySQLASTBuilderTest {
  @Test
  @DisplayName("create table")
  void testCreateTable() {
    final String createTable =
        ""
            + "create table `public`.t ("
            + "`i` int(10) primary key references b(x),"
            + "j varchar(512) NOT NULL DEFAULT 'a',"
            + "k int AUTO_INCREMENT CHECK (k < 100),"
            + "index (j(100)),"
            + "unique (j DESC) using rtree,"
            + "constraint fk_cons foreign key fk (k) references b(y)"
            + ") ENGINE = 'innodb';";
    final var root = parse(createTable, MySQLParser::createStatement);
    assertEquals(CREATE_TABLE, root.type());

    final var tableName = root.get(CREATE_TABLE_NAME);
    assertEquals("public", tableName.get(TABLE_NAME_SCHEMA));
    assertEquals("t", tableName.get(TABLE_NAME_TABLE));

    assertEquals("innodb", root.get(CREATE_TABLE_ENGINE));

    final var columns = root.get(CREATE_TABLE_COLUMNS);
    final var constraints = root.get(CREATE_TABLE_CONSTRAINTS);

    assertEquals(3, columns.size());
    assertEquals(3, constraints.size());

    {
      final var col0 = columns.get(0);

      {
        final var col0Name = col0.get(COLUMN_DEF_NAME);
        assertNull(col0Name.get(COLUMN_NAME_SCHEMA));
        assertNull(col0Name.get(COLUMN_NAME_TABLE));
        assertEquals("i", col0Name.get(COLUMN_NAME_COLUMN));
      }

      {
        assertEquals("int(10)", col0.get(COLUMN_DEF_DATATYPE));
        assertFalse(col0.isFlagged(COLUMN_DEF_AUTOINCREMENT));
        assertFalse(col0.isFlagged(COLUMN_DEF_DEFAULT));
        assertFalse(col0.isFlagged(COLUMN_DEF_GENERATED));
      }

      {
        final var col0Cons = col0.get(COLUMN_DEF_CONS);
        assertTrue(col0Cons.contains(Constraint.PRIMARY));
        assertFalse(col0Cons.contains(Constraint.UNIQUE));
        assertFalse(col0Cons.contains(Constraint.CHECK));
        assertFalse(col0Cons.contains(Constraint.NOT_NULL));
        assertFalse(col0Cons.contains(Constraint.FOREIGN));

        final var col0Refs = col0.get(COLUMN_DEF_REF);
        final var col0RefTable = col0Refs.get(REFERENCES_TABLE);
        final var col0RefCols = col0Refs.get(REFERENCES_COLUMNS);
        assertNull(col0RefTable.get(TABLE_NAME_SCHEMA));
        assertEquals("b", col0RefTable.get(TABLE_NAME_TABLE));
        assertEquals(1, col0RefCols.size());
        final var col0RefCol0 = col0RefCols.get(0);
        assertNull(col0RefCol0.get(COLUMN_NAME_SCHEMA));
        assertNull(col0RefCol0.get(COLUMN_NAME_TABLE));
        assertEquals("x", col0RefCol0.get(COLUMN_NAME_COLUMN));
      }
    }

    {
      {
        final var cons0 = constraints.get(0);
        assertNull(cons0.get(INDEX_DEF_NAME));
        assertNull(cons0.get(INDEX_DEF_CONS));
        assertNull(cons0.get(INDEX_DEF_TYPE));
        assertNull(cons0.get(INDEX_DEF_REFS));
        final var keys = cons0.get(INDEX_DEF_KEYS);
        assertEquals(1, keys.size());

        final var key0 = keys.get(0);
        assertNull(key0.get(KEY_PART_DIRECTION));
        assertEquals("j", key0.get(KEY_PART_COLUMN));
        assertEquals(100, key0.get(KEY_PART_LEN));
      }

      {
        final var cons1 = constraints.get(1);
        assertNull(cons1.get(INDEX_DEF_NAME));
        assertNull(cons1.get(INDEX_DEF_REFS));
        assertEquals(Constraint.UNIQUE, cons1.get(INDEX_DEF_CONS));
        assertEquals(IndexType.RTREE, cons1.get(INDEX_DEF_TYPE));
        final var keys = cons1.get(INDEX_DEF_KEYS);
        assertEquals(1, keys.size());

        final var key0 = keys.get(0);
        assertEquals(KeyDirection.DESC, key0.get(KEY_PART_DIRECTION));
        assertEquals("j", key0.get(KEY_PART_COLUMN));
        assertNull(key0.get(KEY_PART_LEN));
      }

      {
        final var cons2 = constraints.get(2);
        assertEquals("fk", cons2.get(INDEX_DEF_NAME));
        assertEquals(Constraint.FOREIGN, cons2.get(INDEX_DEF_CONS));
        assertNull(cons2.get(INDEX_DEF_TYPE));
        final var keys = cons2.get(INDEX_DEF_KEYS);
        assertEquals(1, keys.size());

        final var key0 = keys.get(0);
        assertNull(key0.get(KEY_PART_DIRECTION));
        assertNull(key0.get(KEY_PART_LEN));
        assertEquals("k", key0.get(KEY_PART_COLUMN));

        final var refs = cons2.get(INDEX_DEF_REFS);
        final var refTable = refs.get(REFERENCES_TABLE);
        final var refCols = refs.get(REFERENCES_COLUMNS);
        assertEquals("b", refTable.get(TABLE_NAME_TABLE));
        assertEquals(1, refCols.size());
        assertEquals("y", refCols.get(0).get(COLUMN_NAME_COLUMN));
      }
    }

    final String expected =
        "CREATE TABLE `public`.`t` (\n"
            + "  `i` int(10) PRIMARY KEY REFERENCES `b`(`x`),\n"
            + "  `j` varchar(512) NOT NULL,\n"
            + "  `k` int AUTO_INCREMENT,\n"
            + "  KEY (`j`(100)),\n"
            + "  UNIQUE KEY (`j` DESC) USING RTREE ,\n"
            + "  FOREIGN KEY `fk`(`k`) REFERENCES `b`(`y`)\n"
            + ") ENGINE = 'innodb'";
    assertEquals(expected, root.toString());
  }
}
