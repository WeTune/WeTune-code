package sjtu.ipads.wtune.sqlparser.rel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.common.multiversion.Snapshot;
import sjtu.ipads.wtune.sqlparser.SQLContext;
import sjtu.ipads.wtune.sqlparser.SQLParser;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.LiteralType;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.SQLNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.DERIVED_SUBQUERY;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.SIMPLE_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.LITERAL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.TABLE_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.SIMPLE_SOURCE;
import static sjtu.ipads.wtune.sqlparser.rel.Relation.RELATION;

public class RelationTest {
  @Test
  @DisplayName("[sqlparser.rel] resolution")
  void testResolution() {
    final String schemaDef =
        ""
            + "CREATE TABLE a (x int, y int, z int);"
            + "CREATE TABLE b (i int, j int, k int);"
            + "CREATE TABLE c (p int, q int, r int)";

    final String sql =
        ""
            + "SELECT * "
            + "FROM (SELECT * FROM a) a"
            + " JOIN b ON a.x = b.i "
            + "WHERE EXISTS (SELECT *, a.*, `c`.`p` FROM c)";

    final Schema schema = Schema.parse(MYSQL, schemaDef);
    final SQLNode node = SQLParser.ofDb(MYSQL).parse(sql);
    node.context().setSchema(schema);

    final Relation rel0 = node.get(RELATION);

    final List<Relation> inputs = rel0.inputs();
    assertEquals(2, inputs.size());
    assertEquals(6, rel0.attributes().size());
    assertNull(rel0.auxiliaryInput());

    assertSame(schema.table("a").column("x"), rel0.attributes().get(0).column(true));
    assertSame(schema.table("b").column("j"), rel0.attributes().get(4).column(true));

    final Relation rel1 = inputs.get(0);
    final Relation rel2 = inputs.get(1);

    assertSame(rel0, rel1.parent());
    assertSame(rel0, rel1.parent());

    assertEquals(1, rel1.inputs().size());
    assertEquals(0, rel2.inputs().size());

    assertNull(rel1.auxiliaryInput());
    assertNull(rel2.auxiliaryInput());

    assertEquals(3, rel1.attributes().size());
    assertEquals(3, rel2.attributes().size());

    final SQLNode existsSubquery =
        node.get(QUERY_BODY).get(QUERY_SPEC_WHERE).get(EXISTS_SUBQUERY_EXPR).get(QUERY_EXPR_QUERY);

    final Relation rel3 = existsSubquery.get(RELATION);
    assertSame(rel0, rel3.parent());
    assertEquals(1, rel3.inputs().size());
    assertEquals(7, rel3.attributes().size());
    assertSame(rel0, rel3.auxiliaryInput());
    assertSame(schema.table("a").column("x"), rel3.attributes().get(3).column(true));
    assertSame(rel1.attributes().get(0), rel3.attributes().get(3).reference());
  }

  @Test
  @DisplayName("[sqlparser.rel] modification")
  void testModification() {
    final String schemaDef =
        ""
            + "CREATE TABLE a (x int, y int, z int);"
            + "CREATE TABLE b (i int, j int);"
            + "CREATE TABLE c (p int, q int, r int, s int)";

    final String sql =
        ""
            + "SELECT * "
            + "FROM (SELECT * FROM a) a"
            + " JOIN b ON a.x = b.i "
            + "WHERE EXISTS (SELECT *, a.*, `c`.`p` FROM c)";

    final Schema schema = Schema.parse(MYSQL, schemaDef);
    final SQLNode node = SQLParser.ofDb(MYSQL).parse(sql);
    final SQLContext context = node.context();

    context.setSchema(schema);

    final Relation relation = node.get(RELATION);
    assertEquals(5, relation.attributes().size());
    final Snapshot snapshot0 = context.snapshot();

    context.derive();
    final SQLNode existsSubquery =
        node.get(QUERY_BODY).get(QUERY_SPEC_WHERE).get(EXISTS_SUBQUERY_EXPR).get(QUERY_EXPR_QUERY);

    final SQLNode literal = SQLNode.expr(LITERAL);
    literal.set(LITERAL_TYPE, LiteralType.BOOL);
    literal.set(LITERAL_VALUE, true);
    existsSubquery.update(literal);
    assertEquals(5, relation.attributes().size());

    final SQLNode tblB = relation.inputs().get(1).node();
    final SQLNode tblC = SQLNode.tableSource(SIMPLE_SOURCE);
    final SQLNode tblCName = SQLNode.node(TABLE_NAME);
    tblCName.set(TABLE_NAME_TABLE, "c");
    tblC.set(SIMPLE_TABLE, tblCName);
    tblB.update(tblC);
    assertEquals(7, relation.attributes().size());

    final SQLNode tblA = relation.inputs().get(0).node();
    final SQLNode subquery = tblA.get(DERIVED_SUBQUERY).get(QUERY_BODY);
    subquery.set(QUERY_SPEC_FROM, tblC.copy());
    assertEquals(8, relation.attributes().size());

    context.setSnapshot(snapshot0);

    assertEquals(6, relation.attributes().size());
  }
}
