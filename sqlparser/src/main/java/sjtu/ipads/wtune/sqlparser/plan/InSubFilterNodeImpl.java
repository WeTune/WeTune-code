package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.Commons.listConcat;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.TUPLE;

class InSubFilterNodeImpl extends PlanNodeBase implements InSubFilterNode {
  private final Expr lhsExpr;
  private final RefBag lhsRefs;
  private Expr rhsExpr, predicate;
  private RefBag refs;

  InSubFilterNodeImpl(Expr lhsExpr) {
    this.lhsExpr = lhsExpr;
    this.lhsRefs = this.refs = lhsExpr.refs();
  }

  static InSubFilterNode mk(ASTNode node) {
    if (!COLUMN_REF.isInstance(node) && !TUPLE.isInstance(node))
      throw new IllegalArgumentException("invalid LHS expression for IN-Sub filter " + node);

    return new InSubFilterNodeImpl(ExprImpl.mk(node));
  }

  static InSubFilterNode mk(RefBag refs) {
    if (refs.isEmpty()) throw new IllegalArgumentException();
    return new InSubFilterNodeImpl(ExprImpl.mk(refs));
  }

  @Override
  public Expr predicate() {
    if (predicate == null)
      throw new IllegalStateException(
          "InSubFilter.predicate() cannot be called before setRhsExpr() called");

    return predicate;
  }

  @Override
  public ValueBag values() {
    return predecessors[0].values();
  }

  @Override
  public RefBag refs() {
    return refs;
  }

  @Override
  public RefBag lhsRefs() {
    return lhsRefs;
  }

  @Override
  public Expr lhsExpr() {
    return lhsExpr;
  }

  @Override
  public void setRhsExpr(Expr rhsExpr) {
    if (this.rhsExpr != null)
      throw new IllegalStateException("RHS expression of subquery filter is immutable once set");

    this.rhsExpr = requireNonNull(rhsExpr);

    // update refs
    if (!rhsExpr.refs().isEmpty()) this.refs = RefBag.mk(listConcat(lhsRefs, rhsExpr.refs()));

    // update predicate
    final ASTNode predicateExpr = ASTNode.expr(ExprKind.BINARY);
    predicateExpr.set(BINARY_OP, BinaryOp.IN_SUBQUERY);
    predicateExpr.set(BINARY_LEFT, lhsExpr.template().deepCopy());
    predicateExpr.set(BINARY_RIGHT, rhsExpr.template().deepCopy());
    this.predicate = new ExprImpl(refs, predicateExpr);
  }

  @Override
  public PlanNode copy(PlanContext ctx) {
    checkContextSet();

    final InSubFilterNode copy = new InSubFilterNodeImpl(lhsExpr);
    copy.setContext(ctx);

    ctx.registerRefs(copy, refs());
    for (Ref ref : refs()) ctx.setRef(ref, this.context.deRef(ref));

    return copy;
  }

  @Override
  public StringBuilder stringify0(StringBuilder builder) {
    builder.append("InSub{");
    stringifyRefs(builder);
    builder.append('}');
    stringifyChildren(builder);
    return builder;
  }
}
