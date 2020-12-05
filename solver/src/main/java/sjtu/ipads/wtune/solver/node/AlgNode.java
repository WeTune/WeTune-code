package sjtu.ipads.wtune.solver.node;

import sjtu.ipads.wtune.solver.core.SolverContext;
import sjtu.ipads.wtune.solver.core.SymbolicColumnRef;
import sjtu.ipads.wtune.solver.sql.ColumnRef;

import java.util.List;
import java.util.Set;

public interface AlgNode {
  boolean FORMAT_BREAK_LINE = false;

  String namespace();

  List<AlgNode> inputs();

  List<ColumnRef> columns();

  List<SymbolicColumnRef> filtered();

  List<SymbolicColumnRef> projected();

  Set<Set<SymbolicColumnRef>> uniqueCores();

  boolean isForcedUnique();

  boolean isSingletonOutput();

  List<SymbolicColumnRef> orderKeys();

  AlgNode setNamespace(String namespace);

  AlgNode setSolverContext(SolverContext context);

  String toString(int indentLevel);

  default Iterable<AlgNode> inputsAndSubquery() {
    return inputs();
  }

  default boolean isInferredUnique() {
    return isSingletonOutput() || !uniqueCores().isEmpty();
  }
}
