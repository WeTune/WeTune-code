package sjtu.ipads.wtune.sqlparser.util;

import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTVistor;

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

  public static List<ASTNode> gatherColumnRefs(Iterable<ASTNode> roots) {
    final ColumnRefCollector collector = new ColumnRefCollector();
    for (ASTNode root : roots) root.accept(collector);
    return collector.nodes;
  }

  public static List<ASTNode> gatherColumnRefs(ASTNode root) {
    return gatherColumnRefs(singletonList(root));
  }
}
