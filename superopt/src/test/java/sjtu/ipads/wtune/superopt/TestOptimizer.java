package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.superopt.internal.Optimizer;
import sjtu.ipads.wtune.superopt.optimization.SubstitutionBank;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static sjtu.ipads.wtune.stmt.support.Workflow.normalize;

public class TestOptimizer {
  private static SubstitutionBank bank;

  private static SubstitutionBank bank() {
    if (bank != null) return bank;

    bank = SubstitutionBank.make();

    try {
      bank.importFrom(Files.readAllLines(Paths.get("wtune_data", "substitution_bank")));
    } catch (IOException ioe) {
      throw new UncheckedIOException(ioe);
    }

    return bank;
  }

  private static void doTest(String appName, int stmtId, String... expected) {
    final Statement stmt = Statement.findOne(appName, stmtId);
    normalize(stmt);
    final ASTNode ast = stmt.parsed();
    final Schema schema = stmt.app().schema("base", true);
    ast.context().setSchema(schema);

    final Optimizer optimizer = Optimizer.make(bank(), schema);
    final List<ASTNode> optimized = optimizer.optimize(ast);

    optimized.forEach(System.out::println);
    boolean passed = false;
    for (String s : expected)
      if (optimized.stream().anyMatch(it -> s.equals(it.toString()))) {
        passed = true;
        break;
      }
    assertTrue(passed);
  }

  @Test // 1
  void testBroadleaf199() {
    doTest(
        "broadleaf",
        199,
        "SELECT `adminrolei0_`.`admin_role_id` AS `admin_ro1_7_`, `adminrolei0_`.`created_by` AS `created_2_7_`, `adminrolei0_`.`date_created` AS `date_cre3_7_`, `adminrolei0_`.`date_updated` AS `date_upd4_7_`, `adminrolei0_`.`updated_by` AS `updated_5_7_`, `adminrolei0_`.`description` AS `descript6_7_`, `adminrolei0_`.`name` AS `name7_7_` FROM `blc_admin_role` AS `adminrolei0_` INNER JOIN `blc_admin_user_role_xref` AS `allusers1_` ON `adminrolei0_`.`admin_role_id` = `allusers1_`.`admin_role_id` WHERE `allusers1_`.`admin_user_id` = 1 ORDER BY `admin_ro1_7_` ASC LIMIT 50");
  }

  @Test // 2
  void testBroadleaf200() {
    doTest(
        "broadleaf",
        200,
        "SELECT COUNT(`allusers1_`.`admin_role_id`) AS `col_0_0_` FROM `blc_admin_user_role_xref` AS `allusers1_` WHERE `allusers1_`.`admin_user_id` = 1");
  }

  @Test // 3
  void testBroadleaf201() {
    doTest(
        "broadleaf",
        201,
        "SELECT `adminpermi0_`.`admin_permission_id` AS `admin_pe1_4_`, `adminpermi0_`.`description` AS `descript2_4_`, `adminpermi0_`.`is_friendly` AS `is_frien3_4_`, `adminpermi0_`.`name` AS `name4_4_`, `adminpermi0_`.`permission_type` AS `permissi5_4_` FROM `blc_admin_permission` AS `adminpermi0_` INNER JOIN `blc_admin_user_permission_xref` AS `allusers1_` ON `adminpermi0_`.`admin_permission_id` = `allusers1_`.`admin_permission_id` WHERE `allusers1_`.`admin_user_id` = 1 AND `adminpermi0_`.`is_friendly` = 1 ORDER BY `descript2_4_` ASC LIMIT 50",
        "SELECT `adminpermi0_`.`admin_permission_id` AS `admin_pe1_4_`, `adminpermi0_`.`description` AS `descript2_4_`, `adminpermi0_`.`is_friendly` AS `is_frien3_4_`, `adminpermi0_`.`name` AS `name4_4_`, `adminpermi0_`.`permission_type` AS `permissi5_4_` FROM `blc_admin_permission` AS `adminpermi0_` INNER JOIN `blc_admin_user_permission_xref` AS `allusers1_` ON `adminpermi0_`.`admin_permission_id` = `allusers1_`.`admin_permission_id` WHERE `adminpermi0_`.`is_friendly` = 1 AND `allusers1_`.`admin_user_id` = 1 ORDER BY `descript2_4_` ASC LIMIT 50");
  }

  @Test // 4
  void testBroadleaf241() {
    doTest(
        "broadleaf",
        241,
        "SELECT COUNT(`adminuseri0_`.`admin_user_id`) AS `col_0_0_` FROM `blc_admin_user` AS `adminuseri0_` WHERE `adminuseri0_`.`archived` = 'N' OR `adminuseri0_`.`archived` IS NULL");
  }

  @Test // 5
  void testDiaspora202() {
    // XXX: distinct
    final String expected =
        "SELECT COUNT(DISTINCT `contacts`.`id`) FROM `contacts` AS `contacts` INNER JOIN `aspect_memberships` AS `aspect_memberships` ON `aspect_memberships`.`aspect_id` = 250 AND `aspect_memberships`.`contact_id` = `contacts`.`id` WHERE `contacts`.`user_id` = 332";
    doTest("diaspora", 202, expected);
  }

  // 6
  // TODO: diaspora-224 too long

  @Test // 7
  void testDiaspora295() {
    doTest(
        "diaspora",
        295,
        "SELECT COUNT(DISTINCT `contacts`.`id`) FROM `contacts` AS `contacts` WHERE `contacts`.`user_id` = 1945");
  }

  @Test // 8
  void testDiaspora460() {
    final String appName = "diaspora";
    final int stmtId = 460;
    final String expected =
        "SELECT `contacts`.`person_id` AS `person_id` FROM `contacts` AS `contacts` INNER JOIN `aspect_memberships` AS `aspect_memberships` ON `contacts`.`id` = `aspect_memberships`.`contact_id`";
    doTest(appName, stmtId, expected);
  }

  @Test // 9
  void testDiaspora478() {
    // XXX DISTINCT
    final String appName = "diaspora";
    final int stmtId = 478;
    final String[] expected = {
      "SELECT `profiles`.`last_name` AS `alias_0`, `contacts`.`id` AS `id` FROM `contacts` AS `contacts` INNER JOIN `profiles` AS `profiles` ON `contacts`.`person_id` = `profiles`.`person_id` WHERE `contacts`.`user_id` = 3 AND `contacts`.`receiving` = TRUE ORDER BY `alias_0` ASC LIMIT 25 OFFSET 0",
      "SELECT `profiles`.`last_name` AS `alias_0`, `contacts`.`id` AS `id` FROM `contacts` AS `contacts` INNER JOIN `profiles` AS `profiles` ON `contacts`.`person_id` = `profiles`.`person_id` WHERE `contacts`.`receiving` = TRUE AND `contacts`.`user_id` = 3 ORDER BY `alias_0` ASC LIMIT 25 OFFSET 0"
    };

    doTest(appName, stmtId, expected);
  }

  @Test // 10
  void testDiaspora492() {
    // XXX: DISTINCT
    final String appName = "diaspora";
    final int stmtId = 492;
    final String[] expected = {
      "SELECT COUNT(*) FROM `conversation_visibilities` AS `conversation_visibilities` WHERE `conversation_visibilities`.`person_id` = 2",
    };

    doTest(appName, stmtId, expected);
  }

