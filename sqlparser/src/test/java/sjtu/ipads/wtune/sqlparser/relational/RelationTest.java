package sjtu.ipads.wtune.sqlparser.relational;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.common.multiversion.Snapshot;
import sjtu.ipads.wtune.sqlparser.ASTContext;
import sjtu.ipads.wtune.sqlparser.ASTParser;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.LiteralType;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.MYSQL;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.DERIVED_SUBQUERY;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.SIMPLE_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.LITERAL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.TABLE_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.SIMPLE_SOURCE;
import static sjtu.ipads.wtune.sqlparser.relational.Relation.RELATION;

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
    final ASTNode node = ASTParser.ofDb(MYSQL).parse(sql);
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

    final ASTNode existsSubquery =
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
    final ASTNode node = ASTParser.ofDb(MYSQL).parse(sql);
    final ASTContext context = node.context();

    context.setSchema(schema);

    Relation relation = node.get(RELATION);
    assertEquals(5, relation.attributes().size());
    final Snapshot snapshot0 = context.snapshot();

    context.derive();
    final ASTNode existsSubquery =
        node.get(QUERY_BODY).get(QUERY_SPEC_WHERE).get(EXISTS_SUBQUERY_EXPR).get(QUERY_EXPR_QUERY);

    final ASTNode literal = ASTNode.expr(LITERAL);
    literal.set(LITERAL_TYPE, LiteralType.BOOL);
    literal.set(LITERAL_VALUE, true);
    assertFalse(relation.isOutdated());
    existsSubquery.update(literal);
    assertTrue(relation.isOutdated());
    relation = node.get(RELATION);
    assertEquals(5, relation.attributes().size());

    final ASTNode tblB = relation.inputs().get(1).node();
    final ASTNode tblC = ASTNode.tableSource(SIMPLE_SOURCE);
    final ASTNode tblCName = ASTNode.node(TABLE_NAME);
    tblCName.set(TABLE_NAME_TABLE, "c");
    tblC.set(SIMPLE_TABLE, tblCName);
    assertFalse(relation.isOutdated());
    tblB.update(tblC);
    assertTrue(relation.isOutdated());
    relation = node.get(RELATION);
    assertEquals(7, relation.attributes().size());

    final ASTNode tblA = relation.inputs().get(0).node();
    final ASTNode subquery = tblA.get(DERIVED_SUBQUERY).get(QUERY_BODY);
    assertFalse(relation.isOutdated());
    subquery.set(QUERY_SPEC_FROM, tblC.copy());
    assertTrue(relation.isOutdated());
    relation = node.get(RELATION);
    assertEquals(8, relation.attributes().size());

    context.setSnapshot(snapshot0);

    assertTrue(relation.isOutdated());
    relation = node.get(RELATION);
    assertEquals(5, relation.attributes().size());
  }
}
