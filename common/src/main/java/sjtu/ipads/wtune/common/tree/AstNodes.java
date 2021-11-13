package sjtu.ipads.wtune.common.tree;

import sjtu.ipads.wtune.common.utils.ListLike;

import java.util.List;

public interface AstNodes<Kind> extends ListLike<AstNode<Kind>> {
  AstContext<Kind> context();

  @Override
  AstNode<Kind> get(int index);

  static <Kind> AstNodes<Kind> mk(AstContext<Kind> context, int[] nodeIds) {
    return new AstNodesBase<>(context, nodeIds);
  }

  static <Kind> AstNodes<Kind> mk(AstContext<Kind> context, List<AstNode<Kind>> nodes) {
    final int[] nodeIds = new int[nodes.size()];
    for (int i = 0; i < nodes.size(); i++) nodeIds[i] = nodes.get(i).nodeId();
    return mk(context, nodeIds);
  }
}
