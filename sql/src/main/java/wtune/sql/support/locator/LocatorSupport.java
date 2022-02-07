package wtune.sql.support.locator;

import wtune.sql.ast.SqlContext;
import wtune.sql.ast.SqlNode;
import wtune.sql.ast.SqlNodes;
import wtune.sql.ast.ExprKind;

public interface LocatorSupport {
  static NodeLocatorBuilder nodeLocator() {
    return new NodeLocatorBuilder();
  }

  static ClauseLocatorBuilder clauseLocator() {
    return new ClauseLocatorBuilder();
  }

  static PredicateLocatorBuilder predicateLocator() {
    return new PredicateLocatorBuilder();
  }

  static SqlNodes gatherColRefs(SqlNode root) {
    return nodeLocator().accept(ExprKind.ColRef).scoped().gather(root);
  }

  static SqlNodes gatherColRefs(Iterable<SqlNode> roots) {
    final SqlGatherer gatherer = nodeLocator().accept(ExprKind.ColRef).scoped().gatherer();
    SqlContext ctx = null;
    for (SqlNode root : roots) {
      if (ctx == null) ctx = root.context();
      gatherer.gather(root);
    }
    return ctx == null ? SqlNodes.mkEmpty() : SqlNodes.mk(ctx, gatherer.nodeIds());
  }
}
