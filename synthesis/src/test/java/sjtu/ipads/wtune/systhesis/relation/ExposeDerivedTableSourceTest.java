package sjtu.ipads.wtune.systhesis.relation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
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

class ExposeDerivedTableSourceTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[Synthesis.Relation.ExposeDerivedTableSource] simple")
  void test() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    {
      stmt.setRawSql(
          "select 1 from a left join (select b.x as m, b.y from b inner join a on b.z = a.k "
              + "where b.y = 1) c on a.i = c.m "
              + "where exists (select 1 from b where b.z = c.y)");
      stmt.retrofitStandard();

      final RelationGraph graph = stmt.analyze(RelationGraphAnalyzer.class);
      Relation target = null;
      for (Relation node : graph.graph().nodes())
        if (node.isTableSource()
            && node.node().get(TABLE_SOURCE_KIND) == SQLTableSource.Kind.DERIVED) target = node;
      assertNotNull(target);
      assertTrue(ExposeDerivedTableSource.canExpose(stmt.parsed(), target));

      final ExposeDerivedTableSource op = new ExposeDerivedTableSource(graph, target);
      final Statement copy = stmt.copy();

      op.modifyGraph(copy.parsed());
      op.modifyAST(copy, copy.parsed());
      assertEquals(
          "SELECT 1 FROM `a` "
              + "LEFT JOIN `b` AS `b_exposed_1_1` ON `a`.`i` = `b_exposed_1_1`.`x` "
              + "INNER JOIN `a` AS `a_exposed_1_1` ON `b_exposed_1_1`.`z` = `a_exposed_1_1`.`k` "
              + "WHERE EXISTS (SELECT 1 FROM `b` WHERE `b`.`z` = `b_exposed_1_1`.`y`) "
              + "AND `b_exposed_1_1`.`y` = 1",
          copy.parsed().toString());
      assertEquals(4, graph.graph().nodes().size());
      assertEquals(3, graph.graph().edges().size());

      op.undoModifyGraph();
      assertEquals(5, graph.graph().nodes().size());
      assertEquals(3, graph.graph().edges().size());
    }
  }
}
