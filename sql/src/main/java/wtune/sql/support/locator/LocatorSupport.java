package wtune.sql.support.locator;

import wtune.sql.ast.*;

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

  static SqlNodes gatherSimpleSources(SqlNode root) {
    return nodeLocator().accept(TableSourceKind.SimpleSource).scoped().gather(root);
  }
}
