package sjtu.ipads.wtune.systhesis.relation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.analyzer.RelationGraphAnalyzer;
import sjtu.ipads.wtune.stmt.attrs.Relation;
import sjtu.ipads.wtune.stmt.attrs.RelationGraph;
import sjtu.ipads.wtune.stmt.statement.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InlineSubqueryTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[Synthesis.Relation.InlineSubquery] simple")
  void test() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    {
      stmt.setRawSql("select 1 from a where a.i in (select x from b)");
      stmt.retrofitStandard();

      final RelationGraph graph = stmt.analyze(RelationGraphAnalyzer.class);
      Relation target = null;
      for (Relation node : graph.graph().nodes()) if (!node.isTableSource()) target = node;
      assertNotNull(target);

      final InlineSubquery op = new InlineSubquery(graph, target);
      final Statement copy = stmt.copy();

      op.modifyGraph();
      op.modifyAST(copy, copy.parsed());

      assertEquals(
          "SELECT 1 FROM `a` INNER JOIN (SELECT `x` FROM `b`) AS `_inlined_1_1` ON `a`.`i` = `_inlined_1_1`.`x`",
          copy.parsed().toString());
      assertEquals(2, graph.graph().nodes().size());
      assertEquals(1, graph.graph().edges().size());

      op.undoModifyGraph();
      assertEquals(2, graph.graph().nodes().size());
      assertEquals(1, graph.graph().edges().size());
    }
  }
}
