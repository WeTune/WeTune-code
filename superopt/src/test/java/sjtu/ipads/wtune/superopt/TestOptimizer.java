package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.superopt.internal.Optimizer;
import sjtu.ipads.wtune.superopt.optimization.SubstitutionBank;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestOptimizer {
  private static void doTest(String appName, int stmtId, String expected) throws IOException {
    final Statement stmt = Statement.findOne(appName, stmtId);
    final ASTNode ast = stmt.parsed();
    ast.context().setSchema(stmt.app().schema("base"));

    final SubstitutionBank bank = SubstitutionBank.make();
    bank.importFrom(Files.readAllLines(Paths.get("wtune_data", "substitution_bank")));

    final Optimizer optimizer = Optimizer.make(bank);
    final List<ASTNode> optimized = optimizer.optimize(ast);

    assertTrue(optimized.stream().anyMatch(it -> expected.equals(it.toString())));
  }

  @Test
  void testBroadleaf199() throws IOException {
    doTest(
        "broadleaf",
        199,
        "SELECT `adminrolei0_`.`admin_role_id` AS `admin_ro1_7_`, `adminrolei0_`.`created_by` AS `created_2_7_`, `adminrolei0_`.`date_created` AS `date_cre3_7_`, `adminrolei0_`.`date_updated` AS `date_upd4_7_`, `adminrolei0_`.`updated_by` AS `updated_5_7_`, `adminrolei0_`.`description` AS `descript6_7_`, `adminrolei0_`.`name` AS `name7_7_` FROM `blc_admin_role` AS `adminrolei0_` INNER JOIN `blc_admin_user_role_xref` AS `allusers1_` ON `adminrolei0_`.`admin_role_id` = `allusers1_`.`admin_role_id` WHERE `allusers1_`.`admin_user_id` = 1 ORDER BY `adminrolei0_`.`admin_ro1_7_` ASC LIMIT 50");
  }

  @Test
  void testBroadleaf200() throws IOException {
    doTest(
        "broadleaf",
        200,
        "SELECT COUNT(`adminrolei0_`.`ADMIN_ROLE_ID`) AS `col_0_0_` FROM `blc_admin_user_role_xref` AS `allusers1_` WHERE `allusers1_`.`admin_user_id` = 1");
  }
}
