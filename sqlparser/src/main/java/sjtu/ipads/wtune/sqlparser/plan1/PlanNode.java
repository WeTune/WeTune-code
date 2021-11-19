package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.common.utils.ListSupport;

import java.util.List;

public interface PlanNode {
  PlanKind kind();

  default int nodeId(PlanContext context) {
    return context.nodeIdOf(this);
  }

  default PlanNode parent(PlanContext context) {
    return context.nodeAt(context.parentOf(nodeId(context)));
  }

  default int numChildren(PlanContext context) {
    return context.childrenOf(nodeId(context)).length;
  }

  default List<PlanNode> children(PlanContext context) {
    return ListSupport.map(context.childrenOf(nodeId(context)), context::nodeAt);
  }

  default PlanNode child(PlanContext context, int index) {
    return context.nodeAt(context.childOf(nodeId(context), index));
  }
}
