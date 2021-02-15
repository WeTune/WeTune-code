package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.OutputAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public abstract class PlanNodeBase implements PlanNode {
  private PlanNode successor;
  private final PlanNode[] predecessors;

  protected PlanNodeBase() {
    predecessors = new PlanNode[type().numPredecessors()];
  }

  @Override
  public PlanNode successor() {
    return successor;
  }

  @Override
  public PlanNode[] predecessors() {
    return predecessors;
  }

  @Override
  public void setPredecessor(int idx, PlanNode op) {
    predecessors[idx] = op;
    if (op != null) op.setSuccessor(this);
  }

  @Override
  public void setSuccessor(PlanNode successor) {
    this.successor = successor;
  }

  @Override
  public PlanNode copy() {
    final PlanNode node = copy0();
    node.setSuccessor(successor());
    final PlanNode[] predecessors = this.predecessors();
    for (int i = 0, bound = predecessors.length; i < bound; i++)
      node.setPredecessor(i, predecessors[i]);
    return node;
  }

  protected static List<OutputAttribute> resolveUsedAttributes0(
      List<ASTNode> columnRefs, PlanNode lookup) {
    return listMap(lookup::resolveAttribute, columnRefs);
  }

  protected static List<OutputAttribute> resolveUsedAttributes1(
      List<OutputAttribute> attr, PlanNode lookup) {
    return listMap(lookup::resolveAttribute, attr);
  }

  protected abstract PlanNode copy0();
}
