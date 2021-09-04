package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sqlparser.plan.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.superopt.fragment.ConstraintAwareModel;
import sjtu.ipads.wtune.superopt.fragment.Fragment;

import static sjtu.ipads.wtune.common.utils.TreeScaffold.displaceGlobal;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.resolveSubqueryExpr;
import static sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport.alignOutValues;

class Match {
  private PlanNode matchPoint;
  private final ConstraintAwareModel model;

  Match(PlanNode matchPoint, ConstraintAwareModel model) {
    this.matchPoint = matchPoint;
    this.model = model;
  }

  PlanNode substitute(Fragment fragment) {
    final PlanContext newContext = matchPoint.context().dup();
    final PlanNode instantiated = fragment.root().instantiate(model, newContext);
    final PlanNode newNode = displaceGlobal(matchPoint, instantiated, false);
    assert instantiated == newNode;
    // SubqueryNode's `predicate` and `rhsExpr` is lost during instantiation, so re-resolve here.
    return shiftToFilterChainHead(resolveSubqueryExpr(instantiated));
  }

  ConstraintAwareModel model() {
    return model;
  }

  PlanNode shiftToFilterChainHead(PlanNode instantiated) {
    if (!instantiated.kind().isFilter() || !instantiated.successor().kind().isFilter())
      return instantiated;

    alignOutValues(matchPoint, instantiated);
    PlanNode path = instantiated;

    while (path.successor().kind().isFilter()) {
      path = path.successor();
      path.rebindRefs(matchPoint.context());
    }

    instantiated.context().clearRedirections();
    return path;
  }

  void shiftMatchPoint() {
    matchPoint = matchPoint.successor();
  }
}
