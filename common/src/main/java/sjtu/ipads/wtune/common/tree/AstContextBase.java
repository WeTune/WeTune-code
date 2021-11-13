package sjtu.ipads.wtune.common.tree;

import sjtu.ipads.wtune.common.field.FieldKey;

import java.util.HashMap;
import java.util.Map;

public class AstContextBase<Kind> extends TreeContextBase<Kind> implements AstContext<Kind> {
  protected Map<FieldKey<?>, Object>[] fields;

  @SuppressWarnings("unchecked")
  protected AstContextBase(int expectedNumNodes) {
    super(expectedNumNodes);
    this.fields = new Map[expectedNumNodes];
  }

  @Override
  public AstFields<Kind> fieldsOf(int nodeId) {
    checkNodePresent(nodeId);
    final Map<FieldKey<?>, Object> rawFields = safeGet(fields, nodeId, null);
    return rawFields == null ? null : new AstFieldsImpl<>(this, nodeId, rawFields);
  }

  @Override
  public <T> void setFieldOf(int nodeId, FieldKey<T> field, T value) {
    checkNodePresent(nodeId);
    checkNodePresent(value);

    fields = ensureCapacity(fields, nodeId, kinds.length);

    Map<FieldKey<?>, Object> rawFields = fields[nodeId];
    if (rawFields == null) rawFields = fields[nodeId] = new HashMap<>();

    field.setTo(new AstFieldsImpl<>(this, nodeId, rawFields), value);

    if (value instanceof AstNode) setParent(((AstNode<?>) value).nodeId(), nodeId);
    if (value instanceof AstNodes) {
      for (AstNode<?> child : ((AstNodes<?>) value)) setParent(child.nodeId(), nodeId);
    }
  }

  @Override
  public void unsetFieldOf(int nodeId, FieldKey<?> field) {
    checkNodePresent(nodeId);

    Map<FieldKey<?>, Object> rawFields = fields[nodeId];
    final Object value = rawFields.remove(field);

    if (value instanceof AstNode) unsetParent(((AstNode<?>) value).nodeId());
    if (value instanceof AstNodes) {
      for (AstNode<?> child : ((AstNodes<?>) value)) unsetParent(child.nodeId());
    }
  }

  private void checkNodePresent(Object obj) {
    if (obj instanceof AstNode) checkNodePresent(((AstNode<?>) obj).nodeId());
    if (obj instanceof AstNodes) for (Object o : ((AstNodes<?>) obj)) checkNodePresent(o);
  }
}
