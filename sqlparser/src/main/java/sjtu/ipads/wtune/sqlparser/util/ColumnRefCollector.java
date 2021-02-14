package sjtu.ipads.wtune.sqlparser.util;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTVistor;

import java.util.ArrayList;
import java.util.List;

public final class ColumnRefCollector implements ASTVistor {
  private final List<ASTNode> nodes = new ArrayList<>();

  @Override
  public boolean enterColumnRef(ASTNode columnRef) {
    nodes.add(columnRef);
    return false;
  }

  @Override
  public boolean enterQuery(ASTNode query) {
    return false;
  }

  public static List<ASTNode> collectColumnRef(ASTNode... roots) {
    final ColumnRefCollector collector = new ColumnRefCollector();
    for (ASTNode root : roots) root.accept(collector);
    return collector.nodes;
  }
}
