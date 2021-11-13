package sjtu.ipads.wtune.common.tree;

import sjtu.ipads.wtune.common.field.FieldKey;

public interface AstContext<Kind> extends TypedTreeContext<Kind> {
  AstFields<Kind> fieldsOf(int nodeId);

  <T> void setFieldOf(int nodeId, FieldKey<T> field, T value);

  void unsetFieldOf(int nodeId, FieldKey<?> field);

  default <T> T fieldOf(int nodeId, FieldKey<T> field) {
    final AstFields<Kind> fields = fieldsOf(nodeId);
    if (fields != null) return field.getFrom(fields);
    else return null;
  }

  @Override
  default int[] childrenOf(int nodeId) {
    throw new UnsupportedOperationException();
  }

  @Override
  default void setChild(int parentNodeId, int childIndex, int childNodeId) {
    throw new UnsupportedOperationException();
  }

  @Override
  default void unsetChild(int parentNodeId, int childIndex) {
    throw new UnsupportedOperationException();
  }
}
