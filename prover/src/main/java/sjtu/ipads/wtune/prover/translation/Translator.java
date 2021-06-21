package sjtu.ipads.wtune.prover.translation;

import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan1.Value;

public class Translator {
  private PlanNode plan;

  private Tuple translateTuple(Tuple base, Value v) {
    final String qualification = v.qualification();
    final String name = v.name();
    return base.proj(qualification).proj(name);
  }
}