  @Test // 11
  void testDiscourse123() {
    final String appName = "discourse";
    final int stmtId = 123;
    final String expected =
        "SELECT `category_groups`.`category_id` AS `category_id` FROM `category_groups` AS `category_groups` INNER JOIN `group_users` AS `group_users` ON `category_groups`.`group_id` = `group_users`.`group_id` WHERE `group_users`.`user_id` = 86";
    doTest(appName, stmtId, expected);
  }

  @Test // 12
  void testDiscourse182() {
    final String appName = "discourse";
    final int stmtId = 182;
    final String expected =
        "SELECT `topic_allowed_groups`.`group_id` AS `group_id` FROM `topic_allowed_groups` AS `topic_allowed_groups` WHERE `topic_allowed_groups`.`topic_id` = 15596";
    doTest(appName, stmtId, expected);
  }

  @Test // 13
  void testDiscourse184() {
    final String appName = "discourse";
    final int stmtId = 184;
    final String expected =
        "SELECT `topic_tags`.`tag_id` AS `tag_id` FROM `topic_tags` AS `topic_tags` WHERE `topic_tags`.`topic_id` = 15596";
    doTest(appName, stmtId, expected);
  }

  // 14
  // TODO: discourse-207 too long

  @Test // 15
  void testDiscourse276() {
    final String appName = "discourse";
    final int stmtId = 276;
    final String expected =
        "SELECT `ignored_users`.`ignored_user_id` AS `ignored_user_id` FROM `ignored_users` AS `ignored_users` WHERE `ignored_users`.`user_id` = 155";
    doTest(appName, stmtId, expected);
  }

  @Test // 16
  void testDiscourse277() {
    final String appName = "discourse";
    final int stmtId = 277;
    final String expected =
        "SELECT `muted_users`.`muted_user_id` AS `muted_user_id` FROM `muted_users` AS `muted_users` WHERE `muted_users`.`user_id` = 155";
    doTest(appName, stmtId, expected);
  }

  @Test // 17
  void testDiscourse371() {
    final String appName = "discourse";
    final int stmtId = 371;
    final String expected =
        "SELECT `topic_allowed_users`.`user_id` AS `user_id` FROM `topic_allowed_users` AS `topic_allowed_users` WHERE `topic_allowed_users`.`topic_id` = 15632";
    doTest(appName, stmtId, expected);
  }

  @Test // 18
  void testDiscourse373() {
    final String appName = "discourse";
    final int stmtId = 373;
    final String expected =
        "SELECT `group_users`.`user_id` AS `user_id` FROM `group_users` AS `group_users` WHERE `group_users`.`group_id` = 2";
    doTest(appName, stmtId, expected);
  }

