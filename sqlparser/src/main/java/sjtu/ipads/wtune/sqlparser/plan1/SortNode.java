package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

import java.util.List;

public interface SortNode extends PlanNode {
  List<Expr> orders();

  @Override
  default OperatorType type() {
    return OperatorType.SORT;
  }
}
