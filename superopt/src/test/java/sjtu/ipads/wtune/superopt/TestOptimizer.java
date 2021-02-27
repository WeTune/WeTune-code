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
    final Schema schema = stmt.app().schema("base");
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

  // TODO: diaspora-224
  @Test
  void testDiaspora295() {
    doTest(
        "diaspora",
        295,
        "SELECT COUNT(DISTINCT `contacts`.`id`) FROM `contacts` AS `contacts` WHERE `contacts`.`user_id` = 1945");
  }
}
