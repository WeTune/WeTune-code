package sjtu.ipads.wtune.solver.sql.expr;

import sjtu.ipads.wtune.solver.core.Constraint;
import sjtu.ipads.wtune.solver.core.SolverContext;
import sjtu.ipads.wtune.solver.core.SymbolicColumnRef;
import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.sql.ColumnRef;
import sjtu.ipads.wtune.solver.sql.impl.IndexInputRefImpl;
import sjtu.ipads.wtune.solver.sql.impl.NameInputRefImpl;

import java.util.List;

public interface InputRef extends Expr {
  IndexInputRef locateIn(List<AlgNode> inputs, List<String> aliases);

  default ColumnRef getCol(List<AlgNode> inputs, List<String> aliases, List<ColumnRef> refs) {
    return locateIn(inputs, aliases).getCol(refs);
  }

  default SymbolicColumnRef getSymCol(
      List<AlgNode> inputs, List<String> aliases, List<SymbolicColumnRef> refs) {
    return locateIn(inputs, aliases).getSymCol(refs);
  }

  @Override
  default ColumnRef asColumn(List<AlgNode> inputs, List<String> alias, List<ColumnRef> refs) {
    return getCol(inputs, alias, refs);
  }

  @Override
  default SymbolicColumnRef asVariable(
      List<AlgNode> inputs, List<String> alias, List<SymbolicColumnRef> refs, SolverContext ctx) {
    return getSymCol(inputs, alias, refs);
  }

  @Override
  default Constraint asConstraint(
      List<AlgNode> inputs, List<String> alias, List<SymbolicColumnRef> refs, SolverContext ctx) {
    throw new UnsupportedOperationException();
  }

  @Override
  default Expr compile(List<AlgNode> inputs, List<String> alias) {
    return locateIn(inputs, alias);
  }

  @Override
  default boolean checkValid() {
    return true;
  }

  @Override
  default boolean isPredicate() {
    return false;
  }

  static NameInputRef ref(String name) {
    return NameInputRefImpl.create(name);
  }

  static IndexInputRef ref(int index) {
    return IndexInputRefImpl.create(index);
  }
}
