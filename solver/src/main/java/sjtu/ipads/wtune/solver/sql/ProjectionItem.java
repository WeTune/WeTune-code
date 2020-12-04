package sjtu.ipads.wtune.solver.sql;

import sjtu.ipads.wtune.solver.core.SolverContext;
import sjtu.ipads.wtune.solver.core.SymbolicColumnRef;
import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.schema.DataType;
import sjtu.ipads.wtune.solver.sql.expr.Expr;
import sjtu.ipads.wtune.solver.sql.impl.ProjectionItemImpl;

import java.util.List;

public interface ProjectionItem {
  ColumnRef projectOn(List<AlgNode> inputs, List<String> aliases, List<ColumnRef> refs);

  SymbolicColumnRef projectOn(
      List<AlgNode> inputs, List<String> aliases, List<SymbolicColumnRef> refs, SolverContext ctx);

  String alias();

  DataType dataType();

  boolean notNull();

  static ProjectionItem create(Expr expr, String alias) {
    return ProjectionItemImpl.create(expr, alias);
  }
}
