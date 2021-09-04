package sjtu.ipads.wtune.sqlparser.plan;

import java.util.List;

public interface SortNode extends PlanNode {
  List<Expr> orders();

  void setRefHints(int[] hints);

  @Override
  default OperatorType kind() {
    return OperatorType.SORT;
  }
}
