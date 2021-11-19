package sjtu.ipads.wtune.sqlparser.mysql;

import org.antlr.v4.runtime.ParserRuleContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.SqlSupport;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNode;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNodes;
import sjtu.ipads.wtune.sqlparser.ast1.constants.VariableScope;
import sjtu.ipads.wtune.sqlparser.mysql.internal.MySQLParser;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.sqlparser.ast1.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast1.ExprKind.*;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlKind.CreateTable;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlNodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast1.constants.ConstraintKind.*;
import static sjtu.ipads.wtune.sqlparser.ast1.constants.IndexKind.RTREE;
import static sjtu.ipads.wtune.sqlparser.ast1.constants.KeyDirection.DESC;
import static sjtu.ipads.wtune.sqlparser.ast1.constants.LiteralKind.*;
import static sjtu.ipads.wtune.sqlparser.ast1.constants.UnaryOpKind.*;
import static sjtu.ipads.wtune.sqlparser.mysql.MySQLRecognizerCommon.PipesAsConcat;

public class MySQLAstBuilderTest {
  private static final MySQLAstParser1 PARSER = new MySQLAstParser1();

  private static class TestHelper {
    private String sql;
    private SqlNode node;
    private final Function<MySQLParser, ParserRuleContext> rule;

    private TestHelper(Function<MySQLParser, ParserRuleContext> rule) {
      this.rule = rule;
    }