  @Test // 19
  void testDiscourse417() {
    final String appName = "discourse";
    final int stmtId = 417;
    final String[] expected =
        new String[] {
          "SELECT `category_users`.`user_id` AS `user_id` FROM `category_users` AS `category_users` WHERE `category_users`.`notification_level` = 4 AND `category_users`.`category_id` IS NULL",
          "SELECT `category_users`.`user_id` AS `user_id` FROM `category_users` AS `category_users` WHERE `category_users`.`category_id` IS NULL AND `category_users`.`notification_level` = 4"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 20
  void testDiscourse449() {
    final String appName = "discourse";
    final int stmtId = 449;
    final String[] expected =
        new String[] {
          "SELECT `child_themes`.`parent_theme_id` AS `parent_theme_id` FROM `child_themes` AS `child_themes` WHERE `child_themes`.`child_theme_id` = 1017",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 21
  void testDiscourse599() {
    final String appName = "discourse";
    final int stmtId = 599;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `category_tags` AS `category_tags` WHERE `category_tags`.`category_id` = 3121",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 22
  void testDiscourse600() {
    final String appName = "discourse";
    final int stmtId = 600;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `category_tag_groups` AS `category_tag_groups` WHERE `category_tag_groups`.`category_id` = 3121",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 23
  void testDiscourse624() {
    final String appName = "discourse";
    final int stmtId = 624;
    final String[] expected =
        new String[] {
          "SELECT `group_users`.`group_id` AS `group_id` FROM `group_users` AS `group_users` WHERE `group_users`.`user_id` = 247",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 24
  void testDiscourse660() {
    final String appName = "discourse";
    final int stmtId = 660;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `category_groups` AS `category_groups` WHERE `category_groups`.`group_id` = 2378",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 25
  void testDiscourse833() {
    final String appName = "discourse";
    final int stmtId = 833;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `group_users` AS `group_users` WHERE `group_users`.`group_id` = 2397",
        };
    doTest(appName, stmtId, expected);
  }

  // 26
  // TODO: discourse-877 too long

  @Test // 27
  void testDiscourse942() {
    final String appName = "discourse";
    final int stmtId = 942;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `group_users` AS `gu` WHERE `gu`.`user_id` = 779 AND (`gu`.`owner` IS TRUE AND `gu`.`group_id` > 0)",
          "SELECT COUNT(*) FROM `group_users` AS `gu` WHERE `gu`.`owner` IS TRUE AND (`gu`.`group_id` > 0 AND `gu`.`user_id` = 779)",
          "SELECT COUNT(*) FROM `group_users` AS `gu` WHERE `gu`.`group_id` > 0 AND (`gu`.`user_id` = 779 AND `gu`.`owner` IS TRUE)",
          "SELECT COUNT(*) FROM `group_users` AS `gu` WHERE `gu`.`user_id` = 779 AND (`gu`.`group_id` > 0 AND `gu`.`owner` IS TRUE)"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 28
  void testDiscourse944() {
    // XXX: ORDER BY
    final String appName = "discourse";
    final int stmtId = 944;
    final String[] expected =
        new String[] {
          "SELECT `gu`.`group_id` AS `group_id` FROM `group_users` AS `group_users` INNER JOIN `group_users` AS `gu` ON `group_users`.`group_id` = `gu`.`group_id` WHERE `gu`.`user_id` = 779 AND (`gu`.`group_id` > 0 AND `group_users`.`user_id` = 779)",
        };
    doTest(appName, stmtId, expected);
  }

  //  @Test // 29
  void testDiscourse945() {
    // XXX: Wrongly remove filter
    final String appName = "discourse";
    final int stmtId = 945;
    final String[] expected =
        new String[] {
          "SELECT `gu`.`group_id` AS `group_id` FROM `group_users` AS `gu` WHERE `gu`.`group_id` > 0 AND `gu`.`user_id` = 779",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 30
  void testDiscourse946() {
    final String appName = "discourse";
    final int stmtId = 946;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `group_users` AS `gu` WHERE `gu`.`user_id` = 779 AND `gu`.`group_id` > 0",
          "SELECT COUNT(*) FROM `group_users` AS `gu` WHERE `gu`.`group_id` > 0 AND `gu`.`user_id` = 779"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 31
  void testDiscourse948() {
    final String appName = "discourse";
    final int stmtId = 948;
    final String[] expected =
        new String[] {
          "SELECT `groups`.`id` AS `id` FROM `group_users` AS `group_users` INNER JOIN `groups` AS `groups` ON `group_users`.`group_id` = `groups`.`id` WHERE `group_users`.`user_id` = 779 AND (`groups`.`id` > 0 AND `groups`.`automatic` = TRUE)",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 31
  void testDiscourse949() {
    final String appName = "discourse";
    final int stmtId = 949;
    final String[] expected =
        new String[] {
          "SELECT `group_users`.`group_id` AS `group_id` FROM `group_users` AS `group_users` INNER JOIN `groups` AS `groups` ON `group_users`.`group_id` = `groups`.`id` WHERE `group_users`.`user_id` = 779 AND (`group_users`.`owner` = TRUE AND (`groups`.`id` > 0 AND `groups`.`automatic` = TRUE))",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 33
  void testDiscourse994() {
    final String appName = "discourse";
    final int stmtId = 994;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `group_users` AS `gu` WHERE `gu`.`group_id` > 0 AND `gu`.`user_id` = 780",
          "SELECT COUNT(*) FROM `group_users` AS `gu` WHERE `gu`.`user_id` = 780 AND `gu`.`group_id` > 0"
        };
    doTest(appName, stmtId, expected);
  }

  // 34
  // TODO: discourse-1000 too long

  @Test // 35
  void testDiscourse1003() {
    final String appName = "discourse";
    final int stmtId = 1003;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `group_requests` AS `group_requests` WHERE `group_requests`.`group_id` = 2563",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 36
  void testDiscourse1006() {
    final String appName = "discourse";
    final int stmtId = 1006;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `group_users` AS `group_users` WHERE `group_users`.`user_id` > 0 AND `group_users`.`group_id` = 2564",
          "SELECT COUNT(*) FROM `group_users` AS `group_users` WHERE `group_users`.`group_id` = 2564 AND `group_users`.`user_id` > 0"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 37
  void testDiscourse1048() {
    final String appName = "discourse";
    final int stmtId = 1048;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `group_histories` AS `group_histories` WHERE `group_histories`.`group_id` = 2576 LIMIT 25 OFFSET 0) AS `subquery_for_count`"
        };
    doTest(appName, stmtId, expected);
  }

  // 38
  // TODO: discourse-1173 too long
  // 39
  // TODO: discourse-1174 too long
  // 40
  // TODO: discourse-1178 too long
  // 41
  // TODO: discourse-1179 too long
  // 42
  // TODO: discourse-1181 too long
  // 43
  // TODO: discourse-1182 too long
  // 44
  // TODO: discourse-1183 too long
  // 45
  // TODO: discourse-1186 too long
  // 46
  // TODO: discourse-1191 too long
  // 47
  // TODO: discourse-1196 too long
  // 48
  // TODO: discourse-1200 too long
  // 49
  // TODO: discourse-1213 too long
  // 50
  // TODO: discourse-1214 too long
  // 51
  // TODO: discourse-1216 too long

  @Test // 52
  void testDiscourse1291() {
    final String appName = "discourse";
    final int stmtId = 1291;
    final String[] expected =
        new String[] {
          "SELECT `child_themes`.`parent_theme_id` AS `parent_theme_id` FROM `child_themes` AS `child_themes` WHERE `child_themes`.`child_theme_id` IN (?)",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 53
  void testDiscourse1361() {
    final String appName = "discourse";
    final int stmtId = 1361;
    final String[] expected =
        new String[] {
          "SELECT `tags`.`name` AS `name` FROM `tags` AS `tags` INNER JOIN `tag_group_memberships` AS `tag_group_memberships` ON `tags`.`id` = `tag_group_memberships`.`tag_id` INNER JOIN `tag_group_permissions` AS `tag_group_permissions` ON `tag_group_memberships`.`tag_group_id` = `tag_group_permissions`.`tag_group_id` WHERE `tag_group_permissions`.`group_id` = 0 AND `tag_group_permissions`.`permission_type` = 3",
          "SELECT `tags`.`name` AS `name` FROM `tags` AS `tags` INNER JOIN `tag_group_memberships` AS `tag_group_memberships` ON `tags`.`id` = `tag_group_memberships`.`tag_id` INNER JOIN `tag_group_permissions` AS `tag_group_permissions` ON `tag_group_memberships`.`tag_group_id` = `tag_group_permissions`.`tag_group_id` WHERE `tag_group_permissions`.`permission_type` = 3 AND `tag_group_permissions`.`group_id` = 0"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 54
  void testDiscourse1426() {
    final String appName = "discourse";
    final int stmtId = 1426;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `topic_allowed_users` AS `topic_allowed_users` WHERE `topic_allowed_users`.`topic_id` = 16056",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 55
  void testDiscourse1473() {
    final String appName = "discourse";
    final int stmtId = 1473;
    final String[] expected =
        new String[] {
          "SELECT `tag_group_memberships`.`tag_id` AS `tag_id` FROM `tag_group_memberships` AS `tag_group_memberships` WHERE `tag_group_memberships`.`tag_group_id` = 453",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 56
  void testDiscourse1957() {
    final String appName = "discourse";
    final int stmtId = 1957;
    final String[] expected =
        new String[] {
          "SELECT `user_emails`.`email` AS `email` FROM `user_emails` AS `user_emails` INNER JOIN `topic_allowed_users` AS `topic_allowed_users` ON `user_emails`.`user_id` = `topic_allowed_users`.`user_id` WHERE `topic_allowed_users`.`topic_id` = 16471",
        };
    doTest(appName, stmtId, expected);
  }

  // 57
  // TODO: discourse-2012 too long
  // 58
  // TODO: discourse-2016 too long
  // 59
  // TODO: discourse-2019 too long

  @Test // 60
  void testDiscourse2291() {
    final String appName = "discourse";
    final int stmtId = 2291;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `post_uploads` AS `post_uploads`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 61
  void testDiscourse2407() {
    final String appName = "discourse";
    final int stmtId = 2407;
    final String[] expected =
        new String[] {
          "SELECT `child_themes`.`parent_theme_id` AS `parent_theme_id` FROM `child_themes` AS `child_themes`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 62
  void testDiscourse2783() {
    final String appName = "discourse";
    final int stmtId = 2783;
    final String[] expected =
        new String[] {
          "SELECT `topic_allowed_groups`.`group_id` AS `group_id` FROM `topic_allowed_groups` AS `topic_allowed_groups` WHERE `topic_allowed_groups`.`topic_id` = 17701 AND `topic_allowed_groups`.`group_id` IN (?)",
          "SELECT `topic_allowed_groups`.`group_id` AS `group_id` FROM `topic_allowed_groups` AS `topic_allowed_groups` WHERE `topic_allowed_groups`.`group_id` IN (?) AND `topic_allowed_groups`.`topic_id` = 17701",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 63
  void testDiscourse2832() {
    final String appName = "discourse";
    final int stmtId = 2832;
    final String[] expected =
        new String[] {
          "SELECT `categories`.`name` AS `name` FROM `categories` AS `categories` INNER JOIN `category_tag_groups` AS `category_tag_groups` ON `categories`.`id` = `category_tag_groups`.`category_id` INNER JOIN `tag_group_memberships` AS `tag_group_memberships` ON `category_tag_groups`.`tag_group_id` = `tag_group_memberships`.`tag_group_id` WHERE `tag_group_memberships`.`tag_id` = 1771 AND `categories`.`id` IN (?)",
          "SELECT `categories`.`name` AS `name` FROM `categories` AS `categories` INNER JOIN `category_tag_groups` AS `category_tag_groups` ON `categories`.`id` = `category_tag_groups`.`category_id` INNER JOIN `tag_group_memberships` AS `tag_group_memberships` ON `category_tag_groups`.`tag_group_id` = `tag_group_memberships`.`tag_group_id` WHERE `categories`.`id` IN (?) AND `tag_group_memberships`.`tag_id` = 1771",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 64
  void testDiscourse2839() {
    final String appName = "discourse";
    final int stmtId = 2839;
    final String[] expected =
        new String[] {
          "SELECT `category_tags`.`category_id` AS `category_id` FROM `category_tags` AS `category_tags` WHERE `category_tags`.`tag_id` = 1775",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 65
  void testDiscourse3638() {
    final String appName = "discourse";
    final int stmtId = 3638;
    final String[] expected =
        new String[] {
          "SELECT `groups_web_hooks`.`group_id` AS `group_id` FROM `groups_web_hooks` AS `groups_web_hooks` WHERE `groups_web_hooks`.`web_hook_id` = 182",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 66
  void testDiscourse3639() {
    final String appName = "discourse";
    final int stmtId = 3639;
    final String[] expected =
        new String[] {
          "SELECT `categories_web_hooks`.`category_id` AS `category_id` FROM `categories_web_hooks` AS `categories_web_hooks` WHERE `categories_web_hooks`.`web_hook_id` = 182",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 67
  void testDiscourse3640() {
    final String appName = "discourse";
    final int stmtId = 3640;
    final String[] expected =
        new String[] {
          "SELECT `tags_web_hooks`.`tag_id` AS `tag_id` FROM `tags_web_hooks` AS `tags_web_hooks` WHERE `tags_web_hooks`.`web_hook_id` = 182",
        };
    doTest(appName, stmtId, expected);
  }

  // 68
  // TODO: discourse-3690 too long
  // 69
  // TODO: discourse-3691 too long
  // 70
  // TODO: discourse-3825 too long
  // 71
  // TODO: discourse-3829 cross join support (no ON-condition)
  // 72
  // TODO: discourse-3831 cross join support (no ON-condition)
  // 73
  // TODO: discourse-3842 cross join support (no ON-condition)

  @Test // 74
  void testDiscourse4071() {
    final String appName = "discourse";
    final int stmtId = 4071;
    final String[] expected =
        new String[] {
          "SELECT `topic_allowed_users`.`user_id` AS `user_id` FROM `topic_allowed_users` AS `topic_allowed_users` WHERE `topic_allowed_users`.`topic_id` = 18844",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 75
  void testDiscourse4156() {
    final String appName = "discourse";
    final int stmtId = 4156;
    final String[] expected =
        new String[] {
          "SELECT `posts`.`topic_id` AS `topic_id` FROM `posts` AS `posts` INNER JOIN `users` AS `users` ON `posts`.`user_id` = `users`.`id` LEFT JOIN `topics` AS `topics` ON `topics`.`id` = `posts`.`topic_id` AND NOT `topics`.`deleted_at` IS NULL WHERE `posts`.`user_id` = 7304 ORDER BY `posts`.`created_at` DESC",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 76
  void testDiscourse5044() {
    final String appName = "discourse";
    final int stmtId = 5044;
    final String[] expected =
        new String[] {
          "SELECT `category_search_data`.`category_id` AS `category_id` FROM `category_search_data` AS `category_search_data` WHERE `category_search_data`.`locale` <> 'fr' OR `category_search_data`.`version` <> 3 ORDER BY `category_id` ASC LIMIT 500",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 77
  void testEladmin104() {
    final String appName = "eladmin";
    final int stmtId = 104;
    final String[] expected =
        new String[] {
          "SELECT `job0_`.`id` AS `id1_6_`, `job0_`.`create_time` AS `create_t2_6_`, `job0_`.`dept_id` AS `dept_id6_6_`, `job0_`.`enabled` AS `enabled3_6_`, `job0_`.`name` AS `name4_6_`, `job0_`.`sort` AS `sort5_6_` FROM `job` AS `job0_` ORDER BY `sort5_6_` ASC LIMIT 10",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 78
  void testEladmin105() {
    final String appName = "eladmin";
    final int stmtId = 105;
    final String[] expected =
        new String[] {
          "SELECT `job0_`.`id` AS `id1_6_`, `job0_`.`create_time` AS `create_t2_6_`, `job0_`.`dept_id` AS `dept_id6_6_`, `job0_`.`enabled` AS `enabled3_6_`, `job0_`.`name` AS `name4_6_`, `job0_`.`sort` AS `sort5_6_` FROM `job` AS `job0_` WHERE `job0_`.`name` LIKE '%d%' ORDER BY `sort5_6_` ASC LIMIT 10",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 79
  void testFatfreecrm9() {
    final String appName = "fatfreecrm";
    final int stmtId = 9;
    final String[] expected =
        new String[] {
          "SELECT `taggings`.`tag_id` AS `tag_id` FROM `taggings` AS `taggings` WHERE `taggings`.`taggable_type` = 'Contact' AND (`taggings`.`context` = 'tags' AND `taggings`.`taggable_id` = 1234)",
          "SELECT `taggings`.`tag_id` AS `tag_id` FROM `taggings` AS `taggings` WHERE `taggings`.`context` = 'tags' AND (`taggings`.`taggable_id` = 1234 AND `taggings`.`taggable_type` = 'Contact')",
          "SELECT `taggings`.`tag_id` AS `tag_id` FROM `taggings` AS `taggings` WHERE `taggings`.`taggable_id` = 1234 AND (`taggings`.`taggable_type` = 'Contact' AND `taggings`.`context` = 'tags')"
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 80
  void testFatfreecrm16() {
    final String appName = "fatfreecrm";
    final int stmtId = 16;
    final String[] expected =
        new String[] {
          "SELECT `groups_users`.`group_id` AS `group_id` FROM `groups_users` AS `groups_users` WHERE `groups_users`.`user_id` = 3056",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 81
  void testFatfreecrm19() {
    final String appName = "fatfreecrm";
    final int stmtId = 19;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `accounts` AS `accounts` WHERE `accounts`.`assigned_to` = 237 OR (`accounts`.`user_id` = 237 OR `accounts`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 82
  void testFatfreecrm26() {
    final String appName = "fatfreecrm";
    final int stmtId = 26;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `contacts` AS `contacts` WHERE `contacts`.`assigned_to` = 688 OR (`contacts`.`user_id` = 688 OR `contacts`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 83
  void testFatfreecrm32() {
    final String appName = "fatfreecrm";
    final int stmtId = 32;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `accounts` AS `accounts` WHERE (`accounts`.`assigned_to` = 238 OR (`accounts`.`user_id` = 238 OR `accounts`.`access` = 'Public')) AND `accounts`.`category` IN (?)",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 84
  void testFatfreecrm33() {
    final String appName = "fatfreecrm";
    final int stmtId = 33;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `leads` AS `leads` WHERE (`leads`.`assigned_to` = 1131 OR (`leads`.`user_id` = 1131 OR `leads`.`access` = 'Public')) AND `leads`.`status` IN (?)",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 85
  void testFatfreecrm80() {
    final String appName = "fatfreecrm";
    final int stmtId = 80;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `opportunities` AS `opportunities` WHERE `opportunities`.`assigned_to` = 1496 OR (`opportunities`.`user_id` = 1496 OR `opportunities`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 86
  void testFatfreecrm87() {
    final String appName = "fatfreecrm";
    final int stmtId = 87;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `leads` AS `leads` WHERE `leads`.`assigned_to` = 980 OR (`leads`.`user_id` = 980 OR `leads`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 87
  void testFatfreecrm94() {
    final String appName = "fatfreecrm";
    final int stmtId = 94;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `accounts` AS `accounts` WHERE (`accounts`.`assigned_to` = 239 OR (`accounts`.`user_id` = 239 OR `accounts`.`access` = 'Public')) AND (`accounts`.`name` LIKE '%second%' OR `accounts`.`email` LIKE '%second%')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 88
  void testFatfreecrm96() {
    final String appName = "fatfreecrm";
    final int stmtId = 96;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `accounts` AS `accounts` WHERE `accounts`.`assigned_to` = 358 OR (`accounts`.`user_id` = 358 OR `accounts`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 89
  void testFatfreecrm112() {
    final String appName = "fatfreecrm";
    final int stmtId = 112;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `campaigns` AS `campaigns` WHERE `campaigns`.`assigned_to` = 410 OR (`campaigns`.`user_id` = 410 OR `campaigns`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 90
  void testFatfreecrm147() {
    final String appName = "fatfreecrm";
    final int stmtId = 147;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `opportunities` AS `opportunities` WHERE `opportunities`.`assigned_to` = 1359 OR (`opportunities`.`user_id` = 1359 OR `opportunities`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 91
  void testFatfreecrm149() {
    final String appName = "fatfreecrm";
    final int stmtId = 149;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `opportunities` AS `opportunities` WHERE `opportunities`.`assigned_to` = 1359 OR (`opportunities`.`user_id` = 1359 OR `opportunities`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 92
  void testFatfreecrm152() {
    final String appName = "fatfreecrm";
    final int stmtId = 152;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `campaigns` AS `campaigns` WHERE (`campaigns`.`assigned_to` = 363 OR (`campaigns`.`user_id` = 363 OR `campaigns`.`access` = 'Public')) AND `campaigns`.`status` IN (?)",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 93
  void testFatfreecrm154() {
    final String appName = "fatfreecrm";
    final int stmtId = 154;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `contacts` AS `contacts` WHERE (`contacts`.`assigned_to` = 547 OR (`contacts`.`user_id` = 547 OR `contacts`.`access` = 'Public')) AND (`contacts`.`first_name` LIKE '%page_sanford@kuvalisraynor.biz%' OR `contacts`.`last_name` LIKE '%page_sanford@kuvalisraynor.biz%' OR (`contacts`.`email` LIKE '%page_sanford@kuvalisraynor.biz%' OR `contacts`.`alt_email` LIKE '%page_sanford@kuvalisraynor.biz%' OR `contacts`.`phone` LIKE '%page_sanford@kuvalisraynor.biz%' OR `contacts`.`mobile` LIKE '%page_sanford@kuvalisraynor.biz%'))",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 94
  void testFatfreecrm175() {
    final String appName = "fatfreecrm";
    final int stmtId = 175;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `leads` AS `leads` WHERE `leads`.`assigned_to` = 1124 OR (`leads`.`user_id` = 1124 OR `leads`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 95
  void testFatfreecrm176() {
    final String appName = "fatfreecrm";
    final int stmtId = 176;
    final String[] expected =
        new String[] {
          "SELECT 1 AS `one` FROM `taggings` AS `taggings` WHERE `taggings`.`taggable_id` = 511 AND (`taggings`.`taggable_type` = 'Lead' AND `taggings`.`context` = 'tags') LIMIT 1",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 96
  void testFatfreecrm178() {
    final String appName = "fatfreecrm";
    final int stmtId = 178;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `opportunities` AS `opportunities` WHERE (`opportunities`.`assigned_to` = 1503 OR (`opportunities`.`user_id` = 1503 OR `opportunities`.`access` = 'Public')) AND `opportunities`.`stage` IN (?)",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 97
  void testFatfreecrm181() {
    final String appName = "fatfreecrm";
    final int stmtId = 181;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `contacts` AS `contacts` WHERE `contacts`.`assigned_to` = 865 OR (`contacts`.`user_id` = 865 OR `contacts`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 98
  void testFatfreecrm187() {
    final String appName = "fatfreecrm";
    final int stmtId = 187;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `campaigns` AS `campaigns` WHERE `campaigns`.`assigned_to` = 541 OR (`campaigns`.`user_id` = 541 OR `campaigns`.`access` = 'Public')",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 99
  void testFatfreecrm190() {
    final String appName = "fatfreecrm";
    final int stmtId = 190;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `opportunities` AS `opportunities` WHERE (`opportunities`.`assigned_to` = 1143 OR (`opportunities`.`user_id` = 1143 OR `opportunities`.`access` = 'Public')) AND `opportunities`.`name` LIKE '%second%'",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 100
  void testFatfreecrm192() {
    final String appName = "fatfreecrm";
    final int stmtId = 192;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `leads` AS `leads` WHERE (`leads`.`assigned_to` = 879 OR (`leads`.`user_id` = 879 OR `leads`.`access` = 'Public')) AND (`leads`.`first_name` LIKE '%bill%' OR `leads`.`last_name` LIKE '%bill%' OR `leads`.`company` LIKE '%bill%' OR `leads`.`email` LIKE '%bill%')",
        };
    doTest(appName, stmtId, expected);
  }

  // 101
  // TODO: febs-52 aggregate
  // 102
  // TODO: febs-94 aggregate
  // 103
  // TODO: febs-96 aggregate
  // 104
  // TODO: febs-98 aggregate
  // 105
  // TODO: febs-101 aggregate

  @Test // 106
  void testHomeland12() {
    final String appName = "homeland";
    final int stmtId = 12;
    final String[] expected =
        new String[] {
          "SELECT `actions`.`user_id` AS `user_id` FROM `actions` AS `actions` WHERE `actions`.`target_id` = ? AND (`actions`.`action_type` = ? AND (`actions`.`target_type` = ? AND (`actions`.`user_type` = ? AND `actions`.`user_type` = ?)))",
        };
    doTest(appName, stmtId, expected);
  }

  // 107
  @Test // 106
  void testHomeland31() {
    final String appName = "homeland";
    final int stmtId = 31;
    final String[] expected =
        new String[] {
          "SELECT COUNT(`subquery_for_count`.`count_column`) FROM (SELECT `users`.`type`, `users`.`id`, `users`.`name`, `users`.`login`, `users`.`email`, `users`.`email_md5`, `users`.`email_public`, `users`.`avatar`, `users`.`state`, `users`.`tagline`, `users`.`github`, `users`.`website`, `users`.`location`, `users`.`location_id`, `users`.`twitter`, `users`.`team_users_count`, `users`.`created_at`, `users`.`updated_at` AS `count_column` FROM `users` AS `users` WHERE `users`.`location_id` = 1 AND NOT `users`.`type` IS NULL LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 108
  void testHomeland72() {
    final String appName = "homeland";
    final int stmtId = 72;
    final String[] expected =
        new String[] {
          "SELECT `actions`.`target_id` AS `target_id` FROM `actions` AS `actions` WHERE `actions`.`user_id` = ? AND (`actions`.`action_type` = ? AND (`actions`.`target_type` = ? AND (`actions`.`user_type` = ? AND `actions`.`target_type` = ?)))",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 109
  void testHomeland73() {
    final String appName = "homeland";
    final int stmtId = 73;
    final String[] expected =
        new String[] {
          "SELECT `actions`.`target_id` AS `target_id` FROM `actions` AS `actions` WHERE `actions`.`user_id` = ? AND (`actions`.`action_type` = ? AND (`actions`.`target_type` = ? AND (`actions`.`user_type` = ? AND `actions`.`target_type` = ?)))",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 110
  void testLobsters93() {
    final String appName = "lobsters";
    final int stmtId = 93;
    final String[] expected =
        new String[] {
          "SELECT COUNT(DISTINCT `comments`.`id`) FROM `comments` AS `comments` WHERE `comments`.`is_deleted` = FALSE AND (`comments`.`is_moderated` = FALSE AND `comments`.`story_id` = 67)",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 111
  void testLobsters95() {
    final String appName = "lobsters";
    final int stmtId = 95;
    final String[] expected =
        new String[] {
          "SELECT COUNT(DISTINCT `comments`.`id`) FROM `comments` AS `comments` INNER JOIN `stories` AS `stories` ON `comments`.`story_id` = `stories`.`id` INNER JOIN `domains` AS `domains` ON `stories`.`domain_id` = `domains`.`id` WHERE `comments`.`is_deleted` = FALSE AND (`comments`.`is_moderated` = FALSE AND (`domains`.`domain` = 'lobste.rs' AND MATCH `comments`.`comment` AGAINST ('comment3 comment4' IN BOOLEAN MODE)))",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 112
  void testLobsters114() {
    final String appName = "lobsters";
    final int stmtId = 114;
    final String[] expected =
        new String[] {
          "SELECT COUNT(DISTINCT `comments`.`id`) FROM `comments` AS `comments` WHERE `comments`.`is_deleted` = FALSE AND (`comments`.`is_moderated` = FALSE AND MATCH `comments`.`comment` AGAINST ('comment1' IN BOOLEAN MODE))",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 113
  void testLobsters129() {
    final String appName = "lobsters";
    final int stmtId = 129;
    final String[] expected =
        new String[] {
          "SELECT COUNT(DISTINCT `stories`.`id`) FROM `stories` AS `stories` WHERE `stories`.`merged_story_id` IS NULL AND (`stories`.`is_expired` = FALSE AND (MATCH `stories`.`title` AGAINST ('unique' IN BOOLEAN MODE) OR MATCH `stories`.`description` AGAINST ('unique' IN BOOLEAN MODE) OR MATCH `stories`.`story_cache` AGAINST ('unique' IN BOOLEAN MODE)))",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 114
  void testPybbs31() {
    final String appName = "pybbs";
    final int stmtId = 31;
    final String[] expected =
        new String[] {
          "SELECT COUNT(1) FROM `topic` AS `t`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 115
  void testPybbs33() {
    final String appName = "pybbs";
    final int stmtId = 33;
    final String[] expected =
        new String[] {
          "SELECT COUNT(1) FROM `topic` AS `t` WHERE `t`.`in_time` BETWEEN '2019-10-11' AND '2019-10-26'",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 116
  void testPybbs51() {
    final String appName = "pybbs";
    final int stmtId = 51;
    final String[] expected =
        new String[] {
          "SELECT COUNT(1) FROM `comment` AS `c`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 117
  void testPybbs53() {
    final String appName = "pybbs";
    final int stmtId = 53;
    final String[] expected =
        new String[] {
          "SELECT COUNT(1) FROM `comment` AS `c` WHERE `c`.`in_time` BETWEEN '2019-10-01' AND '2019-10-31'",
        };
    doTest(appName, stmtId, expected);
  }

  // 118
  // TODO: redmine-136 DISTINCT

  @Test // 119
  void testRedmine284() {
    final String appName = "redmine";
    final int stmtId = 284;
    final String[] expected =
        new String[] {
          "SELECT 1 AS `one` FROM `projects_trackers` AS `projects_trackers` WHERE `projects_trackers`.`project_id` = 1 LIMIT 1",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 120
  void testRedmine341() {
    final String appName = "redmine";
    final int stmtId = 341;
    final String[] expected =
        new String[] {
          "SELECT `changesets_issues`.`issue_id` AS `issue_id` FROM `changesets_issues` AS `changesets_issues` WHERE `changesets_issues`.`changeset_id` = 339",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 121
  void testRedmine535() {
    final String appName = "redmine";
    final int stmtId = 535;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `watchers` AS `watchers` WHERE `watchers`.`watchable_id` = 1 AND `watchers`.`watchable_type` = 'Issue'",
        };
    doTest(appName, stmtId, expected);
  }

  // 122
  // TODO: shopizer-1 DISTINCT
  // 123
  // TODO: shopizer-3 DISTINCT
  // 124
  // TODO: shopizer-14 DISTINCT
  // 125
  // TODO: shopizer-18 DISTINCT
  // 126
  // TODO: shopizer-24 DISTINCT
  // 127
  // TODO: shopizer-31 DISTINCT
  // 128
  // TODO: shopizer-39 DISTINCT
  // 129
  // TODO: shopizer-40 DISTINCT
  // 130
  // TODO: shopizer-46 DISTINCT
  // 131
  // TODO: shopizer-57 DISTINCT
  // 132
  // TODO: shopizer-67 too long
  // 133
  // TODO: shopizer-68 too long
  // 134
  // TODO: shopizer-119 too long
  // 135
  // TODO: solidus-126 too long

  @Test // 136
  void testSolidus230() {
    final String appName = "solidus";
    final int stmtId = 230;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxonomies` AS `spree_taxonomies` WHERE `spree_taxonomies`.`name` LIKE '%style%' LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 137
  void testSolidus273() {
    final String appName = "solidus";
    final int stmtId = 273;
    final String[] expected =
        new String[] {
          "SELECT `spree_products_taxons`.`taxon_id` AS `taxon_id` FROM `spree_products_taxons` AS `spree_products_taxons` WHERE `spree_products_taxons`.`product_id` = 507",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 138
  void testSolidus277() {
    final String appName = "solidus";
    final int stmtId = 277;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxonomies` AS `spree_taxonomies` LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 139
  void testSolidus279() {
    final String appName = "solidus";
    final int stmtId = 279;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxonomies` AS `spree_taxonomies` LIMIT 1 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 140
  void testSolidus312() {
    final String appName = "solidus";
    final int stmtId = 312;
    final String[] expected =
        new String[] {
          "SELECT `spree_option_values_variants`.`option_value_id` AS `option_value_id` FROM `spree_option_values_variants` AS `spree_option_values_variants` WHERE `spree_option_values_variants`.`variant_id` = 1477",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 141
  void testSolidus409() {
    final String appName = "solidus";
    final int stmtId = 409;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_payments` AS `spree_payments` WHERE `spree_payments`.`order_id` = 183 LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 142
  void testSolidus430() {
    final String appName = "solidus";
    final int stmtId = 430;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_stock_locations` AS `spree_stock_locations` WHERE `spree_stock_locations`.`active` = TRUE LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 143
  void testSolidus434() {
    final String appName = "solidus";
    final int stmtId = 434;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_stock_locations` AS `spree_stock_locations` LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 144
  void testSolidus489() {
    final String appName = "solidus";
    final int stmtId = 489;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_product_properties` AS `spree_product_properties` WHERE `spree_product_properties`.`product_id` = 429 LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 145
  void testSolidus523() {
    final String appName = "solidus";
    final int stmtId = 523;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_promotion_codes` AS `spree_promotion_codes` WHERE `spree_promotion_codes`.`promotion_id` = 114 LIMIT 50 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  // 146
  // TODO: solidus-551 too long

  @Test // 147
  void testSolidus649() {
    final String appName = "solidus";
    final int stmtId = 649;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_product_properties` AS `spree_product_properties` WHERE `spree_product_properties`.`product_id` = 426 LIMIT 1 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 148
  void testSolidus652() {
    final String appName = "solidus";
    final int stmtId = 652;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_payments` AS `spree_payments` WHERE `spree_payments`.`order_id` = 180 LIMIT 1 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 149
  void testSolidus656() {
    final String appName = "solidus";
    final int stmtId = 656;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_stock_locations` AS `spree_stock_locations` WHERE `spree_stock_locations`.`name` LIKE '%south%' LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 150
  void testSolidus658() {
    final String appName = "solidus";
    final int stmtId = 658;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_stock_locations` AS `spree_stock_locations` LIMIT 1 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 151
  void testSolidus681() {
    final String appName = "solidus";
    final int stmtId = 681;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_states` AS `spree_states` LIMIT 1 OFFSET 1) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  // 152
  // TODO: solidus-744 distinct
  // 153
  // TODO: solidus-753 distinct

  @Test // 154
  void testSolidus761() {
    final String appName = "solidus";
    final int stmtId = 761;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_zones` AS `spree_zones` WHERE `spree_zones`.`name` LIKE '%south%' LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 155
  void testSolidus765() {
    final String appName = "solidus";
    final int stmtId = 765;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_zones` AS `spree_zones` LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 156
  void testSolidus767() {
    final String appName = "solidus";
    final int stmtId = 767;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_zones` AS `spree_zones` LIMIT 1 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 157
  void testSolidus794() {
    final String appName = "solidus";
    final int stmtId = 794;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_countries` AS `spree_countries` LIMIT 1 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 158
  void testSolidus797() {
    final String appName = "solidus";
    final int stmtId = 797;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_countries` AS `spree_countries` LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 159
  void testSolidus799() {
    final String appName = "solidus";
    final int stmtId = 799;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_countries` AS `spree_countries` WHERE `spree_countries`.`name` LIKE '%zam%' LIMIT 25 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 160
  void testSolidus818() {
    final String appName = "solidus";
    final int stmtId = 818;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `spree_roles_users` AS `spree_roles_users` WHERE `spree_roles_users`.`user_id` = 2401 AND `spree_roles_users`.`role_id` = 27",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 161
  void testSolidus835() {
    final String appName = "solidus";
    final int stmtId = 835;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxons` AS `spree_taxons` WHERE `spree_taxons`.`parent_id` = 99 LIMIT 500 OFFSET 0) AS `subquery_for_count`",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 162
  void testSolidus837() {
    final String app = "solidus";
    final int stmtId = 837;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxons` AS `spree_taxons` WHERE `spree_taxons`.`parent_id` = 78 LIMIT 1 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  // 163
  // TODO spree-109 DISTINCT
  // 164
  // TODO spree-125 DISTINCT
  // 165
  // TODO spree-285 subquery to join

  @Test // 166
  void testSpree333() {
    final String app = "spree";
    final int stmtId = 333;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_orders` AS `spree_orders` WHERE `spree_orders`.`user_id` = 3 LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 167
  void testSpree393() {
    final String app = "spree";
    final int stmtId = 393;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT `spree_products`.`id` AS `id`, `spree_products`.`name` AS `name`, `spree_products`.`description` AS `description`, `spree_products`.`available_on` AS `available_on`, `spree_products`.`discontinue_on` AS `discontinue_on`, `spree_products`.`deleted_at` AS `deleted_at`, `spree_products`.`slug` AS `slug`, `spree_products`.`meta_description` AS `meta_description`, `spree_products`.`meta_keywords` AS `meta_keywords`, `spree_products`.`tax_category_id` AS `tax_category_id`, `spree_products`.`shipping_category_id` AS `shipping_category_id`, `spree_products`.`created_at` AS `created_at`, `spree_products`.`updated_at` AS `updated_at`, `spree_products`.`promotionable` AS `promotionable`, `spree_products`.`meta_title` AS `meta_title` FROM `spree_products` AS `spree_products` INNER JOIN `spree_variants` AS `spree_variants` ON `spree_variants`.`deleted_at` IS NULL AND `spree_variants`.`product_id` = `spree_products`.`id` AND `spree_variants`.`is_master` = TRUE INNER JOIN `spree_prices` AS `spree_prices` ON `spree_prices`.`deleted_at` IS NULL AND `spree_prices`.`variant_id` = `spree_variants`.`id` WHERE `spree_products`.`deleted_at` IS NULL AND ((`spree_products`.`deleted_at` IS NULL OR `spree_products`.`deleted_at` >= '2020-05-01 07:05:53.713734') AND ((`spree_products`.`discontinue_on` IS NULL OR `spree_products`.`discontinue_on` >= '2020-05-01 07:05:53.714089') AND `spree_products`.`available_on` <= '2020-05-01 07:05:53.714072')) LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 168
  void testSpree396() {
    final String app = "spree";
    final int stmtId = 396;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT `spree_products`.`id` AS `id`, `spree_products`.`name` AS `name`, `spree_products`.`description` AS `description`, `spree_products`.`available_on` AS `available_on`, `spree_products`.`discontinue_on` AS `discontinue_on`, `spree_products`.`deleted_at` AS `deleted_at`, `spree_products`.`slug` AS `slug`, `spree_products`.`meta_description` AS `meta_description`, `spree_products`.`meta_keywords` AS `meta_keywords`, `spree_products`.`tax_category_id` AS `tax_category_id`, `spree_products`.`shipping_category_id` AS `shipping_category_id`, `spree_products`.`created_at` AS `created_at`, `spree_products`.`updated_at` AS `updated_at`, `spree_products`.`promotionable` AS `promotionable`, `spree_products`.`meta_title` AS `meta_title` FROM `spree_products` AS `spree_products` INNER JOIN `spree_variants` AS `spree_variants` ON `spree_variants`.`deleted_at` IS NULL AND `spree_variants`.`product_id` = `spree_products`.`id` AND `spree_variants`.`is_master` = TRUE INNER JOIN `spree_prices` AS `spree_prices` ON `spree_prices`.`deleted_at` IS NULL AND `spree_prices`.`variant_id` = `spree_variants`.`id` WHERE `spree_products`.`deleted_at` IS NULL AND ((`spree_products`.`deleted_at` IS NULL OR `spree_products`.`deleted_at` >= '2020-05-01 07:05:55.179498') AND ((`spree_products`.`discontinue_on` IS NULL OR `spree_products`.`discontinue_on` >= '2020-05-01 07:05:55.179826') AND `spree_products`.`available_on` <= '2020-05-01 07:05:55.179810')) LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  // 169
  // TODO spree-402 DISTINCT
  // 170
  // TODO spree-414 DISTINCT

  @Test // 171
  void testSpree447() {
    final String app = "spree";
    final int stmtId = 447;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxons` AS `spree_taxons` WHERE `spree_taxons`.`parent_id` IS NULL LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test // 172
  void testSpree524() {
    final String app = "spree";
    final int stmtId = 524;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_orders` AS `spree_orders` WHERE `spree_orders`.`user_id` = 658 AND NOT `spree_orders`.`completed_at` IS NULL LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree528() {
    final String app = "spree";
    final int stmtId = 528;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_orders` AS `spree_orders` WHERE `spree_orders`.`user_id` = 660 AND NOT `spree_orders`.`completed_at` IS NULL LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree585() {
    final String app = "spree";
    final int stmtId = 585;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxons` AS `spree_taxons` WHERE `spree_taxons`.`parent_id` = 6 LIMIT 1 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree587() {
    final String app = "spree";
    final int stmtId = 587;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxons` AS `spree_taxons` WHERE `spree_taxons`.`parent_id` = 17 LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree589() {
    final String app = "spree";
    final int stmtId = 589;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxons` AS `spree_taxons` WHERE `spree_taxons`.`name` LIKE '%Imaginary%' LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree591() {
    final String app = "spree";
    final int stmtId = 591;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxons` AS `spree_taxons` LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree614() {
    final String app = "spree";
    final int stmtId = 614;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_payments` AS `spree_payments` WHERE `spree_payments`.`order_id` = 23 LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree620() {
    final String app = "spree";
    final int stmtId = 620;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_payments` AS `spree_payments` WHERE `spree_payments`.`order_id` = 45 LIMIT 1 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree632() {
    final String app = "spree";
    final int stmtId = 632;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_orders` AS `spree_orders` WHERE `spree_orders`.`user_id` = 275 LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree634() {
    final String app = "spree";
    final int stmtId = 634;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_orders` AS `spree_orders` WHERE `spree_orders`.`user_id` = 277 AND NOT `spree_orders`.`completed_at` IS NULL LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree657() {
    final String app = "spree";
    final int stmtId = 657;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_stock_locations` AS `spree_stock_locations` WHERE `spree_stock_locations`.`name` LIKE '%south%' LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree659() {
    final String app = "spree";
    final int stmtId = 659;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_stock_locations` AS `spree_stock_locations` LIMIT 1 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree662() {
    final String app = "spree";
    final int stmtId = 662;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_stock_locations` AS `spree_stock_locations` LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree691() {
    final String app = "spree";
    final int stmtId = 691;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_zones` AS `spree_zones` LIMIT 1 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree693() {
    final String app = "spree";
    final int stmtId = 693;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_zones` AS `spree_zones` LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree695() {
    final String app = "spree";
    final int stmtId = 695;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_zones` AS `spree_zones` WHERE `spree_zones`.`name` LIKE '%south%' LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree712() {
    final String app = "spree";
    final int stmtId = 712;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT `spree_products`.`id` AS `id`, `spree_products`.`name` AS `name`, `spree_products`.`description` AS `description`, `spree_products`.`available_on` AS `available_on`, `spree_products`.`discontinue_on` AS `discontinue_on`, `spree_products`.`deleted_at` AS `deleted_at`, `spree_products`.`slug` AS `slug`, `spree_products`.`meta_description` AS `meta_description`, `spree_products`.`meta_keywords` AS `meta_keywords`, `spree_products`.`tax_category_id` AS `tax_category_id`, `spree_products`.`shipping_category_id` AS `shipping_category_id`, `spree_products`.`created_at` AS `created_at`, `spree_products`.`updated_at` AS `updated_at`, `spree_products`.`promotionable` AS `promotionable`, `spree_products`.`meta_title` AS `meta_title` FROM `spree_products` AS `spree_products` INNER JOIN `spree_variants` AS `spree_variants` ON `spree_variants`.`deleted_at` IS NULL AND `spree_variants`.`product_id` = `spree_products`.`id` AND `spree_variants`.`is_master` = TRUE INNER JOIN `spree_prices` AS `spree_prices` ON `spree_prices`.`deleted_at` IS NULL AND `spree_prices`.`variant_id` = `spree_variants`.`id` WHERE `spree_products`.`deleted_at` IS NULL AND ((`spree_products`.`discontinue_on` IS NULL OR `spree_products`.`discontinue_on` >= '2020-05-01 07:07:42.906418') AND `spree_products`.`available_on` <= '2020-05-01 07:07:42.906389') LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree726() {
    final String app = "spree";
    final int stmtId = 726;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_product_properties` AS `spree_product_properties` WHERE `spree_product_properties`.`product_id` = 1059 LIMIT 1 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree729() {
    final String app = "spree";
    final int stmtId = 729;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_product_properties` AS `spree_product_properties` WHERE `spree_product_properties`.`product_id` = 1061 LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree731() {
    final String app = "spree";
    final int stmtId = 731;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_product_properties` AS `spree_product_properties` INNER JOIN `spree_properties` AS `spree_properties` ON `spree_product_properties`.`property_id` = `spree_properties`.`id` WHERE `spree_product_properties`.`product_id` = 1063 AND `spree_properties`.`name` LIKE '%size%' LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree766() {
    final String app = "spree";
    final int stmtId = 766;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_reimbursements` AS `spree_reimbursements` LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree781() {
    final String app = "spree";
    final int stmtId = 781;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_customer_returns` AS `spree_customer_returns` LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree783() {
    final String app = "spree";
    final int stmtId = 783;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxonomies` AS `spree_taxonomies` WHERE `spree_taxonomies`.`name` LIKE '%style%' LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree785() {
    final String app = "spree";
    final int stmtId = 785;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxonomies` AS `spree_taxonomies` LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree787() {
    final String app = "spree";
    final int stmtId = 787;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_taxonomies` AS `spree_taxonomies` LIMIT 1 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree831() {
    final String app = "spree";
    final int stmtId = 831;
    final String[] expected = {
      "SELECT `spree_promotion_rule_taxons`.`taxon_id` AS `taxon_id` FROM `spree_promotion_rule_taxons` AS `spree_promotion_rule_taxons` WHERE `spree_promotion_rule_taxons`.`promotion_rule_id` = 44"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree1147() {
    final String app = "spree";
    final int stmtId = 1147;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_stock_transfers` AS `spree_stock_transfers` WHERE `spree_stock_transfers`.`destination_location_id` = 4 LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree1150() {
    final String app = "spree";
    final int stmtId = 1150;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_stock_transfers` AS `spree_stock_transfers` LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }

  @Test
  void testSpree1151() {
    final String app = "spree";
    final int stmtId = 1151;
    final String[] expected = {
      "SELECT COUNT(*) FROM (SELECT 1 AS `one` FROM `spree_stock_transfers` AS `spree_stock_transfers` WHERE `spree_stock_transfers`.`source_location_id` = 1 LIMIT 25 OFFSET 0) AS `subquery_for_count`"
    };
    doTest(app, stmtId, expected);
  }
}
