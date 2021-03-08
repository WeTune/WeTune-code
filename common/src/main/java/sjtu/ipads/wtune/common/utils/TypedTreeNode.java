package sjtu.ipads.wtune.common.utils;

public interface TypedTreeNode<Type extends Enum<Type>> {
  Type type();

  TypedTreeNode<Type> successor();

  TypedTreeNode<Type>[] predecessors();

  default void accept(TypedTreeVisitor<Type, TypedTreeNode<Type>> visitor) {
    if (visitor.on(this))
      for (TypedTreeNode<Type> predecessor : predecessors()) predecessor.accept(visitor);
    visitor.off(this);
  }
}
