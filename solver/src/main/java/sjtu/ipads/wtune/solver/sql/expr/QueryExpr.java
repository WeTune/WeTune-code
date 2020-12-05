package sjtu.ipads.wtune.solver.sql.expr;

import sjtu.ipads.wtune.solver.core.Constraint;
import sjtu.ipads.wtune.solver.core.SolverContext;
import sjtu.ipads.wtune.solver.core.SymbolicColumnRef;
import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.sql.ColumnRef;

import java.util.List;

public interface QueryExpr extends Expr {
  AlgNode query();

  @Override
  default ColumnRef asColumn(List<AlgNode> inputs, List<String> alias, List<ColumnRef> refs) {
    return query().columns().get(0);
  }

  @Override
  default SymbolicColumnRef asVariable(
      List<AlgNode> inputs, List<String> alias, List<SymbolicColumnRef> refs, SolverContext ctx) {
    return query().projected().get(0);
  }

  @Override
  default Constraint asConstraint(
      List<AlgNode> inputs, List<String> alias, List<SymbolicColumnRef> refs, SolverContext ctx) {
    throw new UnsupportedOperationException();
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
