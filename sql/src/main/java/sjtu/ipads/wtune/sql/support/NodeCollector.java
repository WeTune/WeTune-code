package sjtu.ipads.wtune.sql.support;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.ast1.SqlNodes;
import sjtu.ipads.wtune.sql.ast1.SqlVisitor;

import java.util.function.Predicate;

public abstract class NodeCollector implements SqlVisitor {
  public static final int STOP = 1, ACCEPT = 2, NOT_ACCEPT = 0;

  private final TIntList nodes;

  protected NodeCollector() {
    this.nodes = new TIntArrayList();
  }

  protected NodeCollector(int expectedNumNodes) {
    this.nodes = new TIntArrayList(expectedNumNodes);
  }

  public TIntList nodeIds() {
    return nodes;
  }

  @Override
  public boolean enter(SqlNode node) {
    final int result = check(node);
    if ((result & ACCEPT) == ACCEPT) nodes.add(node.nodeId());
    return (result & STOP) != STOP;
  }

  protected abstract int check(SqlNode node);

  public static SqlNodes collect(SqlNode root, Predicate<SqlNode> filter) {
    final NodeCollector collector =
        new NodeCollector() {
          @Override
          protected int check(SqlNode node) {
            return filter.test(node) ? ACCEPT : NOT_ACCEPT;
          }
        };
    root.accept(collector);
    return SqlNodes.mk(root.context(), collector.nodes);
  }

  public static SqlNode locate(SqlNode root, Predicate<SqlNode> filter) {
    final NodeCollector collector =
        new NodeCollector(1) {
          @Override
          protected int check(SqlNode node) {
            return filter.test(node) ? (ACCEPT | STOP) : NOT_ACCEPT;
          }
        };
    root.accept(collector);
    return collector.nodes.isEmpty() ? null : SqlNode.mk(root.context(), collector.nodes.get(0));
  }
}
