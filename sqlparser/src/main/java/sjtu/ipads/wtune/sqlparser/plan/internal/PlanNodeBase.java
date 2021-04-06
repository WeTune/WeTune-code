package sjtu.ipads.wtune.sqlparser.plan.internal;

import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_EXPR;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.selectItemAlias;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import java.util.ArrayList;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDefBag;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

public abstract class PlanNodeBase implements PlanNode {
  private PlanNode successor;
  private final PlanNode[] predecessors;

  protected PlanNodeBase() {
    predecessors = new PlanNode[type().numPredecessors()];
  }

  protected PlanNodeBase(OperatorType type) {
    predecessors = new PlanNode[type.numPredecessors()];
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
  public void replacePredecessor(PlanNode target, PlanNode rep) {
    final PlanNode[] predecessors = predecessors();
    for (int i = 0; i < predecessors.length; i++)
      if (predecessors[i] == target) {
        setPredecessor(i, rep);
        break;
      }
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    return this.toString().equals(o.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  // helper method user in AggNode & ProjNode
  protected static AttributeDefBag makeAttributes(String qualification, List<ASTNode> selectItems) {
    final Object key = new Object();
    final List<AttributeDef> attrs = new ArrayList<>(selectItems.size());
    for (ASTNode selectItem : selectItems) {
      final ASTNode expr = selectItem.get(SELECT_ITEM_EXPR);
      final String name = selectItemAlias(selectItem);
      final int id = System.identityHashCode(key) * 31 + selectItem.hashCode();
      attrs.add(AttributeDef.fromExpr(id, qualification, name, expr));
    }
    return AttributeDefBag.makeBag(attrs);
  }

  // helper method used in AggNode & SortNode
  protected static TIntList resolveUsed(List<ASTNode> nodes, AttributeDefBag bag) {
    final TIntList used = new TIntArrayList(nodes.size());
    for (ASTNode node : nodes)
      for (ASTNode colRef : gatherColumnRefs(node)) used.add(bag.locate(colRef));
    return used;
  }

  protected static void updateColumnRefs(List<ASTNode> refs, List<AttributeDef> usedAttrs) {
    for (int i = 0, bound = refs.size(); i < bound; i++) {
      final AttributeDef usedAttr = usedAttrs.get(i);
      if (usedAttr != null) refs.get(i).update(usedAttr.makeColumnRef());
    }
  }

  protected static void updateColumnRefs(
      List<ASTNode> refs, TIntList usedAttrs, List<AttributeDef> inputAttrs) {
    for (int i = 0, bound = refs.size(); i < bound; i++) {
      final int attrIdx = usedAttrs.get(i);
      if (attrIdx != -1) refs.get(i).update(inputAttrs.get(attrIdx).makeColumnRef());
    }
  }
}
