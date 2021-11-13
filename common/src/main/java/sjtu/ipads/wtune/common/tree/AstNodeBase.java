package sjtu.ipads.wtune.common.tree;

import sjtu.ipads.wtune.common.field.FieldKey;

import java.util.Set;

public class AstNodeBase<Kind> implements AstNode<Kind> {
  private final AstContext<Kind> context;
  private final int nodeId;

  public AstNodeBase(AstContext<Kind> context, int nodeId) {
    this.context = context;
    this.nodeId = nodeId;
  }

  @Override
  public int size() {
    return context.fieldsOf(nodeId).size();
  }

  @Override
  public Set<FieldKey<?>> keys() {
    return context.fieldsOf(nodeId).keys();
  }

  public AstContext<Kind> context() {
    return context;
  }

  public int nodeId() {
    return nodeId;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    final AstNodeBase<?> that = (AstNodeBase<?>) obj;
    return this.context == that.context && this.nodeId == that.nodeId;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(context) * 31 + Integer.hashCode(nodeId);
  }

  @Override
  public String toString() {
    return "AstNode{" + nodeId + "}";
  }
}
