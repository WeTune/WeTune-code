package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

import java.util.List;
import java.util.Objects;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.simpleName;

public interface PlanNode {
  OperatorType type();

  PlanNode successor();

  PlanNode[] predecessors();

  void setPredecessor(int idx, PlanNode op);

  void setSuccessor(PlanNode successor);

  void resolveUsed();

  List<AttributeDef> definedAttributes();

  List<AttributeDef> usedAttributes();

  PlanNode copy();

  void replacePredecessor(PlanNode target, PlanNode rep);

  default AttributeDef resolveAttribute(String qualification, String name) {
    qualification = simpleName(qualification);
    name = simpleName(name);

    for (AttributeDef attr : definedAttributes())
      if (attr.isReferencedBy(qualification, name)) return attr;

    return null;
  }

  default AttributeDef resolveAttribute(ASTNode columnRef) {
    if (!COLUMN_REF.isInstance(columnRef)) throw new IllegalArgumentException();
    final ASTNode colName = columnRef.get(COLUMN_REF_COLUMN);
    return resolveAttribute(colName.get(COLUMN_NAME_TABLE), colName.get(COLUMN_NAME_COLUMN));
  }

  default AttributeDef resolveAttribute(int id) {
    for (AttributeDef outAttr : definedAttributes()) if (outAttr.isReferencedBy(id)) return outAttr;
    return null;
  }

  default AttributeDef resolveAttribute(AttributeDef attr) {
    if (attr == null) return null;
    final AttributeDef resolved = resolveAttribute(attr.id());
    if (resolved == null && attr.isIdentity()) {
      final AttributeDef upstream = attr.upstream();
      assert upstream != null;
      return upstream == attr ? null : resolveAttribute(upstream);
    } else return resolved;
  }

  static PlanNode rootOf(PlanNode node) {
    while (node.successor() != null) node = node.successor();
    return node;
  }

  static PlanNode copyToRoot(PlanNode node) {
    // copy the nodes on the path from `node` to root
    final PlanNode copy = node.copy();
    if (node.successor() == null) return copy;

    final PlanNode successor = copyToRoot(node.successor());
    successor.replacePredecessor(node, copy);
    return copy;
  }

  static PlanNode copyTree(PlanNode node) {
    final PlanNode copy = node.copy();
    final PlanNode[] predecessors = node.predecessors();
    for (int i = 0; i < predecessors.length; i++) copy.setPredecessor(i, copyTree(predecessors[i]));
    return copy;
  }

  static void resolveUsedTree(PlanNode node) {
    for (PlanNode predecessor : node.predecessors()) resolveUsedTree(predecessor);
    node.resolveUsed();
  }

  static boolean equalsTree(PlanNode n0, PlanNode n1) {
    if (!Objects.equals(n0, n1)) return false;
    final PlanNode[] predecessors0 = n0.predecessors();
    final PlanNode[] predecessors1 = n1.predecessors();
    for (int i = 0, bound = predecessors0.length; i < bound; i++)
      if (!Objects.equals(predecessors0[i], predecessors1[i])) return false;
    return true;
  }

  static int hashCodeTree(PlanNode n0) {
    int hash = n0.hashCode();
    for (PlanNode predecessor : n0.predecessors()) hash = hash * 31 + hashCodeTree(predecessor);
    return hash;
  }
}
