package sjtu.ipads.wtune.superopt.util;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTVistor;
import sjtu.ipads.wtune.sqlparser.ast.FieldDomain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ASTNodeCollector implements ASTVistor {
  private final Predicate<ASTNode> filter;
  private final boolean earlyStop;
  private final List<ASTNode> nodes;

  private ASTNodeCollector(Predicate<ASTNode> filter, boolean earlyStop) {
    this.filter = filter;
    this.earlyStop = earlyStop;
    this.nodes = new ArrayList<>();
  }

  @Override
  public boolean enter(ASTNode node) {
    if (filter.test(node)) {
      nodes.add(node);
      return !earlyStop;
    }
    return true;
  }

  public void reset() {
    nodes.clear();
  }

  public List<ASTNode> nodes() {
    return nodes;
  }

  public List<ASTNode> collect(ASTNode node) {
    node.accept(this);
    return nodes;
  }

  public static ASTNodeCollector collectTypes(FieldDomain domain) {
    return new ASTNodeCollector(domain::isInstance, false);
  }
}
