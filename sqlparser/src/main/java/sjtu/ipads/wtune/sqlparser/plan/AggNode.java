package sjtu.ipads.wtune.sqlparser.plan;

import java.util.List;

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

    void setRefHints(int[] refHints);
}
