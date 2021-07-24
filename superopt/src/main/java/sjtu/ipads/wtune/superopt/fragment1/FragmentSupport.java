package sjtu.ipads.wtune.superopt.fragment1;

import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.superopt.constraint.Constraints;

public class FragmentSupport {
  static PlanNode translateAsPlan(Fragment fragment, Constraints constraints) {
    return new PlanTranslator(fragment, constraints).translate();
  }
}
