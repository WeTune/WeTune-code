package sjtu.ipads.wtune.sqlparser.plan;

public interface LimitNode extends PlanNode {
  Expr limit();

  Expr offset();

  @Override
  default OperatorType kind() {
    return OperatorType.LIMIT;
  }
}
