package sjtu.ipads.wtune.prover.normalform;

import static sjtu.ipads.wtune.prover.ProverSupport.canonizeExpr;
import static sjtu.ipads.wtune.prover.ProverSupport.normalizeExpr;
import static sjtu.ipads.wtune.prover.ProverSupport.translateToExpr;

import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanSupport;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.Statement;

public class CanonizationTest {
  public static void main(String[] args) {
    final Statement stmt0 =
        Statement.make("test", "SELECT c.v FROM c INNER JOIN d ON c.u = d.p", null);
    final Statement stmt1 = Statement.make("test", "SELECT DISTINCT a.j FROM a", null);

    final Schema schema = stmt0.app().schema("base", true);
    final PlanNode plan0 = PlanSupport.assemblePlan(stmt0.parsed(), schema);
    final PlanNode plan1 = PlanSupport.assemblePlan(stmt1.parsed(), schema);
    PlanSupport.disambiguate(plan0);
    PlanSupport.disambiguate(plan1);

    final Disjunction normalForm0 = normalizeExpr(translateToExpr(plan0));
    final Disjunction normalForm1 = normalizeExpr(translateToExpr(plan1));
    final Disjunction canonicalForm0 = canonizeExpr(normalForm0, schema);
    final Disjunction canonicalForm1 = canonizeExpr(normalForm1, schema);

    System.out.println(canonicalForm0);
    System.out.println(canonicalForm1);
  }
}
