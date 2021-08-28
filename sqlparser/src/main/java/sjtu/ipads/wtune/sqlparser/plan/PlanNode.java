package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.common.utils.Showable;
import sjtu.ipads.wtune.common.utils.TreeNode;

public interface PlanNode extends TreeNode<PlanContext, PlanNode>, Showable {
  OperatorType kind();

  ValueBag values();

  RefBag refs();

  void setContext(PlanContext context);

  void freeze();
}
