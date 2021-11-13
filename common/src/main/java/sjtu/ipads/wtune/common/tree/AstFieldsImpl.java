package sjtu.ipads.wtune.common.tree;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import sjtu.ipads.wtune.common.field.FieldKey;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class AstFieldsImpl<Kind> implements AstFields<Kind> {
  private final AstContext<Kind> context;
  private final int nodeId;
  private final Map<FieldKey<?>, Object> fields;

  AstFieldsImpl(AstContext<Kind> context, int nodeId, Map<FieldKey<?>, Object> fields) {
    this.context = context;
    this.nodeId = nodeId;
    this.fields = fields;
  }

  @Override
  public <T> T getField(FieldKey<T> field) {
    return (T) fields.get(field);
  }

  @Override
  public <T> void setField(FieldKey<T> field, T value) {
    fields.put(field, value);
  }

  @Override
  public Kind kind() {
    return context.kindOf(nodeId);
  }

  @Override
  public int parent() {
    return context.parentOf(nodeId);
  }

  @Override
  public int size() {
    return fields.size();
  }

  @Override
  public Set<FieldKey<?>> keys() {
    return fields.keySet();
  }
}
