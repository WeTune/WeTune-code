package sjtu.ipads.wtune.solver.sql.expr;

import sjtu.ipads.wtune.solver.core.SymbolicColumnRef;
import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.sql.ColumnRef;

import java.util.List;

public interface IndexInputRef extends InputRef {
  int index();

  @Override
  default IndexInputRef locateIn(List<AlgNode> inputs, List<String> aliases) {
    return this;
  }

  default ColumnRef getCol(List<ColumnRef> refs) {
    return refs.get(index());
  }

  default SymbolicColumnRef getSymCol(List<SymbolicColumnRef> refs) {
    return refs.get(index());
  }

  @Override
  default Expr compile(List<AlgNode> inputs, List<String> alias) {
    return this;
  }
}
