package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.common.utils.TypedTreeNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

public interface PlanNode extends TypedTreeNode<OperatorType> {
  ValueBag values();

  RefBag refs();

  PlanContext context();

  PlanNode successor();

  PlanNode[] predecessors();

  void setContext(PlanContext context);

  void setSuccessor(PlanNode successor);

  void setPredecessor(int idx, PlanNode predecessor);

  PlanNode copy(PlanContext ctx);
}
