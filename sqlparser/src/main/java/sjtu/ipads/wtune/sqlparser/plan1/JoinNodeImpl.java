package sjtu.ipads.wtune.sqlparser.plan1;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_LEFT;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_OP;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_RIGHT;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

class JoinNodeImpl extends PlanNodeBase implements JoinNode {
  private final OperatorType type;
  private final Expr condition;
  private final boolean isEquiJoin;

  private RefBag lhsRefs, rhsRefs;

  private JoinNodeImpl(
      OperatorType type, Expr condition, boolean isEquiJoin, RefBag lhsRefs, RefBag rhsRefs) {
    super(type);
    this.type = type;
    this.condition = condition;
    this.isEquiJoin = isEquiJoin;
    this.lhsRefs = lhsRefs;
    this.rhsRefs = rhsRefs;
  }

  private JoinNodeImpl(OperatorType type, Expr condition, boolean isEquiJoin) {
    this(type, condition, isEquiJoin, null, null);
  }

  private JoinNodeImpl(OperatorType type, Expr condition) {
    this(type, condition, condition != null && isEquiCond(condition.template()));
  }

  static JoinNode build(OperatorType joinType, ASTNode condition) {
    if (!joinType.isJoin()) throw new IllegalArgumentException("not a join type: " + joinType);
    return new JoinNodeImpl(joinType, condition == null ? null : ExprImpl.build(condition));
  }

  static JoinNode build(OperatorType joinType, RefBag lhsRefs, RefBag rhsRefs) {
    if (!joinType.isJoin()) throw new IllegalArgumentException("not a join type: " + joinType);
    return new JoinNodeImpl(
        joinType, ExprImpl.buildEquiCond(lhsRefs, rhsRefs), true, lhsRefs, rhsRefs);
  }

  @Override
  public OperatorType type() {
    return type;
  }

  @Override
  public boolean isEquiJoin() {
    return isEquiJoin;
  }

  @Override
  public Expr condition() {
    return condition;
  }

  @Override
  public RefBag lhsRefs() {
    if (lhsRefs == null) throw new IllegalStateException("LHS refs have not been set");
    return lhsRefs;
  }

  @Override
  public RefBag rhsRefs() {
    if (rhsRefs == null) throw new IllegalStateException("RHS refs have not been set");
    return rhsRefs;
  }

  @Override
  public RefBag refs() {
    return condition == null ? RefBag.empty() : condition.refs();
  }

  @Override
  public ValueBag values() {
    return ValueBag.empty();
  }

  @Override
  public void setLhsRefs(RefBag lhsRefs) {
    if (!isEquiJoin) throw new IllegalStateException("LHS refs is non-sense for non-equi join");
    if (this.lhsRefs != null) throw new IllegalStateException("LHS ref is immutable once set");
    this.lhsRefs = requireNonNull(lhsRefs);
  }

  @Override
  public void setRhsRefs(RefBag rhsRefs) {
    if (!isEquiJoin) throw new IllegalStateException("RHS refs is non-sense for non-equi join");
    if (this.rhsRefs != null) throw new IllegalStateException("RHS ref is immutable once set");
    this.rhsRefs = requireNonNull(rhsRefs);
  }

  @Override
  protected PlanNode copy0(PlanContext ctx) {
    checkContextSet();

    final JoinNode copy = new JoinNodeImpl(type, condition, isEquiJoin, lhsRefs, rhsRefs);
    copy.setContext(ctx);

    ctx.registerRefs(copy, refs());
    for (Ref ref : refs()) ctx.setRef(ref, this.context.deRef(ref));

    return copy;
  }

  private static boolean isEquiCond(ASTNode expr) {
    final BinaryOp op = expr.get(BINARY_OP);
    if (op == BinaryOp.AND) {
      return isEquiCond(expr.get(BINARY_LEFT)) && isEquiCond(expr.get(BINARY_RIGHT));

    } else if (op == BinaryOp.EQUAL) {
      return COLUMN_REF.isInstance(expr.get(BINARY_LEFT))
          && COLUMN_REF.isInstance(expr.get(BINARY_RIGHT));

    } else return false;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder(type.toString());
    builder.append('{').append(condition);

    final RefBag refs = refs();
    if (!refs.isEmpty()) {
      builder.append(",refs=");
      if (context == null) builder.append(refs);
      else builder.append(context.deRef(refs));
    }

    builder.append('}');
    if (predecessors[0] != null && predecessors[1] != null)
      builder.append('(').append(predecessors[0]).append(',').append(predecessors[1]).append(')');

    return builder.toString();
  }
}
