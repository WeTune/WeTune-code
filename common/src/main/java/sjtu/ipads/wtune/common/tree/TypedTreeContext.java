package sjtu.ipads.wtune.common.tree;

/**
 * A typed tree is a tree whose nodes are typed. This class is the context of a typed tree,
 * maintaining the structural and other information about the tree.
 *
 * <p>Each node is uniquely identified by an integer id. Methods of this class take such an id to
 * operate nodes.
 */
public interface TypedTreeContext<Kind> {
  int NO_SUCH_NODE = 0;

  int numNodes();

  Kind kindOf(int nodeId);

  int parentOf(int nodeId);

  int childOf(int nodeId, int index);

  int[] childrenOf(int nodeId);

  int mkNode(Kind kind);

  void setChild(int parentNodeId, int childIndex, int childNodeId);

  void unsetChild(int parentNodeId, int childIndex);
}
