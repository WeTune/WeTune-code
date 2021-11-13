package sjtu.ipads.wtune.common.tree;

import java.util.Arrays;

public class AstNodesBase<Kind> implements AstNodes<Kind> {
  private final AstContext<Kind> context;
  private final int[] nodeIds;

  protected AstNodesBase(AstContext<Kind> context, int[] nodeIds) {
    this.context = context;
    this.nodeIds = nodeIds;
  }

  @Override
  public AstNode<Kind> get(int index) {
    return AstNode.mk(context, nodeIds[index]);
  }

  @Override
  public int size() {
    return nodeIds.length;
  }

  public AstContext<Kind> context() {
    return context;
  }

  public int[] nodeIds() {
    return nodeIds;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    final AstNodesBase<?> that = (AstNodesBase<?>) obj;
    return this.context == that.context && Arrays.equals(this.nodeIds, that.nodeIds);
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(context) * 31 + Arrays.hashCode(nodeIds);
  }

  @Override
  public String toString() {
    return "AstNodes{" + Arrays.toString(nodeIds) + "}";
  }
}
