package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.simpleName;

public interface PlanNode {
  OperatorType type();

  PlanNode successor();

  PlanNode[] predecessors();

  List<PlanAttribute> outputAttributes();

  void setPredecessor(int idx, PlanNode op);

  void setSuccessor(PlanNode successor);

  PlanNode copy();

  List<PlanAttribute> usedAttributes();

  void resolveUsedAttributes();

  static PlanNode rootOf(PlanNode node) {
    while (node.successor() != null) node = node.successor();
    return node;
  }

  static PlanNode copyTree(PlanNode node) {
    final PlanNode copy = node.copy();
    final PlanNode[] predecessors = node.predecessors();
    for (int i = 0; i < predecessors.length; i++) copy.setPredecessor(i, copyTree(predecessors[i]));
    return copy;
  }

  static PlanNode copyToRoot(PlanNode node) {
    // copy the nodes on the path from `node` to root
    final PlanNode copy = node.copy();
    if (node.successor() == null) return copy;

    final PlanNode successor = copyToRoot(node.successor());
    successor.replacePredecessor(node, copy);
    return copy;
  }

  static void resolveUsedAttributes(PlanNode node) {
    node.resolveUsedAttributes();
    for (PlanNode predecessor : node.predecessors()) resolveUsedAttributes(predecessor);
  }

  default PlanAttribute resolveAttribute(String qualification, String name) {
    qualification = simpleName(qualification);
    name = simpleName(name);

    for (PlanAttribute attr : outputAttributes())
      if ((qualification == null || qualification.equals(attr.qualification()))
          && name.equals(attr.name())) return attr;

    return null;
  }

  default PlanAttribute resolveAttribute(ASTNode columnRef) {
    if (!COLUMN_REF.isInstance(columnRef)) throw new IllegalArgumentException();
    final ASTNode colName = columnRef.get(COLUMN_REF_COLUMN);
    return resolveAttribute(colName.get(COLUMN_NAME_TABLE), colName.get(COLUMN_NAME_COLUMN));
  }

  /**
   * Look up for an attribute that is "ref-equal" to `attr`.
   *
   * <p>This is useful when input attributes are changed. e.g.
   *
   * <pre>  SELECT a.i FROM (SELECT j FROM t) AS a</pre>
   *
   * when the subquery is flatten, {@code `a.i`} should be rectified and results in
   *
   * <pre>  SELECT t.j FROM t</pre>
   *
   * @see PlanAttribute#refEquals(PlanAttribute)
   */
  default PlanAttribute resolveAttribute(PlanAttribute attr) {
    for (PlanAttribute out : outputAttributes()) if (out.refEquals(attr)) return out;
    return null;
  }

  default void replacePredecessor(PlanNode target, PlanNode rep) {
    final PlanNode[] predecessors = predecessors();
    for (int i = 0; i < predecessors.length; i++)
      if (predecessors[i] == target) {
        setPredecessor(i, rep);
        break;
      }
  }
}
