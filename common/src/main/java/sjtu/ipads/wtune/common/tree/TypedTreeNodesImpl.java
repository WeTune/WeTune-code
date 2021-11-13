package sjtu.ipads.wtune.common.tree;

record TypedTreeNodesImpl<Kind>(TypedTreeContext<Kind> context,
                                int[] nodeIds) implements TypedTreeNodes<Kind> {
  @Override
  public TypedTreeNode<Kind> get(int index) {
    return TypedTreeNode.mk(context, nodeIds[index]);
  }

  @Override
  public int size() {
    return nodeIds.length;
  }
}
