package sjtu.ipads.wtune.solver.node;

import sjtu.ipads.wtune.solver.core.SolverContext;
import sjtu.ipads.wtune.solver.core.SymbolicColumnRef;
import sjtu.ipads.wtune.solver.sql.ColumnRef;

import java.util.List;
import java.util.Set;

public interface AlgNode {
  boolean FORMAT_BREAK_LINE = false;

  String namespace();

  AlgNode parent();

  List<AlgNode> inputs();

  List<ColumnRef> columns();

  List<SymbolicColumnRef> filtered();

  List<SymbolicColumnRef> projected();

  AlgNode setNamespace(String namespace);

  AlgNode setParent(AlgNode parent);

  AlgNode setSolverContext(SolverContext context);

  Set<Set<SymbolicColumnRef>> uniqueCores();

  List<SymbolicColumnRef> orderKeys();

  boolean isSingletonOutput();

  String toString(int indentLevel);

  default boolean isInferredUnique() {
    return isSingletonOutput() || !uniqueCores().isEmpty();
  }

  boolean isForcedUnique();
}
