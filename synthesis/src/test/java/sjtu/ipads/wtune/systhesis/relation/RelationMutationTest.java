package sjtu.ipads.wtune.systhesis.relation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.systhesis.Stage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.systhesis.TestHelper.fastRecycleIter;

class RelationMutationTest {
  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  private static List<Statement> doTest(Statement stmt, String... expectations) {
    stmt.retrofitStandard();
    final RelationMutation mutation = RelationMutation.build(stmt);
    final List<Statement> output = new ArrayList<>();
    mutation.setNext(Stage.listCollector(output));
    mutation.feed(stmt);
    for (Statement s : output) {
      //      System.out.println(s.parsed().toString());
      for (String expectation : expectations)
        if (expectation.equals(s.parsed().toString())) return output;
    }
    return fail();
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
    final String expect1 = "SELECT `a`.`i`, `a`.`i` FROM `a` WHERE `a`.`i` = 1";
    final String expect2 = "SELECT `b`.`x`, `b`.`x` FROM `b` WHERE `b`.`x` = 1";
    final String output1 = output.get(1).parsed().toString();
    final String output2 = output.get(2).parsed().toString();
    assertTrue(
        (expect1.equals(output1) && expect2.equals(output2))
            || (expect1.equals(output2) && expect2.equals(output1)));
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
    final Set<String> expectations =
        new HashSet<>(
            Set.of(
                "SELECT 1 FROM `a` WHERE `a`.`i` IN (SELECT `b`.`x` FROM `b` WHERE `b`.`y` = 2)",
                "SELECT 1 FROM `a` INNER JOIN (SELECT `b`.`x` FROM `b` WHERE `b`.`y` = 2) AS `_inlined_1_1` ON `a`.`i` = "
                    + "`_inlined_1_1`.`x`",
                "SELECT 1 FROM `a` INNER JOIN `b` AS `b_exposed_1_1` ON `a`.`i` = `b_exposed_1_1`.`x` WHERE "
                    + "`b_exposed_1_1`.`y` = 2",
                "SELECT 1 FROM (SELECT `b`.`x` FROM `b` WHERE `b`.`y` = 2) AS `_inlined_1_1`",
                "SELECT 1 FROM `b` AS `b_exposed_1_1` WHERE `b_exposed_1_1`.`y` = 2",
                "SELECT 1 FROM `a`"));
    for (Statement statement : output) expectations.remove(statement.parsed().toString());
    assertTrue(expectations.isEmpty());
  }

