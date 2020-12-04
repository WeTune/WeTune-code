package sjtu.ipads.wtune.solver.sql.expr;

import sjtu.ipads.wtune.solver.core.Constraint;
import sjtu.ipads.wtune.solver.core.SolverContext;
import sjtu.ipads.wtune.solver.core.SymbolicColumnRef;
import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.schema.DataType;
import sjtu.ipads.wtune.solver.sql.ColumnRef;

import java.util.List;

public interface ConstExpr extends Expr {
  DataType dataType();

  boolean notNull();

  Object value();

  @Override
  default ColumnRef asColumn(List<AlgNode> inputs, List<String> alias, List<ColumnRef> refs) {
    return ColumnRef.ofExpr(this, null, dataType(), notNull());
  }

  @Override
  default SymbolicColumnRef asVariable(
      List<AlgNode> inputs, List<String> alias, List<SymbolicColumnRef> refs, SolverContext ctx) {
    return SymbolicColumnRef.create(ctx.const_(dataType(), value()), ctx.boolConst(notNull()));
  }

  @Override
  default Constraint asConstraint(
      List<AlgNode> inputs, List<String> alias, List<SymbolicColumnRef> refs, SolverContext ctx) {
    throw new UnsupportedOperationException();
  }

  @Override
  default Expr compile(List<AlgNode> inputs, List<String> alias) {
    return this;
  }

  @Override
  default boolean checkValid() {
    return true;
  }

  @Override
  default boolean isPredicate() {
    return false;
  }
}
