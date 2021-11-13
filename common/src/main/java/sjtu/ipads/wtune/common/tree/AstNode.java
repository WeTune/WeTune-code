package sjtu.ipads.wtune.common.tree;

import sjtu.ipads.wtune.common.field.FieldKey;
import sjtu.ipads.wtune.common.field.Fields;
import sjtu.ipads.wtune.common.utils.ListLike;

public interface AstNode<Kind> extends TypedTreeNode<Kind>, Fields {
  @Override
  AstContext<Kind> context();

  @Override
  default AstNode<Kind> parent() {
    return mk(context(), context().parentOf(nodeId()));
  }

  @Override
  default AstNode<Kind> child(int index) {
    return mk(context(), context().childOf(nodeId(), index));
  }

  @Override
  default ListLike<? extends AstNode<Kind>> children() {
    return AstNodes.mk(context(), context().childrenOf(nodeId()));
  }

  @Override
  default AstNode<Kind> copyTree() {
    return mk(context(), TreeSupport.copyTree(context(), nodeId()));
  }

  @Override
  default <T> T getField(FieldKey<T> field) {
    return context().fieldOf(nodeId(), field);
  }

  @Override
  default <T> void setField(FieldKey<T> field, T value) {
    context().setFieldOf(nodeId(), field, value);
  }

  static <Kind> AstNode<Kind> mk(AstContext<Kind> context, int id) {
    return new AstNodeBase<>(context, id);
  }
}
