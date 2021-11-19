package sjtu.ipads.wtune.sqlparser.util;

import sjtu.ipads.wtune.sqlparser.ast1.SqlNode;
import sjtu.ipads.wtune.sqlparser.ast1.SqlVisitor;

import java.util.ArrayList;
import java.util.List;

public final class ColRefGatherer implements SqlVisitor {
  private final List<SqlNode> nodes;
  private final boolean isSubqueryIncluded;

  public ColRefGatherer(boolean isSubqueryIncluded) {
    this.nodes = new ArrayList<>();
    this.isSubqueryIncluded = isSubqueryIncluded;
  }

  @Override
  public boolean enterColumnRef(SqlNode columnRef) {
    nodes.add(columnRef);
    return false;
  }

  @Override
  public boolean enterQuery(SqlNode query) {
    return isSubqueryIncluded;
  }

  public List<SqlNode> gather(SqlNode root) {
    root.accept(this);
    return nodes;
  }

  public static List<SqlNode> gatherColRefs(Iterable<SqlNode> roots) {
    final ColRefGatherer collector = new ColRefGatherer(false);
    for (SqlNode root : roots) collector.gather(root);
    return collector.nodes;
  }

  public static List<SqlNode> gatherColRefs(SqlNode root) {
    return new ColRefGatherer(false).gather(root);
  }
}
