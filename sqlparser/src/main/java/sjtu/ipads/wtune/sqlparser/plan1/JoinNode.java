package sjtu.ipads.wtune.sqlparser.plan1;

public interface JoinNode extends PlanNode {
  boolean isEquiJoin();

  Expr condition();

  RefBag lhsRefs();

  RefBag rhsRefs();

  void setLhsRefs(RefBag lhsRefs);

  void setRhsRefs(RefBag rhsRefs);
}
