package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.common.utils.TypedTreeNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

import java.util.List;
import java.util.Objects;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.simpleName;

public interface PlanNode extends TypedTreeNode<OperatorType> {
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
      if ((qualification == null || qualification.equals(attr.qualification()))
          && name.equals(attr.name())) return attr;

    for (AttributeDef attr : definedAttributes())
      if (attr.referencesTo(qualification, name)) return attr;

    return null;
  }

  default AttributeDef resolveAttribute(ASTNode columnRef) {
    if (!COLUMN_REF.isInstance(columnRef)) throw new IllegalArgumentException();
    final ASTNode colName = columnRef.get(COLUMN_REF_COLUMN);
    return resolveAttribute(colName.get(COLUMN_NAME_TABLE), colName.get(COLUMN_NAME_COLUMN));
  }

  default AttributeDef resolveAttribute(int id) {
    for (AttributeDef outAttr : definedAttributes()) if (outAttr.referencesTo(id)) return outAttr;
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

  static PlanNode copyOnTree(PlanNode node) {
    final PlanNode copy = node.copy();
    final PlanNode[] predecessors = node.predecessors();
    for (int i = 0; i < predecessors.length; i++)
      copy.setPredecessor(i, copyOnTree(predecessors[i]));
    return copy;
  }

  static void resolveUsedToRoot(PlanNode node) {
    node.resolveUsed();
    if (node.successor() != null) resolveUsedToRoot(node.successor());
  }

  static void resolveUsedOnTree(PlanNode node) {
    for (PlanNode predecessor : node.predecessors()) resolveUsedOnTree(predecessor);
    node.resolveUsed();
  }

  static boolean equalsOnTree(PlanNode n0, PlanNode n1) {
    if (!Objects.equals(n0, n1)) return false;
    final PlanNode[] predecessors0 = n0.predecessors();
    final PlanNode[] predecessors1 = n1.predecessors();
    for (int i = 0, bound = predecessors0.length; i < bound; i++)
      if (!Objects.equals(predecessors0[i], predecessors1[i])) return false;
    return true;
  }

  static int hashCodeOnTree(PlanNode n0) {
    int hash = n0.hashCode();
    for (PlanNode predecessor : n0.predecessors()) hash = hash * 31 + hashCodeOnTree(predecessor);
    return hash;
  }

  static String toStringOnTree(PlanNode n) {
    return toStringOnTree(n, new StringBuilder()).toString();
  }

  static boolean checkConformity(PlanNode n) {
    for (PlanNode predecessor : n.predecessors()) {
      if (!checkConformity(predecessor)) return false;
      if (predecessor.successor() != n) return false;
    }
    return true;
  }

  private static StringBuilder toStringOnTree(PlanNode n, StringBuilder builder) {
    builder.append(n);
    builder.append('(');
    if (n.type().numPredecessors() >= 1) toStringOnTree(n.predecessors()[0], builder);
    if (n.type().numPredecessors() >= 2) {
      builder.append(',');
      toStringOnTree(n.predecessors()[1], builder);
    }
    builder.append(')');
    return builder;
  }
}
