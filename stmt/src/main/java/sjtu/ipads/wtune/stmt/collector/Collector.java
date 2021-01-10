package sjtu.ipads.wtune.stmt.collector;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.SQLVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class Collector implements SQLVisitor {
  private final List<SQLNode> filtered = new ArrayList<>();
  private final Predicate<SQLNode> filter;
  private final boolean earlyStop;

  protected Collector(Predicate<SQLNode> filter, boolean earlyStop) {
    this.filter = filter;
    this.earlyStop = earlyStop;
  }

  @Override
  public boolean enter(SQLNode node) {
    if (filter.test(node)) {
      filtered.add(node);
      return !earlyStop;
    } else return true;
  }

  public static List<SQLNode> collect(SQLNode node, Predicate<SQLNode> filter) {
    return collect(node, filter, true);
  }

  public static List<SQLNode> collect(SQLNode node, Predicate<SQLNode> filter, boolean earlyStop) {
    final Collector collector = new Collector(filter, earlyStop);
    node.accept(collector);
    return collector.filtered;
  }
}
