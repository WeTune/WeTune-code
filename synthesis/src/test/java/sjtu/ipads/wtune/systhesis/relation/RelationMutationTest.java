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
import static org.junit.jupiter.api.Assertions.fail;
import static sjtu.ipads.wtune.systhesis.TestHelper.fastRecycleIter;

class RelationMutationTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  private static List<Statement> doTest(Statement stmt, String expectation) {
    stmt.retrofitStandard();
    final RelationMutation mutation = RelationMutation.build(stmt);
    final List<Statement> output = new ArrayList<>();
    mutation.setNext(Stage.listCollector(output));
    mutation.feed(stmt);
    for (Statement s : output) {
      System.out.println(s.parsed().toString());
      if (expectation.equals(s.parsed().toString())) return output;
    }
    return fail();
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

  //  @Test
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

  @Test
  void broadleaf90() {
    doTest(
        Statement.findOne("broadleaf", 90),
        "SELECT `adminpermi0_`.`ADMIN_PERMISSION_ID` AS `ADMIN_PE1_4_`, `adminpermi0_`.`DESCRIPTION` AS `DESCRIPT2_4_`, `adminpermi0_`.`IS_FRIENDLY` AS `IS_FRIEN3_4_`, `adminpermi0_`.`NAME` AS `NAME4_4_`, `adminpermi0_`.`PERMISSION_TYPE` AS `PERMISSI5_4_` FROM `BLC_ADMIN_PERMISSION` AS `adminpermi0_` INNER JOIN `BLC_ADMIN_ROLE_PERMISSION_XREF` AS `allroles1_` ON `adminpermi0_`.`ADMIN_PERMISSION_ID` = `allroles1_`.`ADMIN_PERMISSION_ID` WHERE `adminpermi0_`.`IS_FRIENDLY` = 1 AND `allroles1_`.`ADMIN_ROLE_ID` = 1 ORDER BY `adminpermi0_`.`DESCRIPTION` ASC LIMIT 50");
  }

  @Test
  void broadleaf199() {
    doTest(
        Statement.findOne("broadleaf", 199),
        "SELECT `adminrolei0_`.`ADMIN_ROLE_ID` AS `ADMIN_RO1_7_`, `adminrolei0_`.`CREATED_BY` AS `CREATED_2_7_`, `adminrolei0_`.`DATE_CREATED` AS `DATE_CRE3_7_`, `adminrolei0_`.`DATE_UPDATED` AS `DATE_UPD4_7_`, `adminrolei0_`.`UPDATED_BY` AS `UPDATED_5_7_`, `adminrolei0_`.`DESCRIPTION` AS `DESCRIPT6_7_`, `adminrolei0_`.`NAME` AS `NAME7_7_` FROM `BLC_ADMIN_ROLE` AS `adminrolei0_` INNER JOIN `BLC_ADMIN_USER_ROLE_XREF` AS `allusers1_` ON `adminrolei0_`.`ADMIN_ROLE_ID` = `allusers1_`.`ADMIN_ROLE_ID` WHERE `allusers1_`.`ADMIN_USER_ID` = 1 ORDER BY `adminrolei0_`.`ADMIN_ROLE_ID` ASC LIMIT 50");
  }

  @Test
  void broadleaf200() {
    doTest(
        Statement.findOne("broadleaf", 200),
        "SELECT COUNT(`adminrolei0_`.`ADMIN_ROLE_ID`) AS `col_0_0_` FROM `BLC_ADMIN_ROLE` AS `adminrolei0_` INNER JOIN `BLC_ADMIN_USER_ROLE_XREF` AS `allusers1_` ON `adminrolei0_`.`ADMIN_ROLE_ID` = `allusers1_`.`ADMIN_ROLE_ID` WHERE `allusers1_`.`ADMIN_USER_ID` = 1");
  }

  @Test
  void broadleaf201() {
    doTest(
        Statement.findOne("broadleaf", 201),
        "SELECT `adminpermi0_`.`ADMIN_PERMISSION_ID` AS `ADMIN_PE1_4_`, `adminpermi0_`.`DESCRIPTION` AS `DESCRIPT2_4_`, `adminpermi0_`.`IS_FRIENDLY` AS `IS_FRIEN3_4_`, `adminpermi0_`.`NAME` AS `NAME4_4_`, `adminpermi0_`.`PERMISSION_TYPE` AS `PERMISSI5_4_` FROM `BLC_ADMIN_PERMISSION` AS `adminpermi0_` INNER JOIN `BLC_ADMIN_USER_PERMISSION_XREF` AS `allusers1_` ON `adminpermi0_`.`ADMIN_PERMISSION_ID` = `allusers1_`.`ADMIN_PERMISSION_ID` WHERE `adminpermi0_`.`IS_FRIENDLY` = 1 AND `allusers1_`.`ADMIN_USER_ID` = 1 ORDER BY `adminpermi0_`.`DESCRIPTION` ASC LIMIT 50");
  }

