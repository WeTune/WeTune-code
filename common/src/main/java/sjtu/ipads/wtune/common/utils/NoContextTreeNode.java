package sjtu.ipads.wtune.common.utils;

public interface NoContextTreeNode<T extends NoContextTreeNode<T>>
    extends TreeNode<NoOpTreeContext, T> {
  T copy();

  @Override
  default void setContext(NoOpTreeContext context) {}

  @Override
  default NoOpTreeContext context() {
    return null;
  }

  @Override
  default T copy(NoOpTreeContext context) {
    return copy();
  }
}
