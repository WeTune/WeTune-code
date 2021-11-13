package sjtu.ipads.wtune.common.tree;

import java.util.Arrays;
import java.util.NoSuchElementException;

abstract class TreeContextBase<Kind> implements TypedTreeContext<Kind> {
  protected int numNodes;

  protected Object[] kinds; // kinds[i] is the kind of node i;
  protected int[] parents; // parents[i] is the parent of node i.

  TreeContextBase(int expectedNumNodes) {
    expectedNumNodes = expectedNumNodes == -1 ? 16 : expectedNumNodes;
    this.kinds = new Object[expectedNumNodes];
    this.parents = new int[expectedNumNodes];
  }

  public int numNodes() {
    return numNodes;
  }

  public Kind kindOf(int nodeId) {
    checkNodePresent(nodeId);
    return (Kind) kinds[nodeId]; // Any known node must have been in `kinds`.
  }

  @Override
  public int parentOf(int nodeId) {
    checkNodePresent(nodeId);
    return safeGet(parents, nodeId, NO_SUCH_NODE);
  }

  @Override
  public int childOf(int nodeId, int index) {
    checkNodePresent(nodeId);
    return safeGet(childrenOf(nodeId), index, NO_SUCH_NODE);
  }

  @Override
  public int mkNode(Kind kind) {
    final int newNodeId = ++numNodes;
    kinds = ensureCapacity(kinds, newNodeId, -1);
    kinds[newNodeId] = kind;
    return newNodeId;
  }

  protected void setParent(int childNodeId, int parentNodeId) {
    parents = ensureCapacity(parents, childNodeId, kinds.length);
    parents[childNodeId] = parentNodeId;
  }

  protected void unsetParent(int childNodeId) {
    parents[childNodeId] = NO_SUCH_NODE;
  }

  protected void checkNodePresent(int nodeId) {
    if (nodeId <= 0 || nodeId > numNodes)
      throw new NoSuchElementException("no such node in this tree: " + nodeId);
  }

  protected <T> T[] ensureCapacity(T[] array, int requirement, int newCapacity) {
    if (array.length <= requirement)
      return Arrays.copyOf(array, newCapacity <= requirement ? (requirement + 1) : newCapacity);
    else return array;
  }

  protected int[] ensureCapacity(int[] array, int requirement, int newCapacity) {
    if (array == null) return new int[Math.max(requirement + 1, newCapacity)];
    else if (array.length <= requirement)
      return Arrays.copyOf(array, newCapacity <= requirement ? (requirement + 1) : newCapacity);
    else return array;
  }

  protected static int safeGet(int[] array, int index, int defaultVal) {
    if (index < array.length) return array[index];
    else return defaultVal;
  }

  protected static <T> T safeGet(T[] array, int index, T defaultVal) {
    if (index < array.length) return array[index];
    else return defaultVal;
  }

  protected static final int[] EMPTY_INT_ARRAY = new int[0];
}
