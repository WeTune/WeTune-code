package sjtu.ipads.wtune.stmt.utils;

import sjtu.ipads.wtune.sql.ast.ASTNode;
import sjtu.ipads.wtune.sql.ast.ASTVistor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class Collector implements ASTVistor {
  private final List<ASTNode> filtered = new ArrayList<>();
  private final Predicate<ASTNode> filter;
  private final boolean earlyStop;

  protected Collector(Predicate<ASTNode> filter, boolean earlyStop) {
    this.filter = filter;
    this.earlyStop = earlyStop;
  }

  @Override
  public boolean enter(ASTNode node) {
    if (filter.test(node)) {
      filtered.add(node);
      return !earlyStop;
    } else return true;
  }

  public static List<ASTNode> collect(ASTNode node, Predicate<ASTNode> filter) {
    return collect(node, filter, true);
  }

  public static List<ASTNode> collect(ASTNode node, Predicate<ASTNode> filter, boolean earlyStop) {
    final Collector collector = new Collector(filter, earlyStop);
    node.accept(collector);
    return collector.filtered;
  }
}
