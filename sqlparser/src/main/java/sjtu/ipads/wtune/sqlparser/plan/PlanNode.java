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

  void setPredecessor(int idx, PlanNode op);

  void setSuccessor(PlanNode successor);

  List<OutputAttribute> outputAttributes();

  List<OutputAttribute> usedAttributes();

  default OutputAttribute outputAttribute(String qualification, String name) {
    for (OutputAttribute attr : outputAttributes())
      if (attr.qualification().equals(qualification) && attr.name().equals(name)) return attr;
    return null;
  }

  default OutputAttribute outputAttribute(ASTNode columnRef) {
    if (!COLUMN_REF.isInstance(columnRef)) throw new IllegalArgumentException();
    final ASTNode colName = columnRef.get(COLUMN_REF_COLUMN);
    return outputAttribute(colName.get(COLUMN_NAME_TABLE), colName.get(COLUMN_NAME_COLUMN));
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
