package sjtu.ipads.wtune.sqlparser.plan.internal;

import static sjtu.ipads.wtune.common.utils.Commons.listJoin;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_OP;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.BINARY;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.plan.AttributeDefBag.makeBag;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDefBag;
import sjtu.ipads.wtune.sqlparser.plan.JoinNode;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan.PlanException;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

public class JoinNodeImpl extends PlanNodeBase implements JoinNode {
  protected OperatorType type;
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
  protected List<AttributeDef> usedInLeft, usedInRight, used;

  protected boolean isASTUpdated;

  protected JoinNodeImpl(
      OperatorType type,
      ASTNode onCondition,
      List<AttributeDef> used,
      List<AttributeDef> usedInLeft,
      List<AttributeDef> usedInRight,
      boolean isNormalForm) {
    super(type);

    if (onCondition == null && used.isEmpty()) throw new IllegalArgumentException();

    this.type = type;
    this.onCondition = onCondition;
    this.used = used;
    this.usedInLeft = usedInLeft;
    this.usedInRight = usedInRight;

    this.isNormalForm = isNormalForm;
    this.isASTUpdated = false;
  }

  public static JoinNode build(OperatorType type, ASTNode onCondition) {
    if (!type.isJoin()) throw new IllegalArgumentException();
    return new JoinNodeImpl(
        type, onCondition, null, null, null, onCondition == null || isNormalForm(onCondition));
  }

  public static JoinNode build(
      OperatorType type, List<AttributeDef> left, List<AttributeDef> right) {
    if (!type.isJoin()) throw new IllegalArgumentException();
    if (left.size() != right.size()) throw new IllegalArgumentException();
    return new JoinNodeImpl(type, null, listJoin(left, right), left, right, true);
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
  public OperatorType type() {
    return type;
  }

  @Override
  public void setJoinType(OperatorType type) {
    if (!type.isJoin()) throw new IllegalArgumentException();
    this.type = type;
  }

  @Override
  public ASTNode onCondition() {
    if (isASTUpdated) return onCondition.deepCopy();
    isASTUpdated = true;

    if (isNormalForm) {
      // if normal formed, reconstruct the ON-condition from scratch
      final List<AttributeDef> leftKeys = leftAttributes(), rightKeys = rightAttributes();

      if (leftKeys.size() != rightKeys.size()) throw new PlanException("Mismatched join keys");

      // conjunct `left = right` pair-wisely
      ASTNode onCondition = null;
      for (int i = 0, bound = leftKeys.size(); i < bound; i++) {
        final ASTNode binary = ASTNode.expr(BINARY);
        binary.set(BINARY_OP, BinaryOp.EQUAL);
        binary.set(BINARY_LEFT, leftKeys.get(i).makeColumnRef());
        binary.set(BINARY_RIGHT, rightKeys.get(i).makeColumnRef());

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
      updateColumnRefs(gatherColumnRefs(copy), usedAttributes());

      return this.onCondition = copy;
    }
  }

  @Override
  public List<AttributeDef> leftAttributes() {
    return usedInLeft;
  }

  @Override
  public List<AttributeDef> rightAttributes() {
    return usedInRight;
  }

  @Override
  public boolean isNormalForm() {
    return isNormalForm;
  }

  @Override
  public List<AttributeDef> usedAttributes() {
    return used;
  }

  @Override
  public AttributeDefBag definedAttributes() {
    final PlanNode leftInput = predecessors()[0], rightInput = predecessors()[1];
    return makeBag(listJoin(leftInput.definedAttributes(), rightInput.definedAttributes()));
  }

  @Override
  protected PlanNode copy0() {
    return new JoinNodeImpl(type, onCondition, used, usedInLeft, usedInRight, isNormalForm);
  }

  @Override
  public void resolveUsed() {
    final List<AttributeDef> oldUsed = this.used;

    final List<AttributeDef> used;
    final List<AttributeDef> usedInLeft;
    final List<AttributeDef> usedInRight;
    if (oldUsed == null) {
      assert onCondition != null;

      final List<ASTNode> colRefs = gatherColumnRefs(onCondition);
      used = new ArrayList<>(colRefs.size());
      usedInLeft = new ArrayList<>(colRefs.size());
      usedInRight = new ArrayList<>(colRefs.size());
      doResolve(colRefs, AttributeDefBag::locate, used, usedInLeft, usedInRight);

    } else {
      used = new ArrayList<>(oldUsed.size());
      usedInLeft = new ArrayList<>(oldUsed.size());
      usedInRight = new ArrayList<>(oldUsed.size());
      doResolve(oldUsed, AttributeDefBag::locate, used, usedInLeft, usedInRight);
    }

    if (isNormalForm && usedInLeft.size() != usedInRight.size())
      throw new PlanException("Mismatched join keys");

    this.used = used;
    this.usedInLeft = usedInLeft;
    this.usedInRight = usedInRight;
    this.isASTUpdated = false;
  }

  private <T> void doResolve(
      List<T> targets,
      BiFunction<AttributeDefBag, T, Integer> resolutionFunc,
      List<AttributeDef> used,
      List<AttributeDef> left,
      List<AttributeDef> right) {
    final AttributeDefBag inAttrs = definedAttributes();
    final int boundary = predecessors()[0].definedAttributes().size();

    for (T target : targets) {
      final int index = resolutionFunc.apply(inAttrs, target);
      if (index == -1) throw new PlanException();

      final AttributeDef attr = inAttrs.get(index);
      used.add(attr);
      if (index < boundary) left.add(attr);
      else right.add(attr);
    }
  }

  @Override
  public String toString() {
    final ASTNode onCondition = isASTUpdated ? this.onCondition : this.onCondition();
    return "%s<%s>".formatted(type(), onCondition);
  }
}
