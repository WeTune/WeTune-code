package sjtu.ipads.wtune.common.tree;

record TypedTreeNodeImpl<Kind>(TypedTreeContext<Kind> context,
                               int nodeId) implements TypedTreeNode<Kind> {}
