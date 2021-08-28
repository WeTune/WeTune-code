package sjtu.ipads.wtune.superopt.optimizer1;

import sjtu.ipads.wtune.sqlparser.plan1.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;
import sjtu.ipads.wtune.superopt.fragment1.ConstraintAwareModel;
import sjtu.ipads.wtune.superopt.fragment1.Fragment;

class Match {
  private PlanNode matchPoint;
  private final ConstraintAwareModel model;

  Match(PlanNode matchPoint, ConstraintAwareModel model) {
    this.matchPoint = matchPoint;
    this.model = model;
  }

  PlanNode substitute(Fragment fragment) {
    final PlanContext newContext = matchPoint.context().dup();
    return fragment.root().instantiate(model, newContext);
  }

  ConstraintAwareModel model() {
    return model;
  }

  void shiftMatchPoint() {
    matchPoint = matchPoint.successor();
  }
}
