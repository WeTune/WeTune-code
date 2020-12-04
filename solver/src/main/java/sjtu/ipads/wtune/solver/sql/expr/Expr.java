package sjtu.ipads.wtune.solver.sql.expr;

import sjtu.ipads.wtune.solver.core.Constraint;
import sjtu.ipads.wtune.solver.core.SolverContext;
import sjtu.ipads.wtune.solver.core.SymbolicColumnRef;
import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.sql.ColumnRef;
import sjtu.ipads.wtune.solver.sql.Operator;
import sjtu.ipads.wtune.solver.sql.impl.BinaryExprImpl;
import sjtu.ipads.wtune.solver.sql.impl.ConstExprImpl;

import java.util.List;

public interface Expr {
  ColumnRef asColumn(List<AlgNode> inputs, List<String> alias, List<ColumnRef> refs);

  SymbolicColumnRef asVariable(
      List<AlgNode> inputs, List<String> alias, List<SymbolicColumnRef> refs, SolverContext ctx);

  Constraint asConstraint(
      List<AlgNode> inputs, List<String> alias, List<SymbolicColumnRef> refs, SolverContext ctx);

  Expr compile(List<AlgNode> inputs, List<String> alias);

  boolean checkValid();

  boolean isPredicate();

  static BinaryExpr binary(Expr left, Operator op, Expr right) {
    return BinaryExprImpl.create(left, op, right);
  }

  static ConstExpr const_(Object value) {
    return ConstExprImpl.create(value);
  }
}
