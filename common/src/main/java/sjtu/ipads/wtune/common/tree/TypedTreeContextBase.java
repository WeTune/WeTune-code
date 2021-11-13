package sjtu.ipads.wtune.common.tree;

import static sjtu.ipads.wtune.common.tree.TreeSupport.isDescendant;

public class TypedTreeContextBase<Kind> extends TreeContextBase<Kind> {
  private final int expectedFanOut;

  protected int[][] children; // children[i] is the child of node i;

  TypedTreeContextBase(int expectedNumNodes, int expectedFanOut) {
    super(expectedNumNodes);
    this.children = new int[parents.length][];
    this.expectedFanOut = expectedFanOut == -1 ? 4 : expectedFanOut;
  }

  @Override
  public int[] childrenOf(int nodeId) {
    checkNodePresent(nodeId);
    return safeGet(children, nodeId, EMPTY_INT_ARRAY);
  }

  @Override
  public void setChild(int parentNodeId, int childIndex, int childNodeId) {
    checkNodePresent(parentNodeId);
    checkNodePresent(childNodeId);
    if (parentOf(childNodeId) != NO_SUCH_NODE) {
      throw new IllegalStateException(
          "cannot set %d as %d's child. Use TreeSupport::moveTree instead"
              .formatted(childNodeId, parentNodeId));
    }
    if (isDescendant(this, childNodeId, parentNodeId)) {
      throw new IllegalStateException(
          "cannot set %d as %d's child. Loop will be incurred."
              .formatted(childNodeId, parentNodeId));
    }

    setParent(childNodeId, parentNodeId);

    children = ensureCapacity(children, parentNodeId, kinds.length);
    children[parentNodeId] = ensureCapacity(children[parentNodeId], childIndex, expectedFanOut);
    children[parentNodeId][childIndex] = childNodeId;
  }

  @Override
  public void unsetChild(int parentNodeId, int childIndex) {
    checkNodePresent(parentNodeId);

    final int[] pChildren = safeGet(children, parentNodeId, null);
    if (pChildren == null || pChildren.length <= childIndex) return;

    unsetParent(pChildren[childIndex]);
    pChildren[childIndex] = NO_SUCH_NODE;
  }
}