  @Test
  void broadleaf202() {
    doTest(
        Statement.findOne("broadleaf", 202),
        "SELECT COUNT(`adminpermi0_`.`ADMIN_PERMISSION_ID`) AS `col_0_0_` FROM `BLC_ADMIN_PERMISSION` AS `adminpermi0_` INNER JOIN `BLC_ADMIN_USER_PERMISSION_XREF` AS `allusers1_` ON `adminpermi0_`.`ADMIN_PERMISSION_ID` = `allusers1_`.`ADMIN_PERMISSION_ID` WHERE `adminpermi0_`.`IS_FRIENDLY` = 1 AND `allusers1_`.`ADMIN_USER_ID` = 1");
  }

  @Test
  void broadleaf241() {
    doTest(
        Statement.findOne("broadleaf", 241),
        "SELECT COUNT(`adminuseri0_`.`ADMIN_USER_ID`) AS `col_0_0_` FROM `BLC_ADMIN_USER` AS `adminuseri0_` WHERE `adminuseri0_`.`ARCHIVED` = 'N' OR `adminuseri0_`.`ARCHIVED` IS NULL");
  }

  @Test
  void diaspora124() {
    doTest(
        Statement.findOne("diaspora", 124),
        "SELECT `conversations`.`updated_at`, `conversations`.`subject`, `conversations`.`guid`, `conversations`.`created_at`, `conversations`.`id`, `conversations`.`author_id` FROM `conversations` INNER JOIN `conversation_visibilities` ON `conversations`.`id` = `conversation_visibilities`.`conversation_id` INNER JOIN `people` ON `conversation_visibilities`.`person_id` = `people`.`id` WHERE `people`.`owner_id` = 1 AND `conversation_visibilities`.`person_id` = 1 AND `conversation_visibilities`.`conversation_id` = 202 ORDER BY `conversations`.`id` ASC LIMIT 1");
  }

  @Test
  void diaspora492() {
    doTest(
        Statement.findOne("diaspora", 492),
        "SELECT COUNT(*) FROM `conversation_visibilities` AS `conversation_visibilities_exposed_1_1` WHERE `conversation_visibilities_exposed_1_1`.`person_id` = 2");
  }

  @Test
  void diaspora576() {
    doTest(
        Statement.findOne("diaspora", 576),
        "SELECT COUNT(DISTINCT `people_exposed_1_1`.`id`) FROM `people` AS `people_exposed_1_1` INNER JOIN `profiles` AS `profiles_exposed_1_1` ON `profiles_exposed_1_1`.`person_id` = `people_exposed_1_1`.`id` INNER JOIN `contacts` AS `contacts_people_exposed_1_1` ON `contacts_people_exposed_1_1`.`person_id` = `people_exposed_1_1`.`id` INNER JOIN `aspect_memberships` AS `aspect_memberships_exposed_1_1` ON `aspect_memberships_exposed_1_1`.`contact_id` = `contacts_people_exposed_1_1`.`id` LEFT JOIN `contacts` AS `contacts_exposed_1_1` ON `contacts_exposed_1_1`.`user_id` = 485 AND `contacts_exposed_1_1`.`person_id` = `people_exposed_1_1`.`id` WHERE (`profiles_exposed_1_1`.`searchable` = TRUE OR `contacts_exposed_1_1`.`user_id` = 485) AND (`profiles_exposed_1_1`.`full_name` LIKE '%my% aspect% contact%' OR `people_exposed_1_1`.`diaspora_handle` LIKE 'myaspectcontact%') AND `people_exposed_1_1`.`closed_account` = FALSE AND `contacts_exposed_1_1`.`user_id` = 485 AND `aspect_memberships_exposed_1_1`.`aspect_id` = 319");
  }