  @Test
  @DisplayName("[Synthesis.Relation.Mutation] mutation_3")
  void test3() {
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

  //  @Test
  @DisplayName("[Synthesis.Relation.Mutation] all statements")
  void testAll() {
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
    // here are two layers of exposure.
    // the order of operation will affect the table alias
    // both 2 results is correct
    doTest(
        Statement.findOne("diaspora", 577),
        "SELECT COUNT(DISTINCT `people_exposed_1_1`.`id`) FROM `people` AS `people_exposed_1_1` INNER JOIN `contacts` AS `contacts_exposed_1_1` ON `contacts_exposed_1_1`.`person_id` = `people_exposed_1_1`.`id` INNER JOIN `aspect_memberships` AS `aspect_memberships_exposed_1_1` ON `aspect_memberships_exposed_1_1`.`contact_id` = `contacts_exposed_1_1`.`id` INNER JOIN `people` AS `people_exposed_1_2` ON `people_exposed_1_1`.`id` = `people_exposed_1_2`.`id` INNER JOIN `profiles` AS `profiles_exposed_1_1` ON `profiles_exposed_1_1`.`person_id` = `people_exposed_1_2`.`id` INNER JOIN `contacts` AS `contacts_people_exposed_1_1` ON `contacts_people_exposed_1_1`.`person_id` = `people_exposed_1_2`.`id` INNER JOIN `aspect_memberships` AS `aspect_memberships_exposed_1_2` ON `aspect_memberships_exposed_1_2`.`contact_id` = `contacts_people_exposed_1_1`.`id` LEFT JOIN `contacts` AS `contacts_exposed_1_2` ON `contacts_exposed_1_2`.`user_id` = 488 AND `contacts_exposed_1_2`.`person_id` = `people_exposed_1_2`.`id` WHERE `contacts_exposed_1_1`.`user_id` = 488 AND `aspect_memberships_exposed_1_1`.`aspect_id` = 322 AND ((`profiles_exposed_1_1`.`searchable` = TRUE OR `contacts_exposed_1_2`.`user_id` = 488) AND (`profiles_exposed_1_1`.`full_name` LIKE '%my% aspect% contact%' OR `people_exposed_1_2`.`diaspora_handle` LIKE 'myaspectcontact%') AND `people_exposed_1_2`.`closed_account` = FALSE AND `contacts_exposed_1_2`.`user_id` = 488 AND `aspect_memberships_exposed_1_2`.`aspect_id` = 321)",
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

  @Test
  void eladmin105() {
    doTest(
        Statement.findOne("eladmin", 105),
        "SELECT `job0_`.`id` AS `id1_6_`, `job0_`.`create_time` AS `create_t2_6_`, `job0_`.`dept_id` AS `dept_id6_6_`, `job0_`.`enabled` AS `enabled3_6_`, `job0_`.`name` AS `name4_6_`, `job0_`.`sort` AS `sort5_6_` FROM `job` AS `job0_` WHERE `job0_`.`name` LIKE '%d%' ORDER BY `job0_`.`sort` ASC LIMIT 10");
  }

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
  void fatfreecrm80() {
    doTest(
        Statement.findOne("fatfreecrm", 80),
        "SELECT COUNT(*) FROM `opportunities` AS `opportunities_exposed_1_1` WHERE `opportunities_exposed_1_1`.`assigned_to` = 1496 OR (`opportunities_exposed_1_1`.`user_id` = 1496 OR `opportunities_exposed_1_1`.`access` = 'Public')");
  }

  @Test
  void fatfreecrm87() {
    doTest(
        Statement.findOne("fatfreecrm", 87),
        "SELECT COUNT(*) FROM `leads` AS `leads_exposed_1_1` WHERE `leads_exposed_1_1`.`assigned_to` = 980 OR (`leads_exposed_1_1`.`user_id` = 980 OR `leads_exposed_1_1`.`access` = 'Public')");
  }

  @Test
  void fatfreecrm94() {
    doTest(
        Statement.findOne("fatfreecrm", 94),
        "SELECT COUNT(*) FROM `accounts` AS `accounts_exposed_1_1` WHERE (`accounts_exposed_1_1`.`assigned_to` = 239 OR (`accounts_exposed_1_1`.`user_id` = 239 OR `accounts_exposed_1_1`.`access` = 'Public')) AND (`accounts_exposed_1_1`.`name` LIKE '%second%' OR `accounts_exposed_1_1`.`email` LIKE '%second%')");
  }

  @Test
  void fatfreecrm96() {
    doTest(
        Statement.findOne("fatfreecrm", 96),
        "SELECT COUNT(*) FROM `accounts` AS `accounts_exposed_1_1` WHERE `accounts_exposed_1_1`.`assigned_to` = 358 OR (`accounts_exposed_1_1`.`user_id` = 358 OR `accounts_exposed_1_1`.`access` = 'Public')");
  }

  @Test
  void fatfreecrm141() {
    doTest(
        Statement.findOne("fatfreecrm", 141),
        "SELECT DISTINCT `contact_opportunities`.`opportunity_id` FROM `contact_opportunities` WHERE `contact_opportunities`.`contact_id` = 2076 ORDER BY `contact_opportunities`.`opportunity_id` DESC");
  }

  @Test
  void fatfreecrm147() {
    doTest(
        Statement.findOne("fatfreecrm", 147),
        "SELECT COUNT(*) FROM `opportunities` AS `opportunities_exposed_1_1` WHERE `opportunities_exposed_1_1`.`assigned_to` = 1359 OR (`opportunities_exposed_1_1`.`user_id` = 1359 OR `opportunities_exposed_1_1`.`access` = 'Public')");
  }

  @Test
  void fatfreecrm152() {
    doTest(
        Statement.findOne("fatfreecrm", 152),
        "SELECT COUNT(*) FROM `campaigns` AS `campaigns_exposed_1_1` WHERE (`campaigns_exposed_1_1`.`assigned_to` = 363 OR (`campaigns_exposed_1_1`.`user_id` = 363 OR `campaigns_exposed_1_1`.`access` = 'Public')) AND `campaigns_exposed_1_1`.`status` IN ('planned', 'started')");
  }

  @Test
  void fatfreecrm154() {
    doTest(
        Statement.findOne("fatfreecrm", 154),
        "SELECT COUNT(*) FROM `contacts` AS `contacts_exposed_1_1` WHERE (`contacts_exposed_1_1`.`assigned_to` = 547 OR (`contacts_exposed_1_1`.`user_id` = 547 OR `contacts_exposed_1_1`.`access` = 'Public')) AND (`contacts_exposed_1_1`.`first_name` LIKE '%page_sanford@kuvalisraynor.biz%' OR `contacts_exposed_1_1`.`last_name` LIKE '%page_sanford@kuvalisraynor.biz%' OR (`contacts_exposed_1_1`.`email` LIKE '%page_sanford@kuvalisraynor.biz%' OR `contacts_exposed_1_1`.`alt_email` LIKE '%page_sanford@kuvalisraynor.biz%' OR `contacts_exposed_1_1`.`phone` LIKE '%page_sanford@kuvalisraynor.biz%' OR `contacts_exposed_1_1`.`mobile` LIKE '%page_sanford@kuvalisraynor.biz%'))");
  }

  @Test
  void fatfreecrm155() {
    doTest(
        Statement.findOne("fatfreecrm", 155),
        "SELECT DISTINCT `account_opportunities`.`opportunity_id` FROM `account_opportunities` WHERE `account_opportunities`.`account_id` = 1106 ORDER BY `account_opportunities`.`opportunity_id` DESC");
  }

  @Test
  void fatfreecrm156() {
    doTest(
        Statement.findOne("fatfreecrm", 156),
        "SELECT DISTINCT `account_contacts`.`contact_id` FROM `account_contacts` WHERE `account_contacts`.`account_id` = 1106");
  }

  @Test
  void fatfreecrm178() {
    doTest(
        Statement.findOne("fatfreecrm", 178),
        "SELECT COUNT(*) FROM `opportunities` AS `opportunities_exposed_1_1` WHERE (`opportunities_exposed_1_1`.`assigned_to` = 1503 OR (`opportunities_exposed_1_1`.`user_id` = 1503 OR `opportunities_exposed_1_1`.`access` = 'Public')) AND `opportunities_exposed_1_1`.`stage` IN ('prospecting')");
  }

  @Test
  void fatfreecrm181() {
    doTest(
        Statement.findOne("fatfreecrm", 181),
        "SELECT COUNT(*) FROM `contacts` AS `contacts_exposed_1_1` WHERE `contacts_exposed_1_1`.`assigned_to` = 865 OR (`contacts_exposed_1_1`.`user_id` = 865 OR `contacts_exposed_1_1`.`access` = 'Public')");
  }

  @Test
  void fatfreecrm187() {
    doTest(
        Statement.findOne("fatfreecrm", 187),
        "SELECT COUNT(*) FROM `campaigns` AS `campaigns_exposed_1_1` WHERE `campaigns_exposed_1_1`.`assigned_to` = 541 OR (`campaigns_exposed_1_1`.`user_id` = 541 OR `campaigns_exposed_1_1`.`access` = 'Public')");
  }

  @Test
  void fatfreecrm190() {
    doTest(
        Statement.findOne("fatfreecrm", 190),
        "SELECT COUNT(*) FROM `opportunities` AS `opportunities_exposed_1_1` WHERE (`opportunities_exposed_1_1`.`assigned_to` = 1143 OR (`opportunities_exposed_1_1`.`user_id` = 1143 OR `opportunities_exposed_1_1`.`access` = 'Public')) AND `opportunities_exposed_1_1`.`name` LIKE '%second%'");
  }

  @Test
  void fatfreecrm192() {
    doTest(
        Statement.findOne("fatfreecrm", 192),
        "SELECT COUNT(*) FROM `leads` AS `leads_exposed_1_1` WHERE (`leads_exposed_1_1`.`assigned_to` = 879 OR (`leads_exposed_1_1`.`user_id` = 879 OR `leads_exposed_1_1`.`access` = 'Public')) AND (`leads_exposed_1_1`.`first_name` LIKE '%bill%' OR `leads_exposed_1_1`.`last_name` LIKE '%bill%' OR `leads_exposed_1_1`.`company` LIKE '%bill%' OR `leads_exposed_1_1`.`email` LIKE '%bill%')");
  }

  @Test
  void febs52() {
    doTest(Statement.findOne("febs", 52), "SELECT COUNT(1) FROM `t_user` AS `u_exposed_1_1`");
  }

  @Test
  void febs94() {
    doTest(
        Statement.findOne("febs", 94),
        "SELECT COUNT(1) FROM `t_user` AS `u_exposed_1_1` WHERE `u_exposed_1_1`.`username` = 'Jana'");
  }

  @Test
  void febs96() {
    doTest(
        Statement.findOne("febs", 96),
        "SELECT COUNT(1) FROM `t_user` AS `u_exposed_1_1` WHERE `u_exposed_1_1`.`ssex` = '1'");
  }

  @Test
  void febs98() {
    doTest(
        Statement.findOne("febs", 98),
        "SELECT COUNT(1) FROM `t_user` AS `u_exposed_1_1` WHERE `u_exposed_1_1`.`mobile` = '17711111111'");
  }

  @Test
  void febs101() {
    doTest(Statement.findOne("febs", 101), "SELECT COUNT(1) FROM `t_role` AS `r_exposed_1_1`");
  }

  @Test
  void guns26() {
    doTest(
        Statement.findOne("guns", 26),
        "SELECT `r`.`role_id` AS `id`, `r`.`pid`, `r`.`name`, CASE WHEN (`r`.`pid` = 0 OR `r`.`pid` IS NULL) THEN 'true' ELSE 'false' END AS `open`, CASE WHEN (`r`.`role_id` = 0 OR `r`.`role_id` IS NULL) THEN 'false' ELSE 'true' END AS `checked` FROM `sys_role` AS `r` WHERE `r`.`role_id` IN (1) ORDER BY `pid`, `sort` ASC");
  }

  @Test
  void guns60() {
    doTest(
        Statement.findOne("guns", 60),
        "SELECT `m1`.`menu_id` AS `id`, `m1`.`icon` AS `icon`, CASE WHEN (`m2`.`menu_id` = 0 OR `m2`.`menu_id` IS NULL) THEN 0 ELSE `m2`.`menu_id` END AS `parentId`, `m1`.`name` AS `name`, `m1`.`url` AS `url`, `m1`.`levels` AS `levels`, `m1`.`menu_flag` AS `ismenu`, `m1`.`sort` AS `num` FROM `sys_menu` AS `m1` LEFT JOIN `sys_menu` AS `m2` ON `m1`.`pcode` = `m2`.`code` INNER JOIN `sys_relation` AS `rela_exposed_1_1` ON `m1`.`menu_id` = `rela_exposed_1_1`.`menu_id` WHERE `m1`.`menu_flag` = 'Y' AND `rela_exposed_1_1`.`role_id` IN (1) ORDER BY `levels`, `m1`.`sort` ASC");
  }

  @Test
  void lobsters91() {
    doTest(
        Statement.findOne("lobsters", 91),
        "SELECT COUNT(DISTINCT `comments`.`id`) FROM `comments` WHERE `comments`.`is_deleted` = FALSE AND `comments`.`is_moderated` = FALSE AND `comments`.`story_id` IN (67, 68, 69, 71) AND MATCH `comment` AGAINST ('comment2 comment3' IN BOOLEAN MODE)");
  }

  @Test
  void lobsters92() {
    doTest(
        Statement.findOne("lobsters", 92),
        "SELECT COUNT(DISTINCT `comments`.`id`) FROM `comments` WHERE `comments`.`is_deleted` = FALSE AND `comments`.`is_moderated` = FALSE AND `comments`.`story_id` IN (67, 68, 69, 71)");
  }

  @Test
  void lobsters93() {
    doTest(
        Statement.findOne("lobsters", 93),
        "SELECT COUNT(DISTINCT `comments`.`id`) FROM `comments` WHERE `comments`.`is_deleted` = FALSE AND `comments`.`is_moderated` = FALSE AND `comments`.`story_id` = 67");
  }

  @Test
  void lobsters127() {
    doTest(
        Statement.findOne("lobsters", 127),
        "SELECT COUNT(DISTINCT `stories`.`id`) FROM `stories` INNER JOIN `domains` ON `domains`.`id` = `stories`.`domain_id` WHERE `stories`.`merged_story_id` IS NULL AND `stories`.`is_expired` = FALSE AND `domains`.`domain` = 'lobste.rs' AND (MATCH `stories`.`title` AGAINST ('term1' IN BOOLEAN MODE) OR MATCH `stories`.`description` AGAINST ('term1' IN BOOLEAN MODE) OR MATCH `stories`.`story_cache` AGAINST ('term1' IN BOOLEAN MODE))");
  }

  @Test
  void lobsters129() {
    doTest(
        Statement.findOne("lobsters", 129),
        "SELECT COUNT(DISTINCT `stories`.`id`) FROM `stories` WHERE `stories`.`merged_story_id` IS NULL AND `stories`.`is_expired` = FALSE AND (MATCH `stories`.`title` AGAINST ('unique' IN BOOLEAN MODE) OR MATCH `stories`.`description` AGAINST ('unique' IN BOOLEAN MODE) OR MATCH `stories`.`story_cache` AGAINST ('unique' IN BOOLEAN MODE))");
  }

  @Test
  void pybbs31() {
    doTest(Statement.findOne("pybbs", 31), "SELECT COUNT(1) FROM `topic` AS `t`");
  }

  @Test
  void pybbs33() {
    doTest(
        Statement.findOne("pybbs", 33),
        "SELECT COUNT(1) FROM `topic` AS `t` WHERE `t`.`in_time` BETWEEN '2019-10-11' AND '2019-10-26'");
  }

  @Test
  void pybbs51() {
    doTest(Statement.findOne("pybbs", 51), "SELECT COUNT(1) FROM `comment` AS `c`");
  }

  @Test
  void shopizer41() {
    doTest(
        Statement.findOne("shopizer", 41),
        "SELECT COUNT(DISTINCT `product0_`.`PRODUCT_ID`) AS `col_0_0_` FROM `PRODUCT` AS `product0_` INNER JOIN `PRODUCT_DESCRIPTION` AS `descriptio1_` ON `product0_`.`PRODUCT_ID` = `descriptio1_`.`PRODUCT_ID` INNER JOIN `PRODUCT_CATEGORY` AS `categories2_` ON `product0_`.`PRODUCT_ID` = `categories2_`.`PRODUCT_ID` WHERE `product0_`.`MERCHANT_ID` = 1 AND `descriptio1_`.`LANGUAGE_ID` = 1 AND `categories2_`.`CATEGORY_ID` IN (2) AND `product0_`.`AVAILABLE` = 1 AND `product0_`.`DATE_AVAILABLE` <= '2019-10-21 21:11:10.902'");
  }

  @Test
  void shopizer59() {
    doTest(
        Statement.findOne("shopizer", 59),
        "SELECT COUNT(DISTINCT `product0_`.`PRODUCT_ID`) AS `col_0_0_` FROM `PRODUCT` AS `product0_` INNER JOIN `PRODUCT_DESCRIPTION` AS `descriptio1_` ON `product0_`.`PRODUCT_ID` = `descriptio1_`.`PRODUCT_ID` INNER JOIN `PRODUCT_CATEGORY` AS `categories2_` ON `product0_`.`PRODUCT_ID` = `categories2_`.`PRODUCT_ID` WHERE `product0_`.`MERCHANT_ID` = 1 AND `descriptio1_`.`LANGUAGE_ID` = 1 AND `categories2_`.`CATEGORY_ID` IN (1)");
  }

  @Test
  void shopizer67() {
    doTest(
        Statement.findOne("shopizer", 67),
        "SELECT COUNT(DISTINCT `product0_`.`PRODUCT_ID`) AS `col_0_0_` FROM `PRODUCT` AS `product0_` INNER JOIN `PRODUCT_DESCRIPTION` AS `descriptio1_` ON `product0_`.`PRODUCT_ID` = `descriptio1_`.`PRODUCT_ID` INNER JOIN `PRODUCT_CATEGORY` AS `categories2_` ON `product0_`.`PRODUCT_ID` = `categories2_`.`PRODUCT_ID` WHERE `product0_`.`MERCHANT_ID` = 1 AND `descriptio1_`.`LANGUAGE_ID` = 1 AND `categories2_`.`CATEGORY_ID` IN (1) AND `product0_`.`MANUFACTURER_ID` = 1 AND `product0_`.`AVAILABLE` = 1 AND `product0_`.`DATE_AVAILABLE` <= '2019-10-21 21:17:32.7'");
  }

  @Test
  void solidus206() {
    doTest(
        Statement.findOne("solidus", 206),
        "SELECT `items`.`variant_id`, `items`.`count_on_hand`, `items`.`updated_at`, `items`.`stock_location_id`, `items`.`created_at`, `items`.`id`, `items`.`backorderable`, `items`.`deleted_at` FROM `spree_stock_items` AS `items` WHERE `items`.`deleted_at` IS NULL AND `items`.`stock_location_id` = 23 LIMIT 0 OFFSET 25");
  }

  @Test
  void solidus210() {
    doTest(
        Statement.findOne("solidus", 210),
        "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_stock_items` INNER JOIN `spree_stock_items` AS `spree_stock_items_exposed_2_1` ON `spree_stock_items`.`id` = `spree_stock_items_exposed_2_1`.`id` LEFT JOIN `spree_stock_locations` AS `spree_stock_locations_exposed_2_1` ON `spree_stock_locations_exposed_2_1`.`id` = `spree_stock_items_exposed_2_1`.`stock_location_id` WHERE `spree_stock_items`.`deleted_at` IS NULL AND `spree_stock_items`.`stock_location_id` = 23 AND `spree_stock_locations_exposed_2_1`.`active` = TRUE LIMIT 0 OFFSET 25) AS `subquery_for_count`");
  }

  @Test
  void solidus475() {
    doTest(
        Statement.findOne("solidus", 475),
        "SELECT `items`.`variant_id`, `items`.`count_on_hand`, `items`.`updated_at`, `items`.`stock_location_id`, `items`.`created_at`, `items`.`id`, `items`.`backorderable`, `items`.`deleted_at` FROM `spree_stock_items` AS `items` INNER JOIN `spree_stock_items` AS `i_exposed_1_1` ON `items`.`id` = `i_exposed_1_1`.`id` LEFT JOIN `spree_stock_locations` AS `spree_stock_locations_exposed_1_1` ON `spree_stock_locations_exposed_1_1`.`id` = `i_exposed_1_1`.`stock_location_id` WHERE `items`.`deleted_at` IS NULL AND `items`.`stock_location_id` = 26 AND `items`.`id` = 68 AND `spree_stock_locations_exposed_1_1`.`active` = TRUE LIMIT 1");
  }
}
