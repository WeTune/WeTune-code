package sjtu.ipads.wtune.sqlparser.plan;

public interface FilterNode extends PlanNode {
  Expr predicate();
}