    private SqlNode sql(String sql) {
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

      final SqlNode root = helper.sql(createTable);
      assertEquals(CreateTable, root.kind());

      final var tableName = root.$(CreateTable_Name);
      assertEquals("public", tableName.$(TableName_Schema));
      assertEquals("t", tableName.$(TableName_Table));

      assertEquals("innodb", root.$(CreateTable_Engine));

      final var columns = root.$(CreateTable_Cols);
      final var constraints = root.$(CreateTable_Cons);

      assertEquals(3, columns.size());
      assertEquals(5, constraints.size());

      {
        final var col0 = columns.get(0);

        {
          final var col0Name = col0.$(ColDef_Name);
          assertNull(col0Name.$(ColName_Schema));
          assertNull(col0Name.$(ColName_Table));
          assertEquals("i", col0Name.$(ColName_Col));
        }

        {
          assertEquals("int(10)", col0.$(ColDef_RawType));
          assertFalse(col0.isFlag(ColDef_AutoInc));
          assertFalse(col0.isFlag(ColDef_Default));
          assertFalse(col0.isFlag(ColDef_Generated));
        }

        {
          final var col0Cons = col0.$(ColDef_Cons);
          assertTrue(col0Cons.contains(PRIMARY));
          assertFalse(col0Cons.contains(UNIQUE));
          assertFalse(col0Cons.contains(CHECK));
          assertFalse(col0Cons.contains(NOT_NULL));
          assertFalse(col0Cons.contains(FOREIGN));

          final var col0Refs = col0.$(ColDef_Ref);
          final var col0RefTable = col0Refs.$(Reference_Table);
          final var col0RefCols = col0Refs.$(Reference_Cols);
          assertNull(col0RefTable.$(TableName_Schema));
          assertEquals("b", col0RefTable.$(TableName_Table));
          assertEquals(1, col0RefCols.size());
          final var col0RefCol0 = col0RefCols.get(0);
          assertNull(col0RefCol0.$(ColName_Schema));
          assertNull(col0RefCol0.$(ColName_Table));
          assertEquals("x", col0RefCol0.$(ColName_Col));
        }
      }

      {
        {
          final var cons0 = constraints.get(0);
          assertNull(cons0.$(IndexDef_Name));
          assertNull(cons0.$(IndexDef_Cons));
          assertNull(cons0.$(IndexDef_Kind));
          assertNull(cons0.$(IndexDef_Refs));
          final var keys = cons0.$(IndexDef_Keys);
          assertEquals(1, keys.size());

          final var key0 = keys.get(0);
          assertNull(key0.$(KeyPart_Direction));
          assertEquals("j", key0.$(KeyPart_Col));
          assertEquals(100, key0.$(KeyPart_Len));
        }

        {
          final var cons1 = constraints.get(1);
          assertNull(cons1.$(IndexDef_Name));
          assertNull(cons1.$(IndexDef_Refs));
          assertEquals(UNIQUE, cons1.$(IndexDef_Cons));
          assertEquals(RTREE, cons1.$(IndexDef_Kind));
          final var keys = cons1.$(IndexDef_Keys);
          assertEquals(1, keys.size());

          final var key0 = keys.get(0);
          assertEquals(DESC, key0.$(KeyPart_Direction));
          assertEquals("j", key0.$(KeyPart_Col));
          assertNull(key0.$(KeyPart_Len));
        }

        {
          final var cons2 = constraints.get(2);
          assertEquals("fk", cons2.$(IndexDef_Name));
          assertEquals(FOREIGN, cons2.$(IndexDef_Cons));
          assertNull(cons2.$(IndexDef_Kind));
          final var keys = cons2.$(IndexDef_Keys);
          assertEquals(1, keys.size());

          final var key0 = keys.get(0);
          assertNull(key0.$(KeyPart_Direction));
          assertNull(key0.$(KeyPart_Len));
          assertEquals("k", key0.$(KeyPart_Col));

          final var refs = cons2.$(IndexDef_Refs);
          final var refTable = refs.$(Reference_Table);
          final var refCols = refs.$(Reference_Cols);
          assertEquals("b", refTable.$(TableName_Table));
          assertEquals(1, refCols.size());
          assertEquals("y", refCols.get(0).$(ColName_Col));
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
      final SqlNode node = helper.sql(createTable);
      assertEquals("CREATE TABLE `t` ( `i` int, PRIMARY KEY ((`i` + 1)) )", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] variable")
  void testVariable() {
    String sql;
    SqlNode node;

    {
      sql = "@`a`=1";
      node = PARSER.parse(sql, MySQLParser::variable);
      assertEquals(VariableScope.USER, node.$(Variable_Scope));
      assertEquals("a", node.$(Variable_Name));
      // TODO: assertEquals("@a=1", node.toString())
      // TODO: assertNotNull(node.$(VARIABLE_ASSIGNMENT));
    }

    {
      sql = "@'a'=1";
      node = PARSER.parse(sql, MySQLParser::variable);
      assertEquals(VariableScope.USER, node.$(Variable_Scope));
      assertEquals("a", node.$(Variable_Name));
    }

    {
      sql = "@@system.var";
      node = PARSER.parse(sql, MySQLParser::variable);
      assertEquals(VariableScope.SYSTEM_GLOBAL, node.$(Variable_Scope));
      assertEquals("@@GLOBAL.system.var", node.toString());
    }

    {
      sql = "@@ session.system.var";
      node = PARSER.parse(sql, MySQLParser::variable);
      assertEquals(VariableScope.SYSTEM_SESSION, node.$(Variable_Scope));
      assertEquals("system.var", node.$(Variable_Name));
      assertEquals("@@SESSION.system.var", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] column ref")
  void testColumnRef() {
    final TestHelper helper = new TestHelper(MySQLParser::columnRef);

    {
      final SqlNode node = helper.sql("a.b.c");
      assertEquals("`a`.`b`.`c`", node.$(ColRef_ColName).toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] json ref")
  void testJsonRef() {
    PARSER.setServerVersion(80000);
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);

    {
      final SqlNode node = helper.sql("a->'$.b'");
      assertEquals(FuncCall, node.$(Expr_Kind));
      final SqlNodes args = node.$(FuncCall_Args);
      assertEquals(2, args.size());

      assertEquals("json_extract", node.$(FuncCall_Name).$(Name2_1));
      assertEquals("`a`", args.get(0).toString());
      assertEquals("'$.b'", args.get(1).toString());
      assertEquals("JSON_EXTRACT(`a`, '$.b')", node.toString());
    }

    {
      final SqlNode node = helper.sql("a->>'$.b'");
      assertEquals(FuncCall, node.$(Expr_Kind));
      final SqlNodes args = node.$(FuncCall_Args);

      assertEquals(1, args.size());
      assertEquals("json_unquote", node.$(FuncCall_Name).$(Name2_1));
      assertEquals("JSON_UNQUOTE(JSON_EXTRACT(`a`, '$.b'))", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] literal")
  void testLiteral() {
    final TestHelper helper = new TestHelper(MySQLParser::literal);

    {
      final SqlNode node = helper.sql("'abc' 'def'");
      assertEquals(TEXT, node.$(Literal_Kind));
      assertEquals("abcdef", node.$(Literal_Value));
      assertEquals("'abcdef'", node.toString());
    }
    {
      final SqlNode node = helper.sql("123");
      assertEquals(INTEGER, node.$(Literal_Kind));
      assertEquals(123, node.$(Literal_Value));
      assertEquals("123", node.toString());
    }
    {
      final SqlNode node = helper.sql("123.123");
      assertEquals(FRACTIONAL, node.$(Literal_Kind));
      assertEquals(123.123, node.$(Literal_Value));
      assertEquals("123.123", node.toString());
    }
    {
      final SqlNode node = helper.sql("null");
      assertEquals(NULL, node.$(Literal_Kind));
      assertNull(node.$(Literal_Value));
      assertEquals("NULL", node.toString());
    }
    {
      final SqlNode node = helper.sql("true");
      assertEquals(BOOL, node.$(Literal_Kind));
      assertEquals(true, node.$(Literal_Value));
      assertEquals("TRUE", node.toString());
    }
    {
      final SqlNode node = helper.sql("timestamp '2020-01-01 00:00:00.000'");
      assertEquals(TEMPORAL, node.$(Literal_Kind));
      assertEquals("2020-01-01 00:00:00.000", node.$(Literal_Value));
      assertEquals("timestamp", node.$(Literal_Unit));
      assertEquals("TIMESTAMP '2020-01-01 00:00:00.000'", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] func call")
  void testFuncCall() {
    final TestHelper helper = new TestHelper(MySQLParser::expr);
    {
      final SqlNode node = helper.sql("now()");
      assertEquals("NOW()", node.toString());
    }
    {
      final SqlNode node = helper.sql("current_user()");
      assertEquals("CURRENT_USER()", node.toString());
    }
    {
      PARSER.setServerVersion(50604);
      final SqlNode node = helper.sql("curtime(0)");
      assertEquals("CURTIME(0)", node.toString());
    }
    {
      final SqlNode node = helper.sql("COALESCE(1,a,b+1)");
      assertEquals("COALESCE(1, `a`, `b` + 1)", node.toString());
    }
    {
      final SqlNode node = helper.sql("concat(1,a,b+1)");
      assertEquals("CONCAT(1, `a`, `b` + 1)", node.toString());
    }
    {
      final SqlNode node = helper.sql("char(1,a,b+1)");
      assertEquals("CHAR(1, `a`, `b` + 1)", node.toString());
    }
    {
      final SqlNode node = helper.sql("adddate(a,interval 10 day)");
      assertEquals("ADDDATE(`a`, INTERVAL 10 DAY)", node.toString());
    }
    {
      final SqlNode node = helper.sql("extract(day from a)");
      assertEquals("EXTRACT(DAY FROM `a`)", node.toString());
    }
    {
      final SqlNode node = helper.sql("get_format(date, 'USA')");
      assertEquals("GET_FORMAT(DATE, 'USA')", node.toString());
    }
    {
      final SqlNode node = helper.sql("position('a' in a)");
      assertEquals("POSITION('a' IN `a`)", node.toString());
    }
    {
      final SqlNode node = helper.sql("timestamp_diff(second,a,b)");
      assertEquals("TIMESTAMP_DIFF(SECOND, `a`, `b`)", node.toString());
    }
    {
      final SqlNode node = helper.sql("old_password('abc')");
      assertEquals("OLD_PASSWORD('abc')", node.toString());
    }
    {
      final SqlNode node = helper.sql("right(1,a)");
      assertEquals("RIGHT(1, `a`)", node.toString());
    }
    {
      SqlNode node = helper.sql("trim(leading 'abc' from a)");
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
      SqlNode node = helper.sql("substring(a,'a',1)");
      assertEquals("SUBSTRING(`a`, 'a', 1)", node.toString());
    }
    {
      SqlNode node = helper.sql("geometrycollection(a,b)");
      assertEquals("GEOMETRYCOLLECTION(`a`, `b`)", node.toString());
      node = helper.sql("geometrycollection()");
      assertEquals("GEOMETRYCOLLECTION()", node.toString());
      node = helper.sql("linestring(a,b)");
      assertEquals("LINESTRING(`a`, `b`)", node.toString());
      node = helper.sql("point(a,b)");
      assertEquals("POINT(`a`, `b`)", node.toString());
    }
    {
      SqlNode node = helper.sql("my.myfunc(a,b)");
      assertEquals("MYFUNC(`a`, `b`)", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] collation")
  void testCollation() {
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);
    {
      final SqlNode node = helper.sql("a collate 'utf8'");
      assertEquals(Collate, node.$(Expr_Kind));
      assertEquals("`a`", node.$(Collate_Expr).toString());
      assertEquals("utf8", node.$(Collate_Collation).$(Symbol_Text));
      assertEquals("`a` COLLATE 'utf8'", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] param marker")
  void testParamMarker() {
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);
    final SqlNode node = helper.sql("?");
    assertEquals(Param, node.$(Expr_Kind));
  }

  @Test
  @DisplayName("[sqlparser.mysql] concat ")
  void testConcatPipe() {
    PARSER.setSqlMode(PipesAsConcat);
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);
    final SqlNode node = helper.sql("'a' || b");
    assertEquals(FuncCall, node.$(Expr_Kind));
    assertEquals("concat", node.$(FuncCall_Name).$(Name2_1));
    assertEquals(2, node.$(FuncCall_Args).size());
    assertEquals("CONCAT('a', `b`)", node.toString());
  }

  @Test
  @DisplayName("[sqlparser.mysql] unary")
  void testUnary() {
    final TestHelper helper = new TestHelper(MySQLParser::expr);
    {
      final SqlNode node = helper.sql("+b");
      assertEquals(Unary, node.$(Expr_Kind));
      assertEquals(UNARY_PLUS, node.$(Unary_Op));
      assertEquals("+`b`", node.toString());
    }
    {
      final SqlNode node = helper.sql("! b");
      assertEquals(Unary, node.$(Expr_Kind));
      assertEquals(NOT, node.$(Unary_Op));
      assertEquals("NOT `b`", node.toString());
    }
    {
      final SqlNode node = helper.sql("binary b");
      assertEquals(Unary, node.$(Expr_Kind));
      assertEquals(BINARY, node.$(Unary_Op));
      assertEquals("BINARY `b`", node.toString());
    }
    {
      final SqlNode node = helper.sql("not b");
      assertEquals(Unary, node.$(Expr_Kind));
      assertEquals(NOT, node.$(Unary_Op));
      assertEquals("NOT `b`", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] grouping op")
  void testGroupingOp() {
    PARSER.setServerVersion(80000);
    final TestHelper helper = new TestHelper(MySQLParser::groupingOperation);
    final SqlNode node = helper.sql("grouping(1,b)");
    assertEquals(GroupingOp, node.$(Expr_Kind));
    assertEquals(2, node.$(GroupingOp_Exprs).size());
    assertEquals("GROUPING(1, `b`)", node.toString());
  }

  @Test
  @DisplayName("[sqlparser.mysql] exists")
  void testExists() {
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);
    final SqlNode node = helper.sql("exists(select 1)");
    assertEquals(Exists, node.$(Expr_Kind));
  }

  @Test
  @DisplayName("[sqlparser.mysql] match against")
  void testMatchAgainst() {
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);
    final SqlNode node = helper.sql("match a against ('123' with query expansion)");
    assertEquals("MATCH `a` AGAINST ('123' WITH QUERY EXPANSION)", node.toString());
  }

  @Test
  @DisplayName("[sqlparser.mysql] cast")
  void testCast() {
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);
    {
      final SqlNode node = helper.sql("convert(a, char)");
      assertEquals("CAST(`a` AS CHAR)", node.toString());
    }
    {
      PARSER.setServerVersion(80017);
      final SqlNode node = helper.sql("cast(a as char array)");
      assertEquals("CAST(`a` AS CHAR ARRAY)", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] default")
  void testDefault() {
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);
    {
      final SqlNode node = helper.sql("default(a.b)");
      assertEquals("DEFAULT(`a`.`b`)", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] values")
  void testValues() {
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);
    {
      final SqlNode node = helper.sql("values(a.b.c)");
      // this is parsed as function
      assertEquals("VALUES(`a`.`b`.`c`)", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] aggregate")
  void testAggregate() {
    final TestHelper helper = new TestHelper(MySQLParser::sumExpr);
    {
      final SqlNode node = helper.sql("count(distinct a)");
      assertTrue(node.isFlag(Aggregate_Distinct));
      assertEquals("COUNT(DISTINCT `a`)", node.toString());
    }
    {
      final SqlNode node = helper.sql("count(*)");
      assertEquals(Wildcard, node.$(Aggregate_Args).get(0).$(Expr_Kind));
      assertEquals("COUNT(*)", node.toString());
    }
    {
      PARSER.setServerVersion(80000);
      final SqlNode node = helper.sql("avg(a) over w");
      assertEquals("AVG(`a`) OVER `w`", node.toString());
    }
    {
      PARSER.setServerVersion(80000);
      final SqlNode node = helper.sql("group_concat(a order by b separator ',') over ()");
      System.out.println(SqlSupport.dumpAst(node));
      assertEquals("GROUP_CONCAT(`a` ORDER BY `b` SEPARATOR ',') OVER ()", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] convert using")
  void testConvertUsing() {
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);
    {
      final SqlNode node = helper.sql("convert(a using '123')");
      assertEquals("CONVERT(`a` USING '123')", node.toString());
    }
    {
      final SqlNode node = helper.sql("convert(a using binary)");
      assertEquals("CONVERT(`a` USING binary)", node.toString());
    }
    {
      final SqlNode node = helper.sql("convert(a using default)");
      assertEquals("CONVERT(`a` USING default)", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] case when")
  void testCaseWhen() {
    final TestHelper helper = new TestHelper(MySQLParser::simpleExpr);
    {
      final SqlNode node = helper.sql("case when true then 1 else 2 end");
      assertEquals("CASE WHEN TRUE THEN 1 ELSE 2 END", node.toString());
    }
    {
      final SqlNode node = helper.sql("case a when 1 then 2 when 2 then 4 else 8 end");
      assertEquals("CASE `a` WHEN 1 THEN 2 WHEN 2 THEN 4 ELSE 8 END", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] frame")
  void testFrame() {
    PARSER.setServerVersion(80000);
    final TestHelper helper = new TestHelper(MySQLParser::windowFrameClause);
    {
      final SqlNode node = helper.sql("rows interval 1 year preceding exclude current row");
      assertEquals("ROWS INTERVAL 1 YEAR PRECEDING EXCLUDE CURRENT ROW", node.toString());
    }
    {
      final SqlNode node = helper.sql("range between unbounded preceding and 1 following");
      assertEquals("RANGE BETWEEN UNBOUNDED PRECEDING AND 1 FOLLOWING", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] window spec")
  void testWindowSpec() {
    PARSER.setServerVersion(80000);
    final TestHelper helper = new TestHelper(MySQLParser::windowSpecDetails);
    {
      final SqlNode node =
          helper.sql(
              "window_name partition by col_a "
                  + "order by col_b desc "
                  + "rows interval 1 year preceding exclude current row");
      assertEquals(
          "(`window_name` PARTITION BY `col_a` ORDER BY `col_b` DESC "
              + "ROWS INTERVAL 1 YEAR PRECEDING EXCLUDE CURRENT ROW)",
          node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] binary")
  void testBinary() {
    final TestHelper helper = new TestHelper(MySQLParser::expr);
    {
      final SqlNode node = helper.sql("1+2");
      assertEquals("1 + 2", node.toString());
    }
    {
      final SqlNode node = helper.sql("1+2*3");
      assertEquals("1 + 2 * 3", node.toString());
    }
    {
      final SqlNode node = helper.sql("(1+2)*3");
      assertEquals("(1 + 2) * 3", node.toString());
    }
    {
      SqlNode node = helper.sql("a is not true");
      assertEquals("NOT `a` IS TRUE", node.toString());
      node = helper.sql("a is false");
      assertEquals("`a` IS FALSE", node.toString());
      node = helper.sql("a is unknown");
      assertEquals("`a` IS UNKNOWN", node.toString());
    }
    {
      SqlNode node = helper.sql("a and b");
      assertEquals("`a` AND `b`", node.toString());
      node = helper.sql("a or b");
      assertEquals("`a` OR `b`", node.toString());
      node = helper.sql("a xor b");
      assertEquals("`a` XOR `b`", node.toString());
    }
    {
      SqlNode node = helper.sql("a like '%123%'");
      assertEquals("`a` LIKE '%123%'", node.toString());
      node = helper.sql("a is null");
      assertEquals("`a` IS NULL", node.toString());
      node = helper.sql("a is not null");
      assertEquals("NOT `a` IS NULL", node.toString());
      node = helper.sql("a in (1,2)");
      assertEquals("`a` IN (1, 2)", node.toString());
    }
    {
      SqlNode node = helper.sql("a regexp 'a*'");
      assertEquals("`a` REGEXP 'a*'", node.toString());
      node = helper.sql("a not regexp 'a*'");
      assertEquals("NOT `a` REGEXP 'a*'", node.toString());
    }
    {
      PARSER.setServerVersion(80017);
      SqlNode node = helper.sql("a member of ((1,2+a,b))");
      assertEquals("`a` MEMBER OF ((1, 2 + `a`, `b`))", node.toString());
    }
    {
      SqlNode node = helper.sql("a sounds like b");
      assertEquals("`a` SOUNDS LIKE `b`", node.toString());
    }
    {
      SqlNode node = helper.sql("a + interval 10 year");
      assertEquals("`a` + INTERVAL 10 YEAR", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] ternary")
  void testTernary() {
    final TestHelper helper = new TestHelper(MySQLParser::expr);
    {
      final SqlNode node = helper.sql("a = (b between 1 and 2)");
      assertEquals("`a` = (`b` BETWEEN 1 AND 2)", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] interval")
  void testInterval() {
    final TestHelper helper = new TestHelper(MySQLParser::expr);
    {
      final SqlNode node = helper.sql("interval (1+a) day + b");
      assertEquals("INTERVAL (1 + `a`) DAY + `b`", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] select item")
  void testSelectItem() {
    final TestHelper helper = new TestHelper(MySQLParser::selectItem);
    {
      final SqlNode node = helper.sql("a.*");
      assertEquals("`a`.*", node.toString());
    }
    {
      final SqlNode node = helper.sql("a.b aaa");
      assertEquals("`a`.`b` AS `aaa`", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] index hint")
  void testIndexHint() {
    final TestHelper helper = new TestHelper(MySQLParser::indexHint);
    {
      final SqlNode node = helper.sql("ignore key for join (a, primary)");
      assertEquals("IGNORE INDEX FOR JOIN (`a`, PRIMARY)", node.toString());
    }
    {
      final SqlNode node = helper.sql("force key for order by (primary)");
      assertEquals("FORCE INDEX FOR ORDER BY (PRIMARY)", node.toString());
    }
    {
      final SqlNode node = helper.sql("use key for group by ()");
      assertEquals("USE INDEX FOR GROUP BY ()", node.toString());
    }
  }

  @Test
  @DisplayName("[sqlparser.mysql] simple table source")
  void testSimpleTableSource() {
    final TestHelper helper = new TestHelper(MySQLParser::singleTable);
    {
      PARSER.setServerVersion(50602);
      final SqlNode node =
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
      final SqlNode node = helper.sql("a join (b join c)");
      assertEquals("`a` INNER JOIN (`b` INNER JOIN `c`)", node.toString());
    }
    {
      final SqlNode node = helper.sql("a natural left join (b join c)");
      assertEquals("`a` NATURAL LEFT JOIN (`b` INNER JOIN `c`)", node.toString());
      assertEquals(
          "`a`\n" + "NATURAL LEFT JOIN (\n" + "  `b`\n" + "  INNER JOIN `c`\n)",
          node.toString(false));
    }
    {
      final SqlNode node = helper.sql("a left join b on a.col = b.col inner join c using (col)");
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
      final SqlNode node =
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
              + "LIMIT ? OFFSET ?",
          node.toString());

      assertEquals(
          "SELECT DISTINCT\n"
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
              + "LIMIT ? OFFSET ?",
          node.toString(false));
    }
  }
}
