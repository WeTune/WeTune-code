package sjtu.ipads.wtune.common.tree;

import sjtu.ipads.wtune.common.utils.ListLike;

public interface TypedTreeNodes<Kind> extends ListLike<TypedTreeNode<Kind>> {
  static <Kind> TypedTreeNodes<Kind> mk(TypedTreeContext<Kind> context, int[] nodeIds) {
    return new TypedTreeNodesImpl<>(context, nodeIds);
  }
}
