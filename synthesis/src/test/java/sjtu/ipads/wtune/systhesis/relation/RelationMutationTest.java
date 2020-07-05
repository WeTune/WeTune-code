package sjtu.ipads.wtune.systhesis.relation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.systhesis.Stage;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.systhesis.TestHelper.fastRecycleIter;

class RelationMutationTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  @DisplayName("[Synthesis.Relation.Mutation] mutation_0")
  void test4() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    stmt.setRawSql("select 1 from a where exists (select 1 from b where a.i = b.x)");
    stmt.retrofitStandard();

    final List<Statement> output = new ArrayList<>();

    final RelationMutation mutation = RelationMutation.build(stmt);
    mutation.setNext(Stage.listCollector(output));
    mutation.feed(stmt);

    assertEquals(1, output.size());
    assertEquals(
        "SELECT 1 FROM `a` WHERE EXISTS (SELECT 1 FROM `b` WHERE `a`.`i` = `b`.`x`)",
        stmt.parsed().toString());
    assertEquals(
        "SELECT 1 FROM `a` WHERE EXISTS (SELECT 1 FROM `b` WHERE `a`.`i` = `b`.`x`)",
        output.get(0).parsed().toString());
  }

  @Test
  @DisplayName("[Synthesis.Relation.Mutation] mutation_0")
  void test0() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    stmt.setRawSql("select count(*) from (select 1 from a where a.i = 1) sub");
    stmt.retrofitStandard();

    final List<Statement> output = new ArrayList<>();

    final RelationMutation mutation = RelationMutation.build(stmt);
    mutation.setNext(Stage.listCollector(output));
    mutation.feed(stmt);

    assertEquals(2, output.size());
    assertEquals(
        "SELECT COUNT(*) FROM (SELECT 1 FROM `a` WHERE `a`.`i` = 1) AS `sub`",
        stmt.parsed().toString());
    assertEquals(
        "SELECT COUNT(*) FROM (SELECT 1 FROM `a` WHERE `a`.`i` = 1) AS `sub`",
        output.get(0).parsed().toString());
    assertEquals(
        "SELECT COUNT(*) FROM `a` AS `a_exposed_1_1` WHERE `a_exposed_1_1`.`i` = 1",
        output.get(1).parsed().toString());
  }

  @Test
  @DisplayName("[Synthesis.Relation.Mutation] mutation_1")
  void test1() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    stmt.setRawSql("select a.i, b.x from a inner join b on a.i = b.x where a.i = 1");
    stmt.retrofitStandard();

    final List<Statement> output = new ArrayList<>();

    final RelationMutation mutation = RelationMutation.build(stmt);
    mutation.setNext(Stage.listCollector(output));
    mutation.feed(stmt);

    assertEquals(3, output.size());
    assertEquals(
        "SELECT `a`.`i`, `b`.`x` FROM `a` INNER JOIN `b` ON `a`.`i` = `b`.`x` WHERE `a`.`i` = 1",
        stmt.parsed().toString());
    assertEquals(
        "SELECT `a`.`i`, `b`.`x` FROM `a` INNER JOIN `b` ON `a`.`i` = `b`.`x` WHERE `a`.`i` = 1",
        output.get(0).parsed().toString());
    assertEquals(
        "SELECT `a`.`i`, `a`.`i` FROM `a` WHERE `a`.`i` = 1", output.get(1).parsed().toString());
    assertEquals(
        "SELECT `b`.`x`, `b`.`x` FROM `b` WHERE `b`.`x` = 1", output.get(2).parsed().toString());
  }

  @Test
  @DisplayName("[Synthesis.Relation.Mutation] mutation_2")
  void test2() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    stmt.setRawSql("select 1 from a where a.i in (select b.x from b where b.y = 2)");
    stmt.retrofitStandard();

    final List<Statement> output = new ArrayList<>();

    final RelationMutation mutation = RelationMutation.build(stmt);
    mutation.setNext(Stage.listCollector(output));
    mutation.feed(stmt);

    assertEquals(6, output.size());
    assertEquals(
        "SELECT 1 FROM `a` WHERE `a`.`i` IN (SELECT `b`.`x` FROM `b` WHERE `b`.`y` = 2)",
        stmt.parsed().toString());
    assertEquals(
        "SELECT 1 FROM `a` WHERE `a`.`i` IN (SELECT `b`.`x` FROM `b` WHERE `b`.`y` = 2)",
        output.get(0).parsed().toString());
    assertEquals(
        "SELECT 1 FROM `a` INNER JOIN (SELECT `b`.`x` FROM `b` WHERE `b`.`y` = 2) AS `_inlined_1_1` ON `a`.`i` = `_inlined_1_1`.`x`",
        output.get(1).parsed().toString());
    assertEquals("SELECT 1 FROM `a`", output.get(2).parsed().toString());
    assertEquals(
        "SELECT 1 FROM `a` INNER JOIN `b` AS `b_exposed_1_1` ON `a`.`i` = `b_exposed_1_1`.`x` WHERE `b_exposed_1_1`.`y` = 2",
        output.get(3).parsed().toString());
    assertEquals(
        "SELECT 1 FROM (SELECT `b`.`x` FROM `b` WHERE `b`.`y` = 2) AS `_inlined_1_1`",
        output.get(4).parsed().toString());
    assertEquals(
        "SELECT 1 FROM `b` AS `b_exposed_1_1` WHERE `b_exposed_1_1`.`y` = 2",
        output.get(5).parsed().toString());
  }

  @Test
  @DisplayName("[Synthesis.Relation.Mutation] all statements")
  void test3() {
    final List<Statement> all = Statement.findAll();

    for (Statement statement : fastRecycleIter(all)) {
      if (statement.parsed() == null) continue;

      statement.retrofitStandard();
      //      System.out.println(statement);

      final RelationMutation mutation = RelationMutation.build(statement);
      final List<Statement> output = new ArrayList<>();
      mutation.setNext(Stage.listCollector(output));
      mutation.feed(statement);
      //      if (output.size() != 1) System.out.println(output.size());
    }
  }
}