  @Test
  void diaspora577() {
    doTest(
        Statement.findOne("diaspora", 577),
        "SELECT COUNT(DISTINCT `people_exposed_1_1`.`id`) FROM `people` AS `people_exposed_1_1` INNER JOIN `contacts` AS `contacts_exposed_1_1` ON `contacts_exposed_1_1`.`person_id` = `people_exposed_1_1`.`id` INNER JOIN `aspect_memberships` AS `aspect_memberships_exposed_1_1` ON `aspect_memberships_exposed_1_1`.`contact_id` = `contacts_exposed_1_1`.`id` INNER JOIN `people` AS `people_exposed_2_1_exposed_1_1` ON `people_exposed_1_1`.`id` = `people_exposed_2_1_exposed_1_1`.`id` INNER JOIN `profiles` AS `profiles_exposed_2_1_exposed_1_1` ON `profiles_exposed_2_1_exposed_1_1`.`person_id` = `people_exposed_2_1_exposed_1_1`.`id` INNER JOIN `contacts` AS `contacts_people_exposed_2_1_exposed_1_1` ON `contacts_people_exposed_2_1_exposed_1_1`.`person_id` = `people_exposed_2_1_exposed_1_1`.`id` INNER JOIN `aspect_memberships` AS `aspect_memberships_exposed_2_1_exposed_1_1` ON `aspect_memberships_exposed_2_1_exposed_1_1`.`contact_id` = `contacts_people_exposed_2_1_exposed_1_1`.`id` LEFT JOIN `contacts` AS `contacts_exposed_2_1_exposed_1_1` ON `contacts_exposed_2_1_exposed_1_1`.`user_id` = 488 AND `contacts_exposed_2_1_exposed_1_1`.`person_id` = `people_exposed_2_1_exposed_1_1`.`id` WHERE `contacts_exposed_1_1`.`user_id` = 488 AND `aspect_memberships_exposed_1_1`.`aspect_id` = 322 AND ((`profiles_exposed_2_1_exposed_1_1`.`searchable` = TRUE OR `contacts_exposed_2_1_exposed_1_1`.`user_id` = 488) AND (`profiles_exposed_2_1_exposed_1_1`.`full_name` LIKE '%my% aspect% contact%' OR `people_exposed_2_1_exposed_1_1`.`diaspora_handle` LIKE 'myaspectcontact%') AND `people_exposed_2_1_exposed_1_1`.`closed_account` = FALSE AND `contacts_exposed_2_1_exposed_1_1`.`user_id` = 488 AND `aspect_memberships_exposed_2_1_exposed_1_1`.`aspect_id` = 321)");
  }

  @Test
  void diaspora579() {
    doTest(
        Statement.findOne("diaspora", 579),
        "SELECT COUNT(DISTINCT `people_exposed_1_1`.`id`) FROM `people` AS `people_exposed_1_1` INNER JOIN `profiles` AS `profiles_exposed_1_1` ON `profiles_exposed_1_1`.`person_id` = `people_exposed_1_1`.`id` INNER JOIN `contacts` AS `contacts_people_exposed_1_1` ON `contacts_people_exposed_1_1`.`person_id` = `people_exposed_1_1`.`id` INNER JOIN `aspect_memberships` AS `aspect_memberships_exposed_1_1` ON `aspect_memberships_exposed_1_1`.`contact_id` = `contacts_people_exposed_1_1`.`id` LEFT JOIN `contacts` AS `contacts_exposed_1_1` ON `contacts_exposed_1_1`.`user_id` = 485 AND `contacts_exposed_1_1`.`person_id` = `people_exposed_1_1`.`id` WHERE (`profiles_exposed_1_1`.`searchable` = TRUE OR `contacts_exposed_1_1`.`user_id` = 485) AND (`profiles_exposed_1_1`.`full_name` LIKE '%my% aspect% contact%' OR `people_exposed_1_1`.`diaspora_handle` LIKE 'myaspectcontact%') AND `people_exposed_1_1`.`closed_account` = FALSE AND `contacts_exposed_1_1`.`user_id` = 485 AND `aspect_memberships_exposed_1_1`.`aspect_id` IN (319, 320)");
  }

  @Test
  void eladmin60() {
    doTest(
        Statement.findOne("eladmin", 60),
        "SELECT `user0_`.`id` AS `id1_20_`, `user0_`.`create_time` AS `create_t2_20_`, `user0_`.`dept_id` AS `dept_id9_20_`, `user0_`.`email` AS `email3_20_`, `user0_`.`enabled` AS `enabled4_20_`, `user0_`.`job_id` AS `job_id10_20_`, `user0_`.`last_password_reset_time` AS `last_pas5_20_`, `user0_`.`password` AS `password6_20_`, `user0_`.`phone` AS `phone7_20_`, `user0_`.`avatar_id` AS `avatar_11_20_`, `user0_`.`username` AS `username8_20_` FROM `user` AS `user0_` ORDER BY `user0_`.`id` DESC LIMIT 10");
  }

