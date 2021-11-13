package sjtu.ipads.wtune.sqlparser.ast1;

import sjtu.ipads.wtune.common.tree.AstNodes;

import java.util.List;

public interface SqlNodes extends AstNodes<SqlKind> {
  @Override
  SqlContext context();

  @Override
  SqlNode get(int index);

  @Override
  List<SqlNode> asList();

  static SqlNodes mk(SqlContext context, int[] nodeIds) {
    return new SqlNodesImpl(context, nodeIds);
  }

  static SqlNodes mkEmpty() {
    return new SqlNodesImpl(null, null);
  }

  static SqlNodes mk(SqlContext context, List<SqlNode> nodes) {
    final int[] nodeIds = new int[nodes.size()];
    for (int i = 0; i < nodes.size(); i++) nodeIds[i] = nodes.get(i).nodeId();
    return mk(context, nodeIds);
  }
}
