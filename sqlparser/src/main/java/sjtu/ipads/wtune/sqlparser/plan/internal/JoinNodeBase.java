package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ASTContext;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.OutputAttribute;

import java.util.List;
import java.util.Objects;

import static java.lang.System.Logger.Level.WARNING;
import static sjtu.ipads.wtune.common.utils.Commons.listConcatView;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.BINARY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.collectColumnRefs;

public abstract class JoinNodeBase extends PlanNodeBase implements JoinNode {
  protected final ASTNode onCondition;
  // Indicates if ON-condition conforms to the form "col = col [AND col = col ...]"
  //
  // If true, then we can only remember which columns are used, and reconstruct the
  // ON-condition even in the absence of AST (i.e. onCondition is null).
  // Otherwise, the AST is essential.
  //
  // Thus, the invariant always holds: !isNormalForm => onCondition != null
  protected final boolean isNormalForm;
  // `left` and `right` is used for normal-formed ON-condition.
  // `used` is used otherwise
  protected List<OutputAttribute> left, right, used;

  protected JoinNodeBase(
      ASTNode onCondition,
      List<OutputAttribute> used,
      List<OutputAttribute> left,
      List<OutputAttribute> right) {
    if (onCondition == null && used == null) throw new IllegalArgumentException();

    this.onCondition = onCondition;
    this.used = used;
    this.left = left;
    this.right = right;
    this.isNormalForm = onCondition == null || isNormalForm(onCondition);
  }

  @Override
  public ASTNode onCondition() {
    if (isNormalForm) {
      // if normal formed, reconstruct the ON-condition from scratch
      final List<OutputAttribute> leftKeys = leftAttributes(), rightKeys = rightAttributes();

      if (leftKeys.size() != rightKeys.size()) ASTContext.LOG.log(WARNING, "Mismatched join keys");

      // conjunct `left = right` pair-wisely
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
      // otherwise, copy the original expression (and rectify column refs)
      final ASTNode copy = onCondition.copy();

      final List<ASTNode> nodes = collectColumnRefs(copy);

      for (int i = 0; i < used.size(); i++) {
        final OutputAttribute usedAttr = used.get(i);
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
  public boolean isNormalForm() {
    return isNormalForm;
  }

  @Override
  public List<OutputAttribute> leftAttributes() {
    return left;
  }

  @Override
  public List<OutputAttribute> rightAttributes() {
    return right;
  }

  @Override
  public List<OutputAttribute> usedAttributes() {
    return used;
  }

  @Override
  public void resolveUsedAttributes() {
    if (used == null) used = resolveUsedAttributes0(collectColumnRefs(onCondition), this);
    else used = resolveUsedAttributes1(used, this);

    left = listFilter(Objects::nonNull, resolveUsedAttributes1(used, predecessors()[0]));
    right = listFilter(Objects::nonNull, resolveUsedAttributes1(used, predecessors()[1]));
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
