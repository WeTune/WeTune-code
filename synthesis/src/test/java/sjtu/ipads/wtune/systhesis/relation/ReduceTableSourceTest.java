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
          "select a.i from a inner join (select i, x from a join b on a.j = b.y) c "
              + "on a.i = c.x "
              + "where exists (select 1 from b where a.i = 4)");
      stmt.retrofitStandard();

      final RelationGraph graph = stmt.analyze(RelationGraphAnalyzer.class);
      Relation target = null;
      for (Relation node : graph.graph().nodes())
        if (node.node().get(TABLE_SOURCE_KIND) == SQLTableSource.Kind.SIMPLE) target = node;
      assertNotNull(target);
      assertTrue(ReduceTableSource.canReduce(stmt.parsed(), graph, target));

      final ReduceTableSource op = new ReduceTableSource(graph, target);
      final Statement copy = stmt.copy();

      op.modifyGraph(copy.parsed());
      op.modifyAST(copy, copy.parsed());

      assertEquals(
          "SELECT `c`.`x` FROM (SELECT `i`, `x` FROM `a` INNER JOIN `b` ON `a`.`j` = `b`.`y`) AS `c` "
              + "WHERE EXISTS (SELECT 1 FROM `b` WHERE `c`.`x` = 4)",
          copy.parsed().toString());
      assertEquals(4, graph.graph().nodes().size());
      assertEquals(1, graph.graph().edges().size());

      op.undoModifyGraph();
      assertEquals(5, graph.graph().nodes().size());
      assertEquals(2, graph.graph().edges().size());
    }
  }
}
