package sjtu.ipads.wtune.solver.sql.impl;

import sjtu.ipads.wtune.solver.core.SolverContext;
import sjtu.ipads.wtune.solver.core.SymbolicColumnRef;
import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.schema.DataType;
import sjtu.ipads.wtune.solver.sql.ColumnRef;
import sjtu.ipads.wtune.solver.sql.ProjectionItem;
import sjtu.ipads.wtune.solver.sql.expr.Expr;

import java.util.List;

public class ProjectionItemImpl implements ProjectionItem {
  private final Expr expr;
  private final String alias;

  private ProjectionItemImpl(Expr expr, String alias) {
    this.expr = expr;
    this.alias = alias;
  }

  public static ProjectionItemImpl create(Expr expr, String alias) {
    return new ProjectionItemImpl(expr, alias);
  }

  @Override
  public DataType dataType() {
    return null;
  }

  @Override
  public boolean notNull() {
    return false;
  }

  @Override
  public String alias() {
    return alias;
  }

  @Override
  public ColumnRef projectOn(List<AlgNode> inputs, List<String> aliases, List<ColumnRef> refs) {
    final ColumnRef ref = expr.asColumn(inputs, aliases, refs).copy();
    return alias == null ? ref : ref.setAlias(alias);
  }

  @Override
  public SymbolicColumnRef projectOn(
      List<AlgNode> inputs, List<String> aliases, List<SymbolicColumnRef> refs, SolverContext ctx) {
    return expr.asVariable(inputs, aliases, refs, ctx);
  }
}
