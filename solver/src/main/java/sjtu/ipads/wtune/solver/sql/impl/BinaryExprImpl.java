package sjtu.ipads.wtune.solver.sql.impl;

import sjtu.ipads.wtune.solver.core.Constraint;
import sjtu.ipads.wtune.solver.core.SolverContext;
import sjtu.ipads.wtune.solver.core.SymbolicColumnRef;
import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.sql.ColumnRef;
import sjtu.ipads.wtune.solver.sql.Operator;
import sjtu.ipads.wtune.solver.sql.expr.BinaryExpr;
import sjtu.ipads.wtune.solver.sql.expr.Expr;
import sjtu.ipads.wtune.solver.sql.expr.ExprVisitor;

import java.util.List;

public class BinaryExprImpl implements BinaryExpr {
  private Expr left;
  private final Operator operator;
  private Expr right;

  private BinaryExprImpl(Expr left, Operator operator, Expr right) {
    this.left = left;
    this.right = right;
    this.operator = operator;
  }

  public static BinaryExpr create(Expr left, Operator operator, Expr right) {
    return new BinaryExprImpl(left, operator, right);
  }

  @Override
  public ColumnRef asColumn(List<AlgNode> inputs, List<String> aliases, List<ColumnRef> refs) {
    throw new UnsupportedOperationException(); // TODO
  }

  @Override
  public SymbolicColumnRef asVariable(
      List<AlgNode> inputs, List<String> aliases, List<SymbolicColumnRef> refs, SolverContext ctx) {
    if (!checkValid()) throw new IllegalStateException("invalid expression " + toString());

    final SymbolicColumnRef leftVar = left.asVariable(inputs, aliases, refs, ctx);
    final SymbolicColumnRef rightVar = right.asVariable(inputs, aliases, refs, ctx);

    final Constraint notNull = ctx.and(leftVar.notNull(), rightVar.notNull());

    switch (operator) {
      case AND:
        return SymbolicColumnRef.create(
            ctx.and(ctx.convert(leftVar.variable()), ctx.convert(rightVar.variable())), notNull);
      case OR:
        return SymbolicColumnRef.create(
            ctx.or(ctx.convert(leftVar.variable()), ctx.convert(rightVar.variable())), notNull);
      case EQ:
      case IN_SUB:
        return SymbolicColumnRef.create(ctx.eq(leftVar.variable(), rightVar.variable()), notNull);
      default:
        throw new IllegalStateException("should not reach here");
    }
  }

  @Override
  public Constraint asConstraint(
      List<AlgNode> inputs, List<String> aliases, List<SymbolicColumnRef> refs, SolverContext ctx) {
    final SymbolicColumnRef variable = asVariable(inputs, aliases, refs, ctx);
    final Constraint c = ctx.convert(variable.variable());

    if (c == null) throw new IllegalArgumentException("not a predicate " + toString());

    return ctx.and(c, variable.notNull());
  }

  @Override
  public Expr compile(List<AlgNode> inputs, List<String> alias) {
    left = left.compile(inputs, alias);
    right = right.compile(inputs, alias);
    return this;
  }

  @Override
  public boolean checkValid() {
    return isPredicate() || operator.isCmp();
  }

  @Override
  public boolean isPredicate() {
    return ((operator().isLogic() && left().isPredicate() && right().isPredicate())
        || (operator().isCmp()));
  }

  @Override
  public Expr left() {
    return left;
  }

  @Override
  public Expr right() {
    return right;
  }

  @Override
  public Operator operator() {
    return operator;
  }

  @Override
  public void acceptVisitor(ExprVisitor visitor) {
    visitor.visit(this);
    left.acceptVisitor(visitor);
    right.acceptVisitor(visitor);
  }

  @Override
  public String toString() {
    return left() + " " + operator.literal() + " " + right();
  }
}
