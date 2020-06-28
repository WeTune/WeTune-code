package sjtu.ipads.wtune.sqlparser.mysql;

import org.antlr.v4.runtime.ParserRuleContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.mysql.internal.MySQLParser;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.Kind.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.Type.CREATE_TABLE;
import static sjtu.ipads.wtune.sqlparser.mysql.MySQLRecognizerCommon.PipesAsConcat;

public class MySQLASTBuilderTest {
  private static final MySQLASTParser PARSER = new MySQLASTParser();

  private static class TestHelper {
    private String sql;
    private SQLNode node;
    private final Function<MySQLParser, ParserRuleContext> rule;

    private TestHelper(Function<MySQLParser, ParserRuleContext> rule) {
      this.rule = rule;
    }

    private SQLNode sql(String sql) {
      if (sql != null) return (node = PARSER.parse(sql, rule));
      return null;
    }
  }

  @AfterEach
  void reset() {
    PARSER.setServerVersion(0);
    PARSER.setSqlMode(0);
  }

  @Test
  @DisplayName("[sqlparser.mysql] create table")
  void testCreateTable() {
    final TestHelper helper = new TestHelper(MySQLParser::createStatement);
    {
      final String createTable =
          ""
              + "create table `public`.t ("
              + "`i` int(10) primary key references b(x),"
              + "j varchar(512) NOT NULL DEFAULT 'a',"
              + "k int AUTO_INCREMENT CHECK (k < 100),"
              + "index (j(100)),"
              + "unique (j DESC) using rtree,"
              + "constraint fk_cons foreign key fk (k) references b(y),"
              + "fulltext (j),"
              + "spatial (k,i)"
              + ") ENGINE = 'innodb';";

      final SQLNode root = helper.sql(createTable);
      assertEquals(CREATE_TABLE, root.type());

      final var tableName = root.get(CREATE_TABLE_NAME);
      assertEquals("public", tableName.get(TABLE_NAME_SCHEMA));
      assertEquals("t", tableName.get(TABLE_NAME_TABLE));

      assertEquals("innodb", root.get(CREATE_TABLE_ENGINE));

      final var columns = root.get(CREATE_TABLE_COLUMNS);
      final var constraints = root.get(CREATE_TABLE_CONSTRAINTS);

      assertEquals(3, columns.size());
      assertEquals(5, constraints.size());

      {
        final var col0 = columns.get(0);

        {
          final var col0Name = col0.get(COLUMN_DEF_NAME);
          assertNull(col0Name.get(COLUMN_NAME_SCHEMA));
          assertNull(col0Name.get(COLUMN_NAME_TABLE));
          assertEquals("i", col0Name.get(COLUMN_NAME_COLUMN));
        }

        {
          assertEquals("int(10)", col0.get(COLUMN_DEF_DATATYPE_RAW));
          assertFalse(col0.isFlagged(COLUMN_DEF_AUTOINCREMENT));
          assertFalse(col0.isFlagged(COLUMN_DEF_DEFAULT));
          assertFalse(col0.isFlagged(COLUMN_DEF_GENERATED));
        }

        {
          final var col0Cons = col0.get(COLUMN_DEF_CONS);
          assertTrue(col0Cons.contains(ConstraintType.PRIMARY));
          assertFalse(col0Cons.contains(ConstraintType.UNIQUE));
          assertFalse(col0Cons.contains(ConstraintType.CHECK));
          assertFalse(col0Cons.contains(ConstraintType.NOT_NULL));
          assertFalse(col0Cons.contains(ConstraintType.FOREIGN));

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
          assertEquals(ConstraintType.UNIQUE, cons1.get(INDEX_DEF_CONS));
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
          assertEquals(ConstraintType.FOREIGN, cons2.get(INDEX_DEF_CONS));
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
              + "  FOREIGN KEY `fk`(`k`) REFERENCES `b`(`y`),\n"
              + "  FULLTEXT KEY (`j`),\n"
              + "  SPATIAL KEY (`k`, `i`)\n"
              + ") ENGINE = 'innodb'";
      assertEquals(expected, root.toString(false));
    }
    {
      PARSER.setServerVersion(80013);
      final String createTable = "create table t (i int, primary key ((i+1)));";
      final SQLNode node = helper.sql(createTable);
      assertEquals("CREATE TABLE `t` ( `i` int, PRIMARY KEY ((`i` + 1)) )", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] variable")
  void testVariable() {
    String sql;
    SQLNode node;

    {
      sql = "@`a`=1";
      node = PARSER.parse(sql, MySQLParser::variable);
      assertEquals(VariableScope.USER, node.get(VARIABLE_SCOPE));
      assertEquals("a", node.get(VARIABLE_NAME));
      // TODO: assertEquals("@a=1", node.toString())
      // TODO: assertNotNull(node.get(VARIABLE_ASSIGNMENT));
    }

    {
      sql = "@'a'=1";
      node = PARSER.parse(sql, MySQLParser::variable);
      assertEquals(VariableScope.USER, node.get(VARIABLE_SCOPE));
      assertEquals("a", node.get(VARIABLE_NAME));
    }

    {
      sql = "@@system.var";
      node = PARSER.parse(sql, MySQLParser::variable);
      assertEquals(VariableScope.SYSTEM_GLOBAL, node.get(VARIABLE_SCOPE));
      assertEquals("@@GLOBAL.system.var", node.toString());
    }

    {
      sql = "@@ session.system.var";
      node = PARSER.parse(sql, MySQLParser::variable);
      assertEquals(VariableScope.SYSTEM_SESSION, node.get(VARIABLE_SCOPE));
      assertEquals("system.var", node.get(VARIABLE_NAME));
      assertEquals("@@SESSION.system.var", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] column ref")
  void testColumnRef() {
    final TestHelper helper = new TestHelper(MySQLParser::columnRef);

    {
      final SQLNode node = helper.sql("a.b.c");
      assertEquals("`a`.`b`.`c`", node.get(COLUMN_REF_COLUMN).toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] json ref")
  void testJsonRef() {
    PARSER.setServerVersion(80000);
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);

    {
      final SQLNode node = helper.sql("a->'$.b'");
      assertEquals(FUNC_CALL, node.get(EXPR_KIND));
      final List<SQLNode> args = node.get(FUNC_CALL_ARGS);
      assertEquals(2, args.size());

      assertEquals("json_extract", node.get(FUNC_CALL_NAME));
      assertEquals("`a`", args.get(0).toString());
      assertEquals("'$.b'", args.get(1).toString());
      assertEquals("JSON_EXTRACT(`a`, '$.b')", node.toString());
    }

    {
      final SQLNode node = helper.sql("a->>'$.b'");
      assertEquals(FUNC_CALL, node.get(EXPR_KIND));
      final List<SQLNode> args = node.get(FUNC_CALL_ARGS);

      assertEquals(1, args.size());
      assertEquals("json_unquote", node.get(FUNC_CALL_NAME));
      assertEquals("JSON_UNQUOTE(JSON_EXTRACT(`a`, '$.b'))", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] literal")
  void testLiteral() {
    final TestHelper helper = new TestHelper(MySQLParser::literal);

    {
      final SQLNode node = helper.sql("'abc' 'def'");
      assertEquals(LiteralType.TEXT, node.get(LITERAL_TYPE));
      assertEquals("abcdef", node.get(LITERAL_VALUE));
      assertEquals("'abcdef'", node.toString());
    }
    {
      final SQLNode node = helper.sql("123");
      assertEquals(LiteralType.INTEGER, node.get(LITERAL_TYPE));
      assertEquals(123, node.get(LITERAL_VALUE));
      assertEquals("123", node.toString());
    }
    {
      final SQLNode node = helper.sql("123.123");
      assertEquals(LiteralType.FRACTIONAL, node.get(LITERAL_TYPE));
      assertEquals(123.123, node.get(LITERAL_VALUE));
      assertEquals("123.123", node.toString());
    }
    {
      final SQLNode node = helper.sql("null");
      assertEquals(LiteralType.NULL, node.get(LITERAL_TYPE));
      assertNull(node.get(LITERAL_VALUE));
      assertEquals("NULL", node.toString());
    }
    {
      final SQLNode node = helper.sql("true");
      assertEquals(LiteralType.BOOL, node.get(LITERAL_TYPE));
      assertEquals(true, node.get(LITERAL_VALUE));
      assertEquals("TRUE", node.toString());
    }
    {
      final SQLNode node = helper.sql("timestamp '2020-01-01 00:00:00.000'");
      assertEquals(LiteralType.TEMPORAL, node.get(LITERAL_TYPE));
      assertEquals("2020-01-01 00:00:00.000", node.get(LITERAL_VALUE));
      assertEquals("timestamp", node.get(LITERAL_UNIT));
      assertEquals("TIMESTAMP '2020-01-01 00:00:00.000'", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] func call")
  void testFuncCall() {
    final TestHelper helper = new TestHelper(MySQLParser::expr);
    {
      final SQLNode node = helper.sql("now()");
      assertEquals("NOW()", node.toString());
    }
    {
      final SQLNode node = helper.sql("current_user()");
      assertEquals("CURRENT_USER()", node.toString());
    }
    {
      PARSER.setServerVersion(50604);
      final SQLNode node = helper.sql("curtime(0)");
      assertEquals("CURTIME(0)", node.toString());
    }
    {
      final SQLNode node = helper.sql("COALESCE(1,a,b+1)");
      assertEquals("COALESCE(1, `a`, `b` + 1)", node.toString());
    }
    {
      final SQLNode node = helper.sql("concat(1,a,b+1)");
      assertEquals("CONCAT(1, `a`, `b` + 1)", node.toString());
    }
    {
      final SQLNode node = helper.sql("char(1,a,b+1)");
      assertEquals("CHAR(1, `a`, `b` + 1)", node.toString());
    }
    {
      final SQLNode node = helper.sql("adddate(a,interval 10 day)");
      assertEquals("ADDDATE(`a`, INTERVAL 10 DAY)", node.toString());
    }
    {
      final SQLNode node = helper.sql("extract(day from a)");
      assertEquals("EXTRACT(DAY FROM `a`)", node.toString());
    }
    {
      final SQLNode node = helper.sql("get_format(date, 'USA')");
      assertEquals("GET_FORMAT(DATE, 'USA')", node.toString());
    }
    {
      final SQLNode node = helper.sql("position('a' in a)");
      assertEquals("POSITION('a' IN `a`)", node.toString());
    }
    {
      final SQLNode node = helper.sql("timestamp_diff(second,a,b)");
      assertEquals("TIMESTAMP_DIFF(SECOND, `a`, `b`)", node.toString());
    }
    {
      final SQLNode node = helper.sql("old_password('abc')");
      assertEquals("OLD_PASSWORD('abc')", node.toString());
    }
    {
      final SQLNode node = helper.sql("right(1,a)");
      assertEquals("RIGHT(1, `a`)", node.toString());
    }
    {
      SQLNode node = helper.sql("trim(leading 'abc' from a)");
      assertEquals("TRIM(LEADING 'abc' FROM `a`)", node.toString());
      node = helper.sql("trim(trailing 'abc' from a)");
      assertEquals("TRIM(TRAILING 'abc' FROM `a`)", node.toString());
      node = helper.sql("trim(both 'abc' from a)");
      assertEquals("TRIM(BOTH 'abc' FROM `a`)", node.toString());
      node = helper.sql("trim(a)");
      assertEquals("TRIM(`a`)", node.toString());
      node = helper.sql("trim(both from a)");
      assertEquals("TRIM(BOTH FROM `a`)", node.toString());
    }
    {
      SQLNode node = helper.sql("substring(a,'a',1)");
      assertEquals("SUBSTRING(`a`, 'a', 1)", node.toString());
    }
    {
      SQLNode node = helper.sql("geometrycollection(a,b)");
      assertEquals("GEOMETRYCOLLECTION(`a`, `b`)", node.toString());
      node = helper.sql("geometrycollection()");
      assertEquals("GEOMETRYCOLLECTION()", node.toString());
      node = helper.sql("linestring(a,b)");
      assertEquals("LINESTRING(`a`, `b`)", node.toString());
      node = helper.sql("point(a,b)");
      assertEquals("POINT(`a`, `b`)", node.toString());
    }
    {
      SQLNode node = helper.sql("my.myfunc(a,b)");
      assertEquals("MYFUNC(`a`, `b`)", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] collation")
  void testCollation() {
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);
    {
      final SQLNode node = helper.sql("a collate 'utf8'");
      assertEquals(COLLATE, node.get(EXPR_KIND));
      assertEquals("`a`", node.get(COLLATE_EXPR).toString());
      assertEquals("utf8", node.get(COLLATE_COLLATION));
      assertEquals("`a` COLLATE 'utf8'", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] param marker")
  void testParamMarker() {
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);
    final SQLNode node = helper.sql("?");
    assertEquals(PARAM_MARKER, node.get(EXPR_KIND));
  }

  @Test
  @DisplayName("[sqlparser.mysql] concat ")
  void testConcatPipe() {
    PARSER.setSqlMode(PipesAsConcat);
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);
    final SQLNode node = helper.sql("'a' || b");
    assertEquals(FUNC_CALL, node.get(EXPR_KIND));
    assertEquals("concat", node.get(FUNC_CALL_NAME));
    assertEquals(2, node.get(FUNC_CALL_ARGS).size());
    assertEquals("CONCAT('a', `b`)", node.toString());
  }

  @Test
  @DisplayName("[sqlparser.mysql] unary")
  void testUnary() {
    final TestHelper helper = new TestHelper(MySQLParser::expr);
    {
      final SQLNode node = helper.sql("+b");
      assertEquals(UNARY, node.get(EXPR_KIND));
      assertEquals(UnaryOp.UNARY_PLUS, node.get(UNARY_OP));
      assertEquals("+`b`", node.toString());
    }
    {
      final SQLNode node = helper.sql("! b");
      assertEquals(UNARY, node.get(EXPR_KIND));
      assertEquals(UnaryOp.NOT, node.get(UNARY_OP));
      assertEquals("NOT `b`", node.toString());
    }
    {
      final SQLNode node = helper.sql("binary b");
      assertEquals(UNARY, node.get(EXPR_KIND));
      assertEquals(UnaryOp.BINARY, node.get(UNARY_OP));
      assertEquals("BINARY `b`", node.toString());
    }
    {
      final SQLNode node = helper.sql("not b");
      assertEquals(UNARY, node.get(EXPR_KIND));
      assertEquals(UnaryOp.NOT, node.get(UNARY_OP));
      assertEquals("NOT `b`", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] grouping op")
  void testGroupingOp() {
    PARSER.setServerVersion(80000);
    final TestHelper helper = new TestHelper(MySQLParser::groupingOperation);
    final SQLNode node = helper.sql("grouping(1,b)");
    assertEquals(GROUPING_OP, node.get(EXPR_KIND));
    assertEquals(2, node.get(GROUPING_OP_EXPRS).size());
    assertEquals("GROUPING(1, `b`)", node.toString());
  }

  @Test
  @DisplayName("[sqlparser.mysql] exists")
  void testExists() {
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);
    final SQLNode node = helper.sql("exists(select 1)");
    assertEquals(EXISTS, node.get(EXPR_KIND));
  }

  @Test
  @DisplayName("[sqlparser.mysql] match against")
  void testMatchAgainst() {
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);
    final SQLNode node = helper.sql("match a against ('123' with query expansion)");
    assertEquals("MATCH `a` AGAINST ('123' WITH QUERY EXPANSION)", node.toString());
  }

  @Test
  @DisplayName("[sqlparser.mysql] cast")
  void testCast() {
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);
    {
      final SQLNode node = helper.sql("convert(a, char)");
      assertEquals("CAST(`a` AS CHAR)", node.toString());
    }
    {
      PARSER.setServerVersion(80017);
      final SQLNode node = helper.sql("cast(a as char array)");
      assertEquals("CAST(`a` AS CHAR ARRAY)", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] default")
  void testDefault() {
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);
    {
      final SQLNode node = helper.sql("default(a.b)");
      assertEquals("DEFAULT(`a`.`b`)", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] values")
  void testValues() {
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);
    {
      final SQLNode node = helper.sql("values(a.b.c)");
      // this is parsed as function
      assertEquals("VALUES(`a`.`b`.`c`)", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] aggregate")
  void testAggregate() {
    final TestHelper helper = new TestHelper(MySQLParser::sumExpr);
    {
      final SQLNode node = helper.sql("count(distinct a)");
      assertTrue(node.isFlagged(AGGREGATE_DISTINCT));
      assertEquals("COUNT(DISTINCT `a`)", node.toString());
    }
    {
      final SQLNode node = helper.sql("count(*)");
      assertEquals(WILDCARD, node.get(AGGREGATE_ARGS).get(0).get(EXPR_KIND));
      assertEquals("COUNT(*)", node.toString());
    }
    {
      PARSER.setServerVersion(80000);
      final SQLNode node = helper.sql("avg(a) over w");
      assertEquals("AVG(`a`) OVER `w`", node.toString());
    }
    {
      PARSER.setServerVersion(80000);
      final SQLNode node = helper.sql("group_concat(a order by b separator ',') over ()");
      assertEquals("GROUP_CONCAT(`a` ORDER BY `b` SEPARATOR ',') OVER ()", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] convert using")
  void testConvertUsing() {
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);
    {
      final SQLNode node = helper.sql("convert(a using '123')");
      assertEquals("CONVERT(`a` USING '123')", node.toString());
    }
    {
      final SQLNode node = helper.sql("convert(a using binary)");
      assertEquals("CONVERT(`a` USING binary)", node.toString());
    }
    {
      final SQLNode node = helper.sql("convert(a using default)");
      assertEquals("CONVERT(`a` USING default)", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] case when")
  void testCaseWhen() {
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);
    {
      final SQLNode node = helper.sql("case when true then 1 else 2 end");
      assertEquals("CASE WHEN TRUE THEN 1 ELSE 2 END", node.toString());
    }
    {
      final SQLNode node = helper.sql("case a when 1 then 2 when 2 then 4 else 8 end");
      assertEquals("CASE `a` WHEN 1 THEN 2 WHEN 2 THEN 4 ELSE 8 END", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] frame")
  void testFrame() {
    PARSER.setServerVersion(80000);
    final TestHelper helper = new TestHelper(MySQLParser::windowFrameClause);
    {
      final SQLNode node = helper.sql("rows interval 1 year preceding exclude current row");
      assertEquals("ROWS INTERVAL 1 YEAR PRECEDING EXCLUDE CURRENT ROW", node.toString());
    }
    {
      final SQLNode node = helper.sql("range between unbounded preceding and 1 following");
      assertEquals("RANGE BETWEEN UNBOUNDED PRECEDING AND 1 FOLLOWING", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] window spec")
  void testWindowSpec() {
    PARSER.setServerVersion(80000);
    final TestHelper helper = new TestHelper(MySQLParser::windowSpecDetails);
    {
      final SQLNode node =
          helper.sql(
              "window_name partition by col_a asc "
                  + "order by col_b desc "
                  + "rows interval 1 year preceding exclude current row");
      assertEquals(
          "(`window_name` PARTITION BY `col_a` ASC ORDER BY `col_b` DESC "
              + "ROWS INTERVAL 1 YEAR PRECEDING EXCLUDE CURRENT ROW)",
          node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] binary")
  void testBinary() {
    final TestHelper helper = new TestHelper(MySQLParser::expr);
    {
      final SQLNode node = helper.sql("1+2");
      assertEquals("1 + 2", node.toString());
    }
    {
      final SQLNode node = helper.sql("1+2*3");
      assertEquals("1 + 2 * 3", node.toString());
    }
    {
      final SQLNode node = helper.sql("(1+2)*3");
      assertEquals("(1 + 2) * 3", node.toString());
    }
    {
      SQLNode node = helper.sql("a is not true");
      assertEquals("NOT `a` IS TRUE", node.toString());
      node = helper.sql("a is false");
      assertEquals("`a` IS FALSE", node.toString());
      node = helper.sql("a is unknown");
      assertEquals("`a` IS UNKNOWN", node.toString());
    }
    {
      SQLNode node = helper.sql("a and b");
      assertEquals("`a` AND `b`", node.toString());
      node = helper.sql("a or b");
      assertEquals("`a` OR `b`", node.toString());
      node = helper.sql("a xor b");
      assertEquals("`a` XOR `b`", node.toString());
    }
    {
      SQLNode node = helper.sql("a like '%123%'");
      assertEquals("`a` LIKE '%123%'", node.toString());
      node = helper.sql("a is null");
      assertEquals("`a` IS NULL", node.toString());
      node = helper.sql("a is not null");
      assertEquals("NOT `a` IS NULL", node.toString());
      node = helper.sql("a in (1,2)");
      assertEquals("`a` IN (1, 2)", node.toString());
    }
    {
      SQLNode node = helper.sql("a regexp 'a*'");
      assertEquals("`a` REGEXP 'a*'", node.toString());
      node = helper.sql("a not regexp 'a*'");
      assertEquals("NOT `a` REGEXP 'a*'", node.toString());
    }
    {
      PARSER.setServerVersion(80017);
      SQLNode node = helper.sql("a member of ((1,2+a,b))");
      assertEquals("`a` MEMBER OF ((1, 2 + `a`, `b`))", node.toString());
    }
    {
      SQLNode node = helper.sql("a sounds like b");
      assertEquals("`a` SOUNDS LIKE `b`", node.toString());
    }
    {
      SQLNode node = helper.sql("a + interval 10 year");
      assertEquals("`a` + INTERVAL 10 YEAR", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] ternary")
  void testTernary() {
    final TestHelper helper = new TestHelper(MySQLParser::expr);
    {
      final SQLNode node = helper.sql("a = (b between 1 and 2)");
      assertEquals("`a` = (`b` BETWEEN 1 AND 2)", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] interval")
  void testInterval() {
    final TestHelper helper = new TestHelper(MySQLParser::expr);
    {
      final SQLNode node = helper.sql("interval (1+a) day + b");
      assertEquals("INTERVAL (1 + `a`) DAY + `b`", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] select item")
  void testSelectItem() {
    final TestHelper helper = new TestHelper(MySQLParser::selectItem);
    {
      final SQLNode node = helper.sql("a.*");
      assertEquals("`a`.*", node.toString());
    }
    {
      final SQLNode node = helper.sql("a.b aaa");
      assertEquals("`a`.`b` AS `aaa`", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] index hint")
  void testIndexHint() {
    final TestHelper helper = new TestHelper(MySQLParser::indexHint);
    {
      final SQLNode node = helper.sql("ignore key for join (a, primary)");
      assertEquals("IGNORE INDEX FOR JOIN (`a`, PRIMARY)", node.toString());
    }
    {
      final SQLNode node = helper.sql("force key for order by (primary)");
      assertEquals("FORCE INDEX FOR ORDER BY (PRIMARY)", node.toString());
    }
    {
      final SQLNode node = helper.sql("use key for group by ()");
      assertEquals("USE INDEX FOR GROUP BY ()", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] simple table source")
  void testSimpleTableSource() {
    final TestHelper helper = new TestHelper(MySQLParser::singleTable);
    {
      PARSER.setServerVersion(50602);
      final SQLNode node =
          helper.sql("t partition (p,q) tt use key for group by (), use key for order by ()");
      assertEquals(
          "`t` PARTITION (`p`, `q`) AS `tt` USE INDEX FOR GROUP BY (), USE INDEX FOR ORDER BY ()",
          node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] joined table source")
  void testJoinedTableSource() {
    final TestHelper helper = new TestHelper(MySQLParser::tableReference);
    {
      final SQLNode node = helper.sql("a join (b join c)");
      assertEquals("`a` INNER JOIN `b` INNER JOIN `c`", node.toString());
    }
    {
      final SQLNode node = helper.sql("a natural left join (b join c)");
      assertEquals("`a` NATURAL LEFT JOIN (`b` INNER JOIN `c`)", node.toString());
      assertEquals(
          "`a`\n" + "NATURAL LEFT JOIN (\n" + "  `b`\n  INNER JOIN `c`\n" + ")",
          node.toString(false));
    }
    {
      final SQLNode node = helper.sql("a left join b on a.col = b.col inner join c using (col)");
      assertEquals(
          "`a` LEFT JOIN `b` ON `a`.`col` = `b`.`col` INNER JOIN `c` USING (`col`)",
          node.toString());
      assertEquals(
          "`a`\n"
              + "LEFT JOIN `b`\n"
              + "  ON `a`.`col` = `b`.`col`\n"
              + "INNER JOIN `c`\n"
              + "  USING (`col`)",
          node.toString(false));
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] select statement")
  void testSelectStatement() {
    final TestHelper helper = new TestHelper(MySQLParser::selectStatement);
    {
      final SQLNode node =
          helper.sql(
              ""
                  + "select distinct "
                  + "  a, b.*, count(1), "
                  + "  case when c = 0 then 1 else 2 end "
                  + "from t0 tt "
                  + "  left join t1 on tt.a = t1.b "
                  + "  inner join (select e from t2) as t3 on t3.e = tt.a "
                  + "where tt.f in (select 1 from t4) "
                  + "  and exists ("
                  + "    select 1 from t5"
                  + "    union all"
                  + "    select 2 from t6"
                  + "  ) "
                  + "group by tt.g, tt.h "
                  + "having sum(tt.i) < 10 "
                  + "order by t1.x, t1.y "
                  + "limit ?,?");

      assertEquals(
          ""
              + "SELECT DISTINCT "
              + "`a`, "
              + "`b`.*, "
              + "COUNT(1), "
              + "CASE "
              + "WHEN `c` = 0 THEN 1 "
              + "ELSE 2 "
              + "END "
              + "FROM `t0` AS `tt` "
              + "LEFT JOIN `t1` "
              + "ON `tt`.`a` = `t1`.`b` "
              + "INNER JOIN ("
              + "SELECT "
              + "`e` "
              + "FROM `t2`"
              + ") AS `t3` "
              + "ON `t3`.`e` = `tt`.`a` "
              + "WHERE "
              + "`tt`.`f` IN ("
              + "SELECT "
              + "1 "
              + "FROM `t4`"
              + ") "
              + "AND EXISTS ("
              + "(SELECT "
              + "1 "
              + "FROM `t5`) "
              + "UNION ALL "
              + "(SELECT "
              + "2 "
              + "FROM `t6`)"
              + ") "
              + "GROUP BY "
              + "`tt`.`g`, "
              + "`tt`.`h` "
              + "HAVING "
              + "SUM(`tt`.`i`) < 10 "
              + "ORDER BY "
              + "`t1`.`x`, "
              + "`t1`.`y` "
              + "LIMIT ?, ?",
          node.toString());

      assertEquals(
          ""
              + "SELECT DISTINCT\n"
              + "  `a`,\n"
              + "  `b`.*,\n"
              + "  COUNT(1),\n"
              + "  CASE\n"
              + "    WHEN `c` = 0 THEN 1\n"
              + "    ELSE 2\n"
              + "  END\n"
              + "FROM `t0` AS `tt`\n"
              + "  LEFT JOIN `t1`\n"
              + "    ON `tt`.`a` = `t1`.`b`\n"
              + "  INNER JOIN (\n"
              + "    SELECT\n"
              + "      `e`\n"
              + "    FROM `t2`\n"
              + "  ) AS `t3`\n"
              + "    ON `t3`.`e` = `tt`.`a`\n"
              + "WHERE\n"
              + "  `tt`.`f` IN (\n"
              + "    SELECT\n"
              + "      1\n"
              + "    FROM `t4`\n"
              + "  )\n"
              + "  AND EXISTS (\n"
              + "    (SELECT\n"
              + "      1\n"
              + "    FROM `t5`)\n"
              + "    UNION ALL\n"
              + "    (SELECT\n"
              + "      2\n"
              + "    FROM `t6`)\n"
              + "  )\n"
              + "GROUP BY\n"
              + "  `tt`.`g`,\n"
              + "  `tt`.`h`\n"
              + "HAVING\n"
              + "  SUM(`tt`.`i`) < 10\n"
              + "ORDER BY\n"
              + "  `t1`.`x`,\n"
              + "  `t1`.`y`\n"
              + "LIMIT ?, ?",
          node.toString(false));
    }
  }
}
