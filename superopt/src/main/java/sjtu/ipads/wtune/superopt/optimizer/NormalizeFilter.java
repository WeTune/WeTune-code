package sjtu.ipads.wtune.superopt.optimizer;

import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.sql.plan.PlanKind;

import java.util.Comparator;

import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;
import static sjtu.ipads.wtune.sql.plan.PlanSupport.stringifyNode;

class NormalizeFilter {
  private final PlanContext plan;
  private int rootId;

  NormalizeFilter(PlanContext plan) {
    this.plan = plan;
  }

  int normalizeTree(int rootId) {
    this.rootId = rootId;
    return sortTree(rootId);
  }

  private int sortTree(int nodeId) {
    final PlanKind kind = plan.kindOf(nodeId);
    final int numChildren = kind.numChildren();
    for (int i = 0; i < numChildren; ++i) sortTree(plan.childOf(nodeId, i));

    if (plan.kindOf(nodeId).isFilter() && (nodeId == rootId || isChainHead(nodeId))) {
      return sortChain(nodeId);
    }

    return nodeId;
  }

  private int sortChain(int nodeId) {
    final FilterChain chain = FilterChain.mk(plan, nodeId);
    chain.sort(Comparator.comparing(it -> stringifyNode(plan, plan.nodeIdOf(it), true)));
    chain.assemble();
    return chain.at(0);
  }

  private boolean isChainHead(int nodeId) {
    final int parentId = plan.parentOf(nodeId);
    return parentId == NO_SUCH_NODE || !plan.kindOf(parentId).isFilter();
  }
}
