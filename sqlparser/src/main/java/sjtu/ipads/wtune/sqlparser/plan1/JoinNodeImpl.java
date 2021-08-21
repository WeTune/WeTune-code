package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.Commons.listJoin;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;

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

  static JoinNode mk(OperatorType joinType, ASTNode condition) {
    if (!joinType.isJoin()) throw new IllegalArgumentException("not a join type: " + joinType);
    return new JoinNodeImpl(joinType, condition == null ? null : ExprImpl.mk(condition));
  }

  static JoinNode mk(OperatorType joinType, RefBag lhsRefs, RefBag rhsRefs) {
    if (!joinType.isJoin()) throw new IllegalArgumentException("not a join type: " + joinType);
    return new JoinNodeImpl(
        joinType, ExprImpl.mkEquiCond(lhsRefs, rhsRefs), true, lhsRefs, rhsRefs);
  }

  @Override
  public OperatorType kind() {
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
    return ValueBag.mk(listJoin(predecessors[0].values(), predecessors[1].values()));
  }

  @Override
  public JoinNode flip(PlanContext ctx) {
    if (ctx != null) {
      checkContextSet();

      final JoinNodeImpl copy = new JoinNodeImpl(type, condition, isEquiJoin, rhsRefs, lhsRefs);
      copy.setContext(ctx);

      ctx.registerRefs(copy, refs());
      for (Ref ref : refs()) ctx.setRef(ref, this.context.deRef(ref));

      return copy;

    } else {
      final RefBag rhsRefs = this.rhsRefs;
      this.rhsRefs = this.lhsRefs;
      this.lhsRefs = rhsRefs;

      return this;
    }
  }

  @Override
  public void setLhsRefs(RefBag lhsRefs) {
    if (this.lhsRefs != null) throw new IllegalStateException("LHS ref is immutable once set");
    this.lhsRefs = requireNonNull(lhsRefs);
  }

  @Override
  public void setRhsRefs(RefBag rhsRefs) {
    if (this.rhsRefs != null) throw new IllegalStateException("RHS ref is immutable once set");
    this.rhsRefs = requireNonNull(rhsRefs);
  }

  @Override
  public PlanNode copy(PlanContext ctx) {
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
  public StringBuilder stringify(StringBuilder builder) {
    builder.append(kind().text()).append('{').append(condition);
    stringifyRefs(builder);
    builder.append('}');
    stringifyChildren(builder);
    return builder;
  }
}
