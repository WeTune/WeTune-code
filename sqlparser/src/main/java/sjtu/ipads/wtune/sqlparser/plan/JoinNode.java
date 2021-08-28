package sjtu.ipads.wtune.sqlparser.plan;

public interface JoinNode extends PlanNode {
  boolean isEquiJoin();

  Expr condition();

  RefBag lhsRefs();

  RefBag rhsRefs();

  JoinNode flip(PlanContext context); // If `context` is null, do in-place. Otherwise, make a copy.

  void setJoinType(OperatorType type);

  void setLhsRefs(RefBag lhsRefs);

  void setRhsRefs(RefBag rhsRefs);

  static JoinNode mk(OperatorType joinType, RefBag lhsRefs, RefBag rhsRefs) {
    return JoinNodeImpl.mk(joinType, lhsRefs, rhsRefs);
  }
}
