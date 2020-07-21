package sjtu.ipads.wtune.systhesis.relation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.SQLTableSource;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.analyzer.RelationGraphAnalyzer;
import sjtu.ipads.wtune.stmt.attrs.Relation;
import sjtu.ipads.wtune.stmt.attrs.RelationGraph;
import sjtu.ipads.wtune.stmt.statement.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.SIMPLE_ALIAS;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.TABLE_SOURCE_KIND;

class ReduceTableSourceTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[Synthesis.Relation.ReduceTableSource] simple")
  void test() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    {
      stmt.setRawSql(
          "select A.i from a as A inner join (select i, x from a join b on a.j = b.y) c "
              + "on A.i = c.x "
              + "where exists (select 1 from b where A.i = 4)");
      stmt.retrofitStandard();

      final RelationGraph graph = stmt.analyze(RelationGraphAnalyzer.class);
      Relation target = null;
      for (Relation node : graph.graph().nodes())
        if (node.node().get(TABLE_SOURCE_KIND) == SQLTableSource.Kind.SIMPLE
            && "A".equals(node.node().get(SIMPLE_ALIAS))) target = node;
      assertNotNull(target);
      assertTrue(ReduceTableSource.canReduce(stmt.parsed(), graph, target));

      final ReduceTableSource op = new ReduceTableSource(graph, target);
      final Statement copy = stmt.copy();

      assertEquals(5, graph.graph().nodes().size());
      assertEquals(2, graph.graph().edges().size());

      op.modifyGraph(copy.parsed());
      op.modifyAST(copy, copy.parsed());

      assertEquals(
          "SELECT `c`.`x` AS `i` FROM (SELECT `i`, `x` FROM `a` INNER JOIN `b` ON `a`.`j` = `b`.`y`) AS `c` "
              + "WHERE EXISTS (SELECT 1 FROM `b` WHERE `c`.`x` = 4)",
          copy.parsed().toString());
      assertEquals(4, graph.graph().nodes().size());
      assertEquals(1, graph.graph().edges().size());

      op.undoModifyGraph();
      assertEquals(5, graph.graph().nodes().size());
      assertEquals(2, graph.graph().edges().size());
    }
  }

  @Test
  @DisplayName("[Synthesis.Relation.ReduceTableSource] simple")
  void test0() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    {
      stmt.setRawSql(
          "select a.j from a join b as b on b.x = a.i join c on c.u = b.x where c.v = '123'");
      stmt.retrofitStandard();

      final RelationGraph graph = stmt.analyze(RelationGraphAnalyzer.class);
      Relation target = null;
      for (Relation node : graph.graph().nodes())
        if (node.node().get(TABLE_SOURCE_KIND) == SQLTableSource.Kind.SIMPLE
            && "b".equals(node.node().get(SIMPLE_ALIAS))) target = node;
      assertNotNull(target);
      assertTrue(ReduceTableSource.canReduce(stmt.parsed(), graph, target));

      final ReduceTableSource op = new ReduceTableSource(graph, target);
      final Statement copy = stmt.copy();

      assertEquals(3, graph.graph().nodes().size());
      assertEquals(2, graph.graph().edges().size());

      op.modifyGraph(copy.parsed());
      op.modifyAST(copy, copy.parsed());

      assertEquals(
          "SELECT `a`.`j` FROM `a` INNER JOIN `c` ON `c`.`u` = `a`.`i` WHERE `c`.`v` = '123'",
          copy.parsed().toString());

      assertEquals(2, graph.graph().nodes().size());
      assertEquals(1, graph.graph().edges().size());

      op.undoModifyGraph();
      assertEquals(3, graph.graph().nodes().size());
      assertEquals(2, graph.graph().edges().size());
    }
  }
}
