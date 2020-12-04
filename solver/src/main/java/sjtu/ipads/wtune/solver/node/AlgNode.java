package sjtu.ipads.wtune.solver.node;

import sjtu.ipads.wtune.solver.core.SolverContext;
import sjtu.ipads.wtune.solver.core.SymbolicColumnRef;
import sjtu.ipads.wtune.solver.sql.ColumnRef;

import java.util.List;

public interface AlgNode {
  boolean FORMAT_BREAK_LINE = false;

  String namespace();

  AlgNode parent();

  List<AlgNode> inputs();

  List<ColumnRef> columns();

  List<SymbolicColumnRef> filtered();

  List<SymbolicColumnRef> projected();

  String toString(int indentLevel);

  AlgNode setNamespace(String namespace);

  AlgNode setParent(AlgNode parent);

  AlgNode setSolverContext(SolverContext context);
}
