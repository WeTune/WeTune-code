package sjtu.ipads.wtune.prover.decision;

import sjtu.ipads.wtune.prover.ProverSupport;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanSupport;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.Statement;

class DecisionTest {
  public static void main(String[] args) {
    final Statement stmt0 = Statement.make("test", "SELECT DISTINCT d.p FROM d INNER JOIN c", null);
    final Statement stmt1 = Statement.make("test", "SELECT DISTINCT d.p FROM d", null);
    final Schema schema = stmt0.app().schema("base");

    final PlanNode plan0 = PlanSupport.assemblePlan(stmt0.parsed(), schema);
    final PlanNode plan1 = PlanSupport.assemblePlan(stmt1.parsed(), schema);

    System.out.println(ProverSupport.decideEq(plan0, plan1));
  }
}
