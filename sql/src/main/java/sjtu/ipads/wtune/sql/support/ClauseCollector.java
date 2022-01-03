package sjtu.ipads.wtune.sql.support;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.common.field.FieldKey;
import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.ast1.SqlNodes;
import sjtu.ipads.wtune.sql.ast1.SqlVisitor;

public abstract class ClauseCollector implements SqlVisitor {
  private static final int STOP = 1, ACCEPT = 2, NOT_ACCEPT = 0;

  private final TIntList nodes;

  protected ClauseCollector() {
    this.nodes = new TIntArrayList();
  }

  protected ClauseCollector(int expectedNumNodes) {
    this.nodes = new TIntArrayList(expectedNumNodes);
  }

  @Override
  public boolean enterChild(SqlNode parent, FieldKey<SqlNode> key, SqlNode child) {
    final int result = check(key);
    if ((result & ACCEPT) == ACCEPT) nodes.add(child.nodeId());
    return (result & STOP) != STOP;
  }

  @Override
  public boolean enterChildren(SqlNode parent, FieldKey<SqlNodes> key, SqlNodes child) {
    final int result = check(key);
    if ((result & ACCEPT) == ACCEPT) nodes.addAll(child.nodeIds());
    return (result & STOP) != STOP;
  }

  protected abstract int check(FieldKey<?> clause);

  public static SqlNodes collect(SqlNode root, FieldKey<?> target) {
    final ClauseCollector collector =
        new ClauseCollector() {
          @Override
          protected int check(FieldKey<?> clause) {
            return clause == target ? ACCEPT : NOT_ACCEPT;
          }
        };
    root.accept(collector);
    return SqlNodes.mk(root.context(), collector.nodes);
  }
}
