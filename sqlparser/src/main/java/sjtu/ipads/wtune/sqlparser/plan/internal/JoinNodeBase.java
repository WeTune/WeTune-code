package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ASTContext;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.InnerJoinNode;
import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.LeftJoinNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.System.Logger.Level.WARNING;
import static sjtu.ipads.wtune.common.utils.Commons.listJoin;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.BINARY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.plan.internal.DerivedAttributeDef.fastEquals;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

public abstract class JoinNodeBase extends PlanNodeBase implements JoinNode {
  protected ASTNode onCondition;
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
  protected List<AttributeDef> left, right, used;

  private boolean dirty = true;

  protected JoinNodeBase(
      ASTNode onCondition,
      List<AttributeDef> used,
      List<AttributeDef> left,
      List<AttributeDef> right) {
    if (onCondition == null && used == null) throw new IllegalArgumentException();

    if (onCondition != null) onCondition = ASTContext.unmanage(onCondition.deepCopy());

    this.onCondition = onCondition;
    this.used = used;
    this.left = left;
    this.right = right;
    this.isNormalForm = onCondition == null || isNormalForm(onCondition);
  }

  @Override
  public ASTNode onCondition() {
    if (!dirty) return onCondition.deepCopy();
    dirty = false;

    if (isNormalForm) {
      // if normal formed, reconstruct the ON-condition from scratch
      final List<AttributeDef> leftKeys = leftAttributes(), rightKeys = rightAttributes();

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

      return this.onCondition = onCondition;

    } else {
      // otherwise, copy the original expression (and rectify column refs)
      final ASTNode copy = onCondition.deepCopy();
      updateColumnRefs(gatherColumnRefs(copy), used);

      return this.onCondition = copy;
    }
  }

  @Override
  public List<AttributeDef> definedAttributes() {
    return listJoin(predecessors()[0].definedAttributes(), predecessors()[1].definedAttributes());
  }

  @Override
  public boolean isNormalForm() {
    return isNormalForm;
  }

  @Override
  public List<AttributeDef> leftAttributes() {
    return left;
  }

  @Override
  public List<AttributeDef> rightAttributes() {
    return right;
  }

  @Override
  public List<AttributeDef> usedAttributes() {
    return used;
  }

  @Override
  public void resolveUsed() {
    if (used == null) {
      used = resolveUsed0(gatherColumnRefs(onCondition), this);
      left = listFilter(Objects::nonNull, resolveUsed1(used, predecessors()[0]));
      right = listFilter(Objects::nonNull, resolveUsed1(used, predecessors()[1]));

    } else {
      // efficiency-critical part, so we have do some ugly things
      final List<AttributeDef> inAttrs = definedAttributes();
      final int boundary = predecessors()[0].definedAttributes().size();

      final List<AttributeDef> newUsed = new ArrayList<>(used.size());
      final List<AttributeDef> newLeft = new ArrayList<>(left.size());
      final List<AttributeDef> newRight = new ArrayList<>(right.size());

      outer:
      for (AttributeDef usedAttr : used) {
        // fast path
        for (int i = 0; i < inAttrs.size(); i++) {
          final AttributeDef resolved = inAttrs.get(i);
          if (fastEquals(usedAttr, resolved)) {
            newUsed.add(usedAttr);
            if (i < boundary) newLeft.add(resolved);
            else newRight.add(resolved);
            continue outer;
          }
        }

        // slow path, rare cases
        for (int i = 0; i < inAttrs.size(); i++) {
          final AttributeDef resolved = inAttrs.get(i);
          if (usedAttr.equals(resolved)) {
            newUsed.add(usedAttr);
            if (i < boundary) newLeft.add(resolved);
            else newRight.add(resolved);
          }
        }
      }

      used = newUsed;
      left = newLeft;
      right = newRight;
    }

    assert !isNormalForm || left.size() == right.size();

    dirty = true;
  }

  @Override
  public InnerJoinNode toInnerJoin() {
    if (this instanceof InnerJoinNode) return (InnerJoinNode) this;
    else return new InnerJoinNodeImpl(onCondition, used, left, right);
  }

  @Override
  public LeftJoinNode toLeftJoin() {
    if (this instanceof LeftJoinNode) return (LeftJoinNode) this;
    else return new LeftJoinNodeImpl(onCondition, used, left, right);
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

  @Override
  public String toString() {
    return "%s<%s>".formatted(type(), onCondition());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    JoinNodeBase that = (JoinNodeBase) o;
    return Objects.equals(onCondition, that.onCondition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.getClass(), onCondition);
  }
}
