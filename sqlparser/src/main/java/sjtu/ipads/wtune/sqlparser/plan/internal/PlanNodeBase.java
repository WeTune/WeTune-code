package sjtu.ipads.wtune.sqlparser.plan.internal;

import gnu.trove.list.TIntList;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.plan.internal.DerivedAttributeDef.fastEquals;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.selectItemAlias;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.simpleName;

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

  @Override
  public AttributeDef resolveAttribute(String qualification, String name) {
    qualification = simpleName(qualification);
    name = simpleName(name);

    for (AttributeDef attr : definedAttributes())
      if ((qualification == null || qualification.equals(attr.qualification()))
          && name.equals(attr.name())) return attr;

    for (AttributeDef attr : definedAttributes())
      if (attr.referencesTo(qualification, name)) return attr;

    return null;
  }

  @Override
  public AttributeDef resolveAttribute(ASTNode columnRef) {
    if (!COLUMN_REF.isInstance(columnRef)) throw new IllegalArgumentException();
    final ASTNode colName = columnRef.get(COLUMN_REF_COLUMN);
    return resolveAttribute(colName.get(COLUMN_NAME_TABLE), colName.get(COLUMN_NAME_COLUMN));
  }

  @Override
  public AttributeDef resolveAttribute(int attrId) {
    // fast path
    for (AttributeDef outAttr : definedAttributes()) if (outAttr.id() == attrId) return outAttr;
    // slow path
    for (AttributeDef outAttr : definedAttributes())
      if (outAttr.referencesTo(attrId)) return outAttr;
    return null;
  }

  @Override
  public AttributeDef resolveAttribute(AttributeDef attr) {
    if (attr == null) return null;
    // fast path
    for (AttributeDef outAttr : definedAttributes()) if (fastEquals(outAttr, attr)) return outAttr;
    // slow path
    for (AttributeDef outAttr : definedAttributes()) if (outAttr.equals(attr)) return outAttr;
    return null;
  }

  protected abstract PlanNode copy0();

  protected AttributeDef makeAttribute(String qualification, ASTNode selectItem) {
    final ASTNode expr = selectItem.get(SELECT_ITEM_EXPR);
    final String name = selectItemAlias(selectItem);
    final int id = System.identityHashCode(this) * 31 + selectItem.hashCode();
    return AttributeDef.fromExpr(id, qualification, name, expr);
  }

  protected static List<AttributeDef> resolveUsed0(List<ASTNode> columnRefs, PlanNode lookup) {
    return listMap(lookup::resolveAttribute, columnRefs);
  }

  protected static List<AttributeDef> resolveUsed1(List<AttributeDef> attr, PlanNode lookup) {
    return listMap(lookup::resolveAttribute, attr);
  }

  protected static void updateColumnRefs(List<ASTNode> refs, List<AttributeDef> usedAttrs) {
    for (int i = 0, bound = refs.size(); i < bound; i++) {
      final AttributeDef usedAttr = usedAttrs.get(i);
      if (usedAttr != null) refs.get(i).update(usedAttr.toColumnRef());
    }
  }

  protected static void updateColumnRefs(
      List<ASTNode> refs, TIntList usedAttrs, List<AttributeDef> inputAttrs) {
    for (int i = 0, bound = refs.size(); i < bound; i++) {
      final int attrIdx = usedAttrs.get(i);
      if (attrIdx != -1) refs.get(i).update(inputAttrs.get(attrIdx).toColumnRef());
    }
  }

  protected static void bindAttributes(List<AttributeDef> attrs, PlanNode node) {
    attrs.forEach(it -> it.setDefiner(node));
  }
}
