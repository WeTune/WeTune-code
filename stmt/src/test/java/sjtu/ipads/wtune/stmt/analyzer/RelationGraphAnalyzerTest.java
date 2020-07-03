package sjtu.ipads.wtune.stmt.analyzer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.dao.StatementDao;
import sjtu.ipads.wtune.stmt.statement.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelationGraphAnalyzerTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @AfterAll
  static void toreDown() {
    StatementDao.getGlobal().close();
  }

  @Test
  @DisplayName("[Stmt.Analyzer.RelationGraph] collect relation")
  void testCollectRelation() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    stmt.setRawSql(
        "select * from a join (select 1 from (select 2 from b) as b) as b "
            + "where a.i = (select 3 from b) and a.j in (select 4 from b) "
            + "and exists (select 5 from b) and not a.k in (select 7 from b) "
            + "and (a.i = 0 or a.i in (select 8 from b))");
    stmt.retrofitStandard();
    assertEquals(6, RelationGraphAnalyzer.collectRelation(stmt.parsed()).size());
  }

  @Test
  @DisplayName("[Stmt.Analyzer.RelationGraph] collect join condition")
  void testCollectJoinCondition() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    stmt.setRawSql(
        "select * from a join (select b.i x from (select a.i from b join a on a.k = b.z) as b) as b "
            + "on a.i = b.x "
            + "where a.i = (select 3 from b) and a.j in (select 4 from b) "
            + "and exists (select 5 from b) and not a.k in (select 7 from b) "
            + "and (a.i = 0 or a.i in (select 8 from b))");
    stmt.retrofitStandard();
    assertEquals(4, RelationGraphAnalyzer.collectJoinCondition(stmt.parsed()).size());
  }

  @Test
  @DisplayName("[Stmt.Analyzer.RelationGraph] build graph")
  void testBuildGraph() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    stmt.setRawSql(
        "select * from a join (select 1 x from (select 2 from b join a on a.k = b.z) as b) as b "
            + "on a.i = b.x "
            + "where a.i = (select x from b) and a.j in (select y from b) "
            + "and exists (select 5 from b) and not a.k in (select 7 from b) "
            + "and (a.i = 0 or a.i in (select 8 from b))");
    stmt.retrofitStandard();
    System.out.println(stmt.analyze(RelationGraphAnalyzer.class));
    //    assertEquals(4, RelationGraphAnalyzer.collectJoinCondition(stmt.parsed()).size());
  }
}
