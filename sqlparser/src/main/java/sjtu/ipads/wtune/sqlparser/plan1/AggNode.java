package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

import java.util.List;

public interface AggNode extends PlanNode {
  List<Expr> groups();

  Expr having();

  @Override default OperatorType type() {
    return OperatorType.AGG;
  }
}
