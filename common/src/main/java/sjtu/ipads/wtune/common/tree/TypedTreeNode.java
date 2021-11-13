package sjtu.ipads.wtune.common.tree;

import sjtu.ipads.wtune.common.utils.ListLike;

/** A convenient wrapper for a tree node. Operations are delegated to the context. */
public interface TypedTreeNode<Kind> {
  TypedTreeContext<Kind> context();

  int nodeId();

  default Kind kind() {
    return context().kindOf(nodeId());
  }

  default TypedTreeNode<Kind> parent() {
    return mk(context(), context().parentOf(nodeId()));
  }

  default TypedTreeNode<Kind> child(int index) {
    return mk(context(), context().childOf(nodeId(), index));
  }

  default ListLike<? extends TypedTreeNode<Kind>> children() {
    return TypedTreeNodes.mk(context(), context().childrenOf(nodeId()));
  }

  default TypedTreeNode<Kind> copyTree() {
    return mk(context(), TreeSupport.copyTree(context(), nodeId()));
  }

  default void setChild(int index, TypedTreeNode<Kind> child) {
    context().setChild(nodeId(), index, child.nodeId());
  }

  default void unsetChild(int index) {
    context().unsetChild(nodeId(), index);
  }

  static <Kind> TypedTreeNode<Kind> mk(TypedTreeContext<Kind> context, int id) {
    return new TypedTreeNodeImpl<>(context, id);
  }
}
