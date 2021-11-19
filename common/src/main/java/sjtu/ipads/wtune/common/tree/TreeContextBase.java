package sjtu.ipads.wtune.common.tree;

import java.util.Arrays;

import static sjtu.ipads.wtune.common.tree.TreeSupport.checkNodePresent;

public abstract class TreeContextBase<Kind, Nd extends TreeContextBase.NdBase<Kind>>
    implements TreeContext<Kind> {
  protected int maxNodeId;
  protected Nd[] nodes;

  protected TreeContextBase(Nd[] nodes) {
    this.maxNodeId = 0;
    this.nodes = nodes;
  }

  @Override
  public int maxNodeId() {
    return maxNodeId;
  }

  @Override
  public boolean isPresent(int nodeId) {
    return nodeId > 0 && nodeId <= maxNodeId && nodes[nodeId] != null;
  }

  @Override
  public Kind kindOf(int nodeId) {
    checkNodePresent(this, nodeId);
    return nodes[nodeId].kind();
  }

  @Override
  public int parentOf(int nodeId) {
    checkNodePresent(this, nodeId);
    return nodes[nodeId].parentId();
  }

  @Override
  public boolean isChildOf(int parentId, int nodeId) {
    checkNodePresent(this, nodeId);
    checkNodePresent(this, parentId);
    return nodes[nodeId].parentId() == parentId;
  }

  @Override
  public int mkNode(Kind kind) {
    final int newNodeId = ++maxNodeId;
    nodes = ensureCapacity(nodes, newNodeId, nodes.length << 1);
    nodes[newNodeId] = mk(kind);
    return newNodeId;
  }

  @Override
  public void deleteNode(int nodeId) {
    detachNode(nodeId);
    nodes[nodeId] = null;
    if (nodeId == maxNodeId) --maxNodeId;
  }

  @Override
  public void compact() {
    if (maxNodeId <= 1) return;

    int forwardIdx = 1, backwardIdx = maxNodeId;
    while (true) {
      while (nodes[forwardIdx] != null && forwardIdx < backwardIdx) ++forwardIdx;
      while (nodes[backwardIdx] == null && backwardIdx > forwardIdx) --backwardIdx;
      if (forwardIdx == backwardIdx) break;
      reNumber(backwardIdx, forwardIdx);
    }

    maxNodeId = nodes[backwardIdx] == null ? backwardIdx - 1 : backwardIdx;
    if (maxNodeId <= (nodes.length >> 1)) nodes = Arrays.copyOf(nodes, maxNodeId);
  }

  protected abstract void reNumber(int from, int to);

  protected abstract Nd mk(Kind kind);

  protected static <T> T[] ensureCapacity(T[] array, int requirement, int newCapacity) {
    if (array.length <= requirement)
      return Arrays.copyOf(array, newCapacity <= requirement ? (requirement + 1) : newCapacity);
    else return array;
  }

  protected static int[] ensureCapacity(int[] array, int requirement, int newCapacity) {
    if (array.length <= requirement)
      return Arrays.copyOf(array, newCapacity <= requirement ? (requirement + 1) : newCapacity);
    else return array;
  }

  public interface NdBase<Kind> {
    int parentId();

    Kind kind();
  }
}
