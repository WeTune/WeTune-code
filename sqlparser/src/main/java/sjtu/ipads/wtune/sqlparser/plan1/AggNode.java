package sjtu.ipads.wtune.sqlparser.plan1;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

public interface AggNode extends PlanNode {
  List<Expr> groups();

  Expr having();

  RefBag aggRefs();

  RefBag groupRefs();

  RefBag havingRefs();

  @Override
  default OperatorType kind() {
    return OperatorType.AGG;
  }
}
