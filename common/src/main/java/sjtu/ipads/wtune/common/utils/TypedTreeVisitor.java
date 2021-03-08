package sjtu.ipads.wtune.common.utils;

public interface TypedTreeVisitor<Type extends Enum<Type>, Tree extends TypedTreeNode<Type>> {
  default boolean on(Tree tree) {
    return true;
  }

  default void off(Tree tree) {}
}
