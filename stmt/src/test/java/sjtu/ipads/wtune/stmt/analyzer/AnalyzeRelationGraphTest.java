package sjtu.ipads.wtune.stmt.analyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalyzeRelationGraphTest {
  //  @BeforeAll
  //  static void setUp() throws ClassNotFoundException {
  //    Class.forName("org.sqlite.JDBC");
  //    Setup._default().registerAsGlobal().setup();
  //  }
  //
  //  @Test
  //  @DisplayName("[Stmt.Analyzer.RelationGraph] collect relation")
  //  void testCollectRelation() {
  //    final Statement stmt =
  //        Statement.build(
  //            "test",
  //            "select * from a join (select 1 from (select 2 from b) as b) as b "
  //                + "where a.i = (select 3 from b) and a.j in (select 4 from b) "
  //                + "and exists (select 5 from b) and not a.k in (select 7 from b) "
  //                + "and (a.i = 0 or a.i in (select 8 from b))",
  //            null);
  //
  //    stmt.retrofitStandard();
  //    assertEquals(11, RelationGraphAnalyzer.collectRelation(stmt.parsed()).size());
  //  }
  //
  //  @Test
  //  @DisplayName("[Stmt.Analyzer.RelationGraph] collect join condition")
  //  void testCollectJoinCondition() {
  //    final Statement stmt =
  //        Statement.build(
  //            "test",
  //            "select * from a join (select b.i x from (select a.i from b join a on a.k = b.z) as
  // b) as b "
  //                + "on a.i = b.x "
  //                + "where a.i = (select 3 from b) and a.j in (select 4 from b) "
  //                + "and exists (select 5 from b) and not a.k in (select 7 from b) "
  //                + "and (a.i = 0 or a.i in (select 8 from b))",
  //            null);
  //    stmt.retrofitStandard();
  //    assertEquals(4, RelationGraphAnalyzer.collectJoinCondition(stmt.parsed()).size());
  //  }
  //
  //  @Test
  //  @DisplayName("[Stmt.Analyzer.RelationGraph] build graph")
  //  void testBuildGraph() {
  //    final Statement stmt =
  //        Statement.build(
  //            "test",
  //            "select * from a join (select 1 x from (select 2 from b join a on a.k = b.z) as b)
  // as b "
  //                + "on a.i = b.x "
  //                + "where a.i = (select x from b) and a.j in (select y from b) "
  //                + "and exists (select 5 from b) and not a.k in (select 7 from b) "
  //                + "and (a.i = 0 or a.i in (select 8 from b))",
  //            null);
  //    stmt.retrofitStandard();
  //    assertEquals(4, RelationGraphAnalyzer.collectJoinCondition(stmt.parsed()).size());
  //  }
}
