package sjtu.ipads.wtune.systhesis.exprlist;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.analyzer.NodeFinder;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.systhesis.operators.DropOrderBy;

import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_ORDER_BY;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public class ReduceSubqueryOrderBy implements ExprListMutator {
  private final SQLNode target;

  public ReduceSubqueryOrderBy(SQLNode target) {
    this.target = target;
  }

  public static boolean canReduceOrderBy(SQLNode node) {
    final QueryScope scope = node.get(RESOLVED_QUERY_SCOPE);
    return scope.parent() != null && scope.queryNode().get(QUERY_ORDER_BY) != null;
  }

  @Override
  public SQLNode target() {
    return target;
  }

  @Override
  public SQLNode modifyAST(SQLNode root) {
    DropOrderBy.build().apply(NodeFinder.find(root, target));
    return root;
  }
}
