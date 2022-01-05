package sjtu.ipads.wtune.sql.support.locator;

import sjtu.ipads.wtune.sql.ast1.SqlContext;
import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.ast1.SqlNodes;

import static sjtu.ipads.wtune.sql.ast1.ExprKind.ColRef;

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
    return nodeLocator().accept(ColRef).scoped().gather(root);
  }

  static SqlNodes gatherColRefs(Iterable<SqlNode> roots) {
    final SqlGatherer gatherer = nodeLocator().accept(ColRef).scoped().gatherer();
    SqlContext ctx = null;
    for (SqlNode root : roots) {
      if (ctx == null) ctx = root.context();
      gatherer.gather(root);
    }
    return ctx == null ? SqlNodes.mkEmpty() : SqlNodes.mk(ctx, gatherer.nodeIds());
  }
}
