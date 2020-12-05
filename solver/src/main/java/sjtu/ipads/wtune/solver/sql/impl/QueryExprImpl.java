package sjtu.ipads.wtune.solver.sql.impl;

import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.sql.expr.ExprVisitor;
import sjtu.ipads.wtune.solver.sql.expr.QueryExpr;

public class QueryExprImpl implements QueryExpr {
  private final AlgNode query;

  private QueryExprImpl(AlgNode query) {
    this.query = query;
  }

  public static QueryExpr create(AlgNode node) {
    return new QueryExprImpl(node);
  }

  @Override
  public AlgNode query() {
    return query;
  }

  @Override
  public void acceptVisitor(ExprVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return query.toString(2);
  }
}