  @Test
  void eladmin61() {
    doTest(
        Statement.findOne("eladmin", 61),
        "SELECT `user0_`.`id` AS `id1_20_`, `user0_`.`create_time` AS `create_t2_20_`, `user0_`.`dept_id` AS `dept_id9_20_`, `user0_`.`email` AS `email3_20_`, `user0_`.`enabled` AS `enabled4_20_`, `user0_`.`job_id` AS `job_id10_20_`, `user0_`.`last_password_reset_time` AS `last_pas5_20_`, `user0_`.`password` AS `password6_20_`, `user0_`.`phone` AS `phone7_20_`, `user0_`.`avatar_id` AS `avatar_11_20_`, `user0_`.`username` AS `username8_20_` FROM `user` AS `user0_` WHERE `user0_`.`enabled` = 1 ORDER BY `user0_`.`id` DESC LIMIT 10");
  }

  @Test
  void eladmin104() {
    doTest(
        Statement.findOne("eladmin", 104),
        "SELECT `job0_`.`id` AS `id1_6_`, `job0_`.`create_time` AS `create_t2_6_`, `job0_`.`dept_id` AS `dept_id6_6_`, `job0_`.`enabled` AS `enabled3_6_`, `job0_`.`name` AS `name4_6_`, `job0_`.`sort` AS `sort5_6_` FROM `job` AS `job0_` ORDER BY `job0_`.`sort` ASC LIMIT 10");
  }

  //  @Test
//  void eladmin105() {
//    doTest(
//        Statement.findOne("eladmin", 105),
//        "SELECT `job0_`.`id` AS `id1_6_`, `job0_`.`create_time` AS `create_t2_6_`, `job0_`.`dept_id` AS `dept_id6_6_`, `job0_`.`enabled` AS `enabled3_6_`, `job0_`.`name` AS `name4_6_`, `job0_`.`sort` AS `sort5_6_` FROM `job` AS `job0_` WHERE `job0_`.`name` LIKE '%%' ORDER BY `job0_`.`sort` ASC LIMIT 10");
//  }

  @Test
  void fatfreecrm9() {
    doTest(
        Statement.findOne("fatfreecrm", 9),
        "SELECT `taggings`.`tag_id` FROM `taggings` WHERE `taggings`.`taggable_id` = 1234 AND `taggings`.`taggable_type` = 'Contact' AND `taggings`.`context` = 'tags'");
  }

  @Test
  void fatfreecrm16() {
    doTest(
        Statement.findOne("fatfreecrm", 16),
        "SELECT `groups_users`.`group_id` FROM `groups_users` WHERE `groups_users`.`user_id` = 3056");
  }

  @Test
  void fatfreecrm19() {
    doTest(
        Statement.findOne("fatfreecrm", 19),
        "SELECT COUNT(*) FROM `accounts` AS `accounts_exposed_1_1` WHERE `accounts_exposed_1_1`.`assigned_to` = 237 OR (`accounts_exposed_1_1`.`user_id` = 237 OR `accounts_exposed_1_1`.`access` = 'Public')");
  }

  @Test
  void fatfreecrm32() {
    doTest(
        Statement.findOne("fatfreecrm", 32),
        "SELECT COUNT(*) FROM `accounts` AS `accounts_exposed_1_1` WHERE (`accounts_exposed_1_1`.`assigned_to` = 238 OR (`accounts_exposed_1_1`.`user_id` = 238 OR `accounts_exposed_1_1`.`access` = 'Public')) AND `accounts_exposed_1_1`.`category` IN ('customer', 'vendor')");
  }

  @Test
  void fatfreecrm33() {
    doTest(
        Statement.findOne("fatfreecrm", 33),
        "SELECT COUNT(*) FROM `leads` AS `leads_exposed_1_1` WHERE (`leads_exposed_1_1`.`assigned_to` = 1131 OR (`leads_exposed_1_1`.`user_id` = 1131 OR `leads_exposed_1_1`.`access` = 'Public')) AND `leads_exposed_1_1`.`status` IN ('new')");
  }
}
