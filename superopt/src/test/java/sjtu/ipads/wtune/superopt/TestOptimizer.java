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

  @Test
  void testBroadleaf199() {
    doTest(
        "broadleaf",
        199,
        "SELECT `adminrolei0_`.`admin_role_id` AS `admin_ro1_7_`, `adminrolei0_`.`created_by` AS `created_2_7_`, `adminrolei0_`.`date_created` AS `date_cre3_7_`, `adminrolei0_`.`date_updated` AS `date_upd4_7_`, `adminrolei0_`.`updated_by` AS `updated_5_7_`, `adminrolei0_`.`description` AS `descript6_7_`, `adminrolei0_`.`name` AS `name7_7_` FROM `blc_admin_role` AS `adminrolei0_` INNER JOIN `blc_admin_user_role_xref` AS `allusers1_` ON `adminrolei0_`.`admin_role_id` = `allusers1_`.`admin_role_id` WHERE `allusers1_`.`admin_user_id` = 1 ORDER BY `adminrolei0_`.`admin_ro1_7_` ASC LIMIT 50");
  }

  @Test
  void testBroadleaf200() {
    doTest(
        "broadleaf",
        200,
        "SELECT COUNT(`allusers1_`.`admin_role_id`) AS `col_0_0_` FROM `blc_admin_user_role_xref` AS `allusers1_` WHERE `allusers1_`.`admin_user_id` = 1");
  }

  @Test
  void testBroadleaf201() {
    doTest(
        "broadleaf",
        201,
        "SELECT `adminpermi0_`.`admin_permission_id` AS `admin_pe1_4_`, `adminpermi0_`.`description` AS `descript2_4_`, `adminpermi0_`.`is_friendly` AS `is_frien3_4_`, `adminpermi0_`.`name` AS `name4_4_`, `adminpermi0_`.`permission_type` AS `permissi5_4_` FROM `blc_admin_permission` AS `adminpermi0_` INNER JOIN `blc_admin_user_permission_xref` AS `allusers1_` ON `adminpermi0_`.`admin_permission_id` = `allusers1_`.`admin_permission_id` WHERE `allusers1_`.`admin_user_id` = 1 AND `adminpermi0_`.`is_friendly` = 1 ORDER BY `adminpermi0_`.`descript2_4_` ASC LIMIT 50",
        "SELECT `adminpermi0_`.`admin_permission_id` AS `admin_pe1_4_`, `adminpermi0_`.`description` AS `descript2_4_`, `adminpermi0_`.`is_friendly` AS `is_frien3_4_`, `adminpermi0_`.`name` AS `name4_4_`, `adminpermi0_`.`permission_type` AS `permissi5_4_` FROM `blc_admin_permission` AS `adminpermi0_` INNER JOIN `blc_admin_user_permission_xref` AS `allusers1_` ON `adminpermi0_`.`admin_permission_id` = `allusers1_`.`admin_permission_id` WHERE `adminpermi0_`.`is_friendly` = 1 AND `allusers1_`.`admin_user_id` = 1 ORDER BY `adminpermi0_`.`descript2_4_` ASC LIMIT 50");
  }

  @Test
  void testBroadleaf241() {
    doTest(
        "broadleaf",
        241,
        "SELECT COUNT(`adminuseri0_`.`admin_user_id`) AS `col_0_0_` FROM `blc_admin_user` AS `adminuseri0_` WHERE `adminuseri0_`.`archived` = 'N' OR `adminuseri0_`.`archived` IS NULL");
  }

  @Test
  void testDiaspora202() {
    // TODO: remove distinct
    final String expected =
        "SELECT COUNT(DISTINCT `contacts`.`id`) FROM `contacts` AS `contacts` INNER JOIN `aspect_memberships` AS `aspect_memberships` ON `aspect_memberships`.`aspect_id` = 250 AND `aspect_memberships`.`contact_id` = `contacts`.`id` WHERE `contacts`.`user_id` = 332";
    doTest("diaspora", 202, expected);
  }

  // TODO: diaspora-224 too long

  @Test
  void testDiaspora295() {
    doTest(
        "diaspora",
        295,
        "SELECT COUNT(DISTINCT `contacts`.`id`) FROM `contacts` AS `contacts` WHERE `contacts`.`user_id` = 1945");
  }

  @Test
  void testDiaspora460() {
    final String appName = "diaspora";
    final int stmtId = 460;
    final String expected =
        "SELECT `contacts`.`person_id` AS `person_id` FROM `contacts` AS `contacts` INNER JOIN `aspect_memberships` AS `aspect_memberships` ON `contacts`.`id` = `aspect_memberships`.`contact_id` WHERE 1 = 0";
    doTest(appName, stmtId, expected);
  }

  @Test
  void testDiaspora478() {
    // TODO: DISTINCT
    final String appName = "diaspora";
    final int stmtId = 478;
    final String[] expected = {
      "SELECT `profiles`.`last_name` AS `alias_0`, `contacts`.`id` AS `id` FROM `contacts` AS `contacts` INNER JOIN `profiles` AS `profiles` ON `contacts`.`person_id` = `profiles`.`person_id` WHERE `contacts`.`user_id` = 3 AND `contacts`.`receiving` = TRUE ORDER BY `profiles`.`alias_0` ASC LIMIT 25 OFFSET 0",
      "SELECT `profiles`.`last_name` AS `alias_0`, `contacts`.`id` AS `id` FROM `contacts` AS `contacts` INNER JOIN `profiles` AS `profiles` ON `contacts`.`person_id` = `profiles`.`person_id` WHERE `contacts`.`receiving` = TRUE AND `contacts`.`user_id` = 3 ORDER BY `profiles`.`alias_0` ASC LIMIT 25 OFFSET 0"
    };

    doTest(appName, stmtId, expected);
  }

  // TODO: diaspora-492 failed to remove subquery

  @Test
  void testDiscourse123() {
    final String appName = "discourse";
    final int stmtId = 123;
    final String expected =
        "SELECT `category_groups`.`category_id` AS `category_id` FROM `category_groups` AS `category_groups` INNER JOIN `group_users` AS `group_users` ON `category_groups`.`group_id` = `group_users`.`group_id` WHERE `group_users`.`user_id` = 86";
    doTest(appName, stmtId, expected);
  }

  @Test
  void testDiscourse182() {
    final String appName = "discourse";
    final int stmtId = 182;
    final String expected =
        "SELECT `topic_allowed_groups`.`group_id` AS `group_id` FROM `topic_allowed_groups` AS `topic_allowed_groups` WHERE `topic_allowed_groups`.`topic_id` = 15596";
    doTest(appName, stmtId, expected);
  }

  // TODO: discourse-207 too long

  @Test
  void testDiscourse276() {
    final String appName = "discourse";
    final int stmtId = 276;
    final String expected =
        "SELECT `ignored_users`.`ignored_user_id` AS `ignored_user_id` FROM `ignored_users` AS `ignored_users` WHERE `ignored_users`.`user_id` = 155";
    doTest(appName, stmtId, expected);
  }

  @Test
  void testDiscourse277() {
    final String appName = "discourse";
    final int stmtId = 277;
    final String expected =
        "SELECT `muted_users`.`muted_user_id` AS `muted_user_id` FROM `muted_users` AS `muted_users` WHERE `muted_users`.`user_id` = 155";
    doTest(appName, stmtId, expected);
  }

  @Test
  void testDiscourse371() {
    final String appName = "discourse";
    final int stmtId = 371;
    final String expected =
        "SELECT `topic_allowed_users`.`user_id` AS `user_id` FROM `topic_allowed_users` AS `topic_allowed_users` WHERE `topic_allowed_users`.`topic_id` = 15632";
    doTest(appName, stmtId, expected);
  }

  @Test
  void testDiscourse373() {
    final String appName = "discourse";
    final int stmtId = 373;
    final String expected =
        "SELECT `group_users`.`user_id` AS `user_id` FROM `group_users` AS `group_users` WHERE `group_users`.`group_id` = 2";
    doTest(appName, stmtId, expected);
  }

  @Test
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

  @Test
  void testDiscourse449() {
    final String appName = "discourse";
    final int stmtId = 449;
    final String[] expected =
        new String[] {
          "SELECT `child_themes`.`parent_theme_id` AS `parent_theme_id` FROM `child_themes` AS `child_themes` WHERE `child_themes`.`child_theme_id` = 1017",
        };
    doTest(appName, stmtId, expected);
  }

  @Test
  void testDiscourse599() {
    final String appName = "discourse";
    final int stmtId = 599;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `category_tags` AS `category_tags` WHERE `category_tags`.`category_id` = 3121",
        };
    doTest(appName, stmtId, expected);
  }

  @Test
  void testDiscourse600() {
    final String appName = "discourse";
    final int stmtId = 600;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `category_tag_groups` AS `category_tag_groups` WHERE `category_tag_groups`.`category_id` = 3121",
        };
    doTest(appName, stmtId, expected);
  }

  @Test
  void testDiscourse624() {
    final String appName = "discourse";
    final int stmtId = 624;
    final String[] expected =
        new String[] {
          "SELECT `group_users`.`group_id` AS `group_id` FROM `group_users` AS `group_users` WHERE `group_users`.`user_id` = 247",
        };
    doTest(appName, stmtId, expected);
  }

  @Test
  void testDiscourse660() {
    final String appName = "discourse";
    final int stmtId = 660;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `category_groups` AS `category_groups` WHERE `category_groups`.`group_id` = 2378",
        };
    doTest(appName, stmtId, expected);
  }

  @Test
  void testDiscourse833() {
    final String appName = "discourse";
    final int stmtId = 833;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `group_users` AS `group_users` WHERE `group_users`.`group_id` = 2397",
        };
    doTest(appName, stmtId, expected);
  }

  // TODO: discourse: 877 too long

  @Test
  void testDiscourse942() {
    final String appName = "discourse";
    final int stmtId = 942;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `group_users` AS `gu` WHERE `gu`.`user_id` = 779 AND (`gu`.`owner` AND `gu`.`group_id` > 0)",
        };
    doTest(appName, stmtId, expected);
  }
}
