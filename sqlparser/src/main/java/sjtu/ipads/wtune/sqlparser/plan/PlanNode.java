package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;

public interface PlanNode {
  OperatorType type();

  PlanNode successor();

  PlanNode[] predecessors();

  List<OutputAttribute> outputAttributes();

  List<OutputAttribute> usedAttributes();

  void setPredecessor(int idx, PlanNode op);

  void setSuccessor(PlanNode successor);

  PlanNode copy();

  void resolveUsedAttributes();

  default OutputAttribute resolveAttribute(String qualification, String name) {
    for (OutputAttribute attr : outputAttributes())
      if (attr.qualification().equals(qualification) && attr.name().equals(name)) return attr;
    return null;
  }

  default OutputAttribute resolveAttribute(ASTNode columnRef) {
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
   * @see OutputAttribute#refEquals(OutputAttribute)
   */
  default OutputAttribute resolveAttribute(OutputAttribute attr) {
    for (OutputAttribute out : outputAttributes())
      if (out.refEquals(attr))
        return out;
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
