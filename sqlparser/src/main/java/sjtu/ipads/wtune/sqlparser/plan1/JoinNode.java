package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

public interface JoinNode extends PlanNode {
  boolean isEquiJoin();

  Expr condition();

  RefBag lhsRefs();

  RefBag rhsRefs();

  JoinNode flip(PlanContext context);

  void setLhsRefs(RefBag lhsRefs);

  void setRhsRefs(RefBag rhsRefs);

  static JoinNode mk(OperatorType joinType, RefBag lhsRefs, RefBag rhsRefs) {
    return JoinNodeImpl.mk(joinType, lhsRefs, rhsRefs);
  }
}
