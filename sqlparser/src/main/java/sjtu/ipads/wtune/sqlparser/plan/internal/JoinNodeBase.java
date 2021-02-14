package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.OutputAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.common.utils.Commons.listConcatView;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.BINARY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.collectColumnRef;

public abstract class JoinNodeBase extends PlanNodeBase implements JoinNode {
  private final ASTNode onCondition;
  private final List<ASTNode> columnRefs;
  private final boolean isNormalForm;

  protected JoinNodeBase(ASTNode onCondition) {
    this.onCondition = onCondition;
    this.columnRefs = collectColumnRef(onCondition);
    this.isNormalForm = isNormalForm(onCondition);
  }

  @Override
  public ASTNode onCondition() {
    if (isNormalForm) {
      final List<OutputAttribute> leftKeys = leftAttributes(), rightKeys = rightAttributes();
      if (leftKeys.size() != rightKeys.size()) System.err.println("Mismatched join keys");

      ASTNode onCondition = null;
      for (int i = 0, bound = Math.min(leftKeys.size(), rightKeys.size()); i < bound; i++) {
        final ASTNode binary = ASTNode.expr(BINARY);
        binary.set(BINARY_OP, BinaryOp.EQUAL);
        binary.set(BINARY_LEFT, leftKeys.get(i).toColumnRef());
        binary.set(BINARY_RIGHT, rightKeys.get(i).toColumnRef());

        if (onCondition == null) onCondition = binary;
        else {
          final ASTNode conjunction = ASTNode.expr(BINARY);
          conjunction.set(BINARY_OP, BinaryOp.AND);
          conjunction.set(BINARY_LEFT, onCondition);
          conjunction.set(BINARY_RIGHT, binary);
          onCondition = conjunction;
        }
      }

      return onCondition;

    } else {
      final ASTNode copy = onCondition.copy();

      final List<ASTNode> nodes = collectColumnRef(copy);
      final List<OutputAttribute> usedAttrs = usedAttributes0(nodes);

      for (int i = 0; i < usedAttrs.size(); i++) {
        final OutputAttribute usedAttr = usedAttrs.get(i);
        if (usedAttr != null) nodes.get(i).update(usedAttr.toColumnRef());
      }

      return copy;
    }
  }

  @Override
  public List<OutputAttribute> outputAttributes() {
    return listConcatView(
        predecessors()[0].outputAttributes(), predecessors()[1].outputAttributes());
  }

  @Override
  public List<OutputAttribute> leftAttributes() {
    return partialUsedAttributes0(columnRefs, predecessors()[0]);
  }

  @Override
  public List<OutputAttribute> rightAttributes() {
    return partialUsedAttributes0(columnRefs, predecessors()[1]);
  }

  @Override
  public List<OutputAttribute> usedAttributes() {
    return usedAttributes0(columnRefs);
  }

  @Override
  public boolean isNormalForm() {
    return isNormalForm;
  }

  private List<OutputAttribute> partialUsedAttributes0(List<ASTNode> nodes, PlanNode predecessor) {
    return nodes.stream()
        .map(predecessor::outputAttribute)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private List<OutputAttribute> usedAttributes0(List<ASTNode> nodes) {
    return listMap(this::outputAttribute, nodes);
  }

  private static boolean isNormalForm(ASTNode expr) {
    final BinaryOp op = expr.get(BINARY_OP);
    if (op == BinaryOp.AND) {
      return isNormalForm(expr.get(BINARY_LEFT)) && isNormalForm(expr.get(BINARY_RIGHT));

    } else if (op == BinaryOp.EQUAL) {
      return COLUMN_REF.isInstance(expr.get(BINARY_LEFT))
          && COLUMN_REF.isInstance(expr.get(BINARY_RIGHT));

    } else return false;
  }
}
