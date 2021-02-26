package sjtu.ipads.wtune.sqlparser.plan.internal;

import gnu.trove.list.TIntList;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
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
    final PlanNode[] thisPred = this.predecessors();
    final PlanNode[] copiedPred = node.predecessors();
    for (int i = 0, bound = thisPred.length; i < bound; i++) copiedPred[i] = thisPred[i];
    return node;
  }

  protected abstract PlanNode copy0();

  protected static List<PlanAttribute> resolveUsed0(List<ASTNode> columnRefs, PlanNode lookup) {
    return listMap(lookup::resolveAttribute, columnRefs);
  }

  protected static List<PlanAttribute> resolveUsed1(List<PlanAttribute> attr, PlanNode lookup) {
    return listMap(lookup::resolveAttribute, attr);
  }

  protected static void updateColumnRefs(List<ASTNode> refs, List<PlanAttribute> usedAttrs) {
    for (int i = 0, bound = refs.size(); i < bound; i++) {
      final PlanAttribute usedAttr = usedAttrs.get(i);
      if (usedAttr != null) refs.get(i).update(usedAttr.toColumnRef());
    }
  }

  protected static void updateColumnRefs(
      List<ASTNode> refs, TIntList usedAttrs, List<PlanAttribute> inputAttrs) {
    for (int i = 0, bound = refs.size(); i < bound; i++) {
      final int attrIdx = usedAttrs.get(i);
      if (attrIdx != -1) refs.get(i).update(inputAttrs.get(attrIdx).toColumnRef());
    }
  }

  protected static void bindAttributes(List<PlanAttribute> attrs, PlanNode node) {
    attrs.forEach(it -> it.setOrigin(node));
  }
}
