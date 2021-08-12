package sjtu.ipads.wtune.sqlparser.plan1;

public interface FilterNode extends PlanNode {
  Expr predicate();
}
