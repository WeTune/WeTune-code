package sjtu.ipads.wtune.prover.normalform;

import static sjtu.ipads.wtune.prover.ProverSupport.canonizeExpr;
import static sjtu.ipads.wtune.prover.ProverSupport.normalizeExpr;
import static sjtu.ipads.wtune.prover.ProverSupport.translateAsUExpr;

import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanSupport;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.Statement;

public class EliminatorTest {
  public static void main(String[] args) {
    final Statement stmt0 =
        Statement.make(
            "broadleaf",
            "SELECT DISTINCT `adminuseri2_`.`ADMIN_USER_ID` FROM `BLC_ADMIN_USER_ROLE_XREF` AS `allusers1_` INNER JOIN `BLC_ADMIN_USER` AS `adminuseri2_` ON `allusers1_`.`ADMIN_USER_ID` = `adminuseri2_`.`ADMIN_USER_ID` LEFT JOIN `BLC_ADMIN_USER_SANDBOX` AS `adminuseri2_1_` ON `adminuseri2_`.`ADMIN_USER_ID` = `adminuseri2_1_`.`ADMIN_USER_ID` WHERE `adminuseri2_`.`ADMIN_USER_ID` = 1",
            null);
    final Statement stmt1 =
        Statement.make(
            "broadleaf",
            "SELECT DISTINCT `adminuseri2_`.`ADMIN_USER_ID` FROM `BLC_ADMIN_USER_ROLE_XREF` AS `allusers1_` INNER JOIN `BLC_ADMIN_USER` AS `adminuseri2_` ON `allusers1_`.`ADMIN_USER_ID` = `adminuseri2_`.`ADMIN_USER_ID` WHERE `adminuseri2_`.`ADMIN_USER_ID` = 1",
            null);
    final Schema schema = stmt0.app().schema("base");

    final PlanNode plan0 = PlanSupport.assemblePlan(stmt0.parsed(), schema);
    final PlanNode plan1 = PlanSupport.assemblePlan(stmt1.parsed(), schema);

    PlanSupport.disambiguate(plan0);
    PlanSupport.disambiguate(plan1);

    final Disjunction normalForm0 = normalizeExpr(translateAsUExpr(plan0));
    final Disjunction normalForm1 = normalizeExpr(translateAsUExpr(plan1));

    System.out.println(normalForm0);
    System.out.println(normalForm1);

    final Disjunction canonicalForm0 = canonizeExpr(normalForm0, schema);
    final Disjunction canonicalForm1 = canonizeExpr(normalForm1, schema);

    System.out.println(canonicalForm0);
    System.out.println(canonicalForm1);
  }
}
