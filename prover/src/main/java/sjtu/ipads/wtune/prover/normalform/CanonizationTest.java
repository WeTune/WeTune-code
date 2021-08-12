package sjtu.ipads.wtune.prover.normalform;

import static sjtu.ipads.wtune.prover.ProverSupport.canonizeExpr;
import static sjtu.ipads.wtune.prover.ProverSupport.normalizeExpr;
import static sjtu.ipads.wtune.prover.ProverSupport.translateAsUExpr;

import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanSupport;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.Statement;

public class CanonizationTest {
  public static void main(String[] args) {
    final Statement stmt0 =
        Statement.make("test", "Select sub.* From (Select a.j From a) As sub", null);
    final Schema schema = stmt0.app().schema("base", true);
    final PlanNode plan0 = PlanSupport.assemblePlan(stmt0.parsed(), schema);
    PlanSupport.disambiguate(plan0);

    final Disjunction normalForm0 = normalizeExpr(translateAsUExpr(plan0));
    System.out.println(normalForm0);
    final Disjunction canonicalForm0 = canonizeExpr(normalForm0, schema);
    System.out.println(canonicalForm0);
  }
}
